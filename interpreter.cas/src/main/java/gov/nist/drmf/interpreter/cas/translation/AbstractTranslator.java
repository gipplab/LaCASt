package gov.nist.drmf.interpreter.cas.translation;

import gov.nist.drmf.interpreter.cas.logging.TranslatedExpression;
import gov.nist.drmf.interpreter.cas.translation.components.*;
import gov.nist.drmf.interpreter.common.InformationLogger;
import gov.nist.drmf.interpreter.common.Keys;
import gov.nist.drmf.interpreter.common.TranslationException;
import gov.nist.drmf.interpreter.common.TranslationException.Reason;
import gov.nist.drmf.interpreter.common.grammar.Brackets;
import gov.nist.drmf.interpreter.common.grammar.ITranslator;
import gov.nist.drmf.interpreter.common.grammar.MathTermTags;
import gov.nist.drmf.interpreter.mlp.extensions.FeatureSetUtility;
import mlp.FeatureSet;
import mlp.MathTerm;
import mlp.PomTaggedExpression;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Andre Greiner-Petter
 */
public abstract class AbstractTranslator implements ITranslator<PomTaggedExpression> {


	public static final Map<String, Map<Integer, Set<String>>> problemTokens = new HashMap<>();

	public static final String SPACE = " ";

	public static final String OPEN_PARENTHESIS_PATTERN =
		"(left)[-\\s](parenthesis|bracket|brace|delimiter)";

	public static final String CLOSE_PARENTHESIS_PATTERN =
		"(right)[-\\s](parenthesis|bracket|brace|delimiter)";

	public static final String PARENTHESIS_PATTERN =
		"(right|left)[-\\s](parenthesis|bracket|brace|delimiter)";

	public static final String CHAR_BACKSLASH = "\\";
	public static final Pattern DLMF_ID_PATTERN = Pattern.compile( "-(\\d+)-\\d+-E\\d+.s.tex" );

	public static String MULTIPLY;

	private static boolean SET_MODE = false;

	protected static InformationLogger INFO_LOG;

	private static Logger LOG = LogManager.getLogger( AbstractTranslator.class.getName() );

	protected TranslatedExpression local_inner_exp = new TranslatedExpression();

	protected static TranslatedExpression global_exp;

	private boolean inner_Error = false;

	private static String id = "undefined";
	private boolean tolerant = true;

	public static Map<String, Map<Integer, Set<String>>> getProblemTokens() {
		return problemTokens;
	}

	public void setId( String id ) {
		AbstractTranslator.id = id;
	}

	public boolean isMlpError() {
		return mlpError;
	}

	private boolean mlpError = false;


	public void setTolerant( boolean tolerant ) {
		this.tolerant = tolerant;
	}

	/**
	 * This method simply handles a general expression and invoke
	 * all special parses if needed!
	 *
	 * @param exp
	 * @param exp_list
	 * @return
	 */
	protected TranslatedExpression parseGeneralExpression(
		PomTaggedExpression exp,
		List<PomTaggedExpression> exp_list ) {
		// create inner local translation (recursive)
		AbstractTranslator inner_parser = null;
		// if there was an inner error
		boolean return_value;

		// if it is an empty exp...
		if ( exp.isEmpty() ) {
			return local_inner_exp;
		}

		// handle all different cases
		// first, does this expression contains a term?
		if ( !containsTerm( exp ) ) {
			inner_parser = new EmptyExpressionTranslator();
			return_value = inner_parser.translate( exp );
		} else { // if not handle all different cases of terms
			MathTerm term = exp.getRoot();
			// first, is this a DLMF macro?
			if ( isDLMFMacro( term ) ) { // BEFORE FUNCTION!
				MacroTranslator mp = new MacroTranslator();
				return_value = mp.translate( exp, exp_list );
				inner_parser = mp;
			} //is it a sum or a product
			else if (isSumOrProduct(term)){
				SumProductTranslator sm = new SumProductTranslator();
				return_value = sm.translate(exp, exp_list);
				inner_parser = sm;
			} // it could be a sub sequence
			else if ( isSubSequence( term ) ) {
				Brackets bracket = Brackets.getBracket( term.getTermText() );
				SequenceTranslator sp = new SequenceTranslator( bracket, SET_MODE );
				return_value = sp.translate( exp_list );
				inner_parser = sp;
			} // this is special, could be a function like cos
			else if ( isFunction( term ) ) {
				FunctionTranslator fp = new FunctionTranslator();
				return_value = fp.translate( exp, exp_list );
				inner_parser = fp;
			} // otherwise it is a general math term
			else {
				MathTermTranslator mp = new MathTermTranslator();
				return_value = mp.translate( exp, exp_list );
				inner_parser = mp;
			}
		}
		inner_Error = !return_value;
		return inner_parser.local_inner_exp;
	}

	private boolean isDLMFMacro( MathTerm term ) {
		MathTermTags tag = MathTermTags.getTagByKey( term.getTag() );
		if ( tag != null && tag.equals( MathTermTags.dlmf_macro ) )
			return true;
		FeatureSet dlmf = term.getNamedFeatureSet( Keys.KEY_DLMF_MACRO );
		if ( dlmf != null ) {
			SortedSet<String> role = dlmf.getFeature( Keys.FEATURE_ROLE );
			if ( role != null &&
				(role.first().matches( Keys.FEATURE_VALUE_CONSTANT ) ||
					role.first().matches( Keys.FEATURE_VALUE_SYMBOL )
				) )
				return false;
			else return true;
		} else return false;
	}

	protected boolean isSumOrProduct(MathTerm term){
		if(term.getTag().equals(MathTermTags.operator.tag()))
			return FeatureSetUtility.isSum(term) || FeatureSetUtility.isProduct(term);
		return false;
	}

	protected boolean isSubSequence( MathTerm term ) {
		String tag = term.getTag();
		if ( tag.matches( OPEN_PARENTHESIS_PATTERN ) ) {
			return true;
		} else if ( tag.matches( CLOSE_PARENTHESIS_PATTERN ) ) {
			LOG.error( "Reached a closed bracket " + term.getTermText() +
				" but there was not a corresponding" +
				" open bracket before." );
			return false;
		} else return false;
	}

	private boolean isFunction( MathTerm term ) {
		MathTermTags tag = MathTermTags.getTagByKey( term.getTag() );
		if ( tag == null ) {
			return FeatureSetUtility.isFunction( term );
		}
		if ( tag.equals( MathTermTags.function ) ) return true;
		return false;
	}

	public boolean containsTerm( PomTaggedExpression e ) {
		MathTerm t = e.getRoot();
		return (t != null && !t.isEmpty());
	}

	/**
	 * Simple test if the given string is wrapped by parenthesis.
	 * It only returns true if there is an open bracket at start and
	 * at the end AND the first open one is really closed in the end.
	 * Something like (1)/(2) would return false.
	 *
	 * @param str with or without brackets
	 * @return false if there are no brackets
	 */
	protected static boolean testBrackets( String str ) {
		String tmp = str.trim();
		if ( !tmp.matches( Brackets.OPEN_PATTERN + ".*" + Brackets.CLOSED_PATTERN ) )
			return false;

		Brackets open = Brackets.getBracket( tmp.charAt( 0 ) + "" );
		Brackets inner, last;
		LinkedList<Brackets> open_list = new LinkedList<>();
		open_list.add( open );
		String symbol;

		for ( int i = 1; i < tmp.length(); i++ ) {
			if ( open_list.isEmpty() ) return false;

			symbol = "" + tmp.charAt( i );
			inner = Brackets.getBracket( symbol );

			if ( inner == null ) continue;
			else if ( inner.opened ) {
				open_list.addLast( inner );
			} else {
				last = open_list.getLast();
				if ( last.counterpart.equals( inner.symbol ) )
					open_list.removeLast();
				else return false;
			}
		}
		return open_list.isEmpty();
	}

	public static void activateSetMode() {
		LOG.info( "Set-Mode for sequences activated!" );
		SET_MODE = true;
	}

	public static void deactivateSetMode() {
		LOG.info( "Set-Mode for sequences deactivated!" );
		SET_MODE = false;
	}

	@Override
	public abstract boolean translate( PomTaggedExpression expression );

	@Override
	public String getTranslatedExpression() {
		return local_inner_exp.getTranslatedExpression();
	}

	public TranslatedExpression getTranslatedExpressionObject() {
		return local_inner_exp;
	}

	public TranslatedExpression getGlobalExpressionObject() {
		return global_exp;
	}

	protected boolean isInnerError() {
		return inner_Error;
	}

	public void reset() {
		local_inner_exp = new TranslatedExpression();
		global_exp = new TranslatedExpression();
		mlpError = false;
	}

	protected void appendLocalErrorExpression( String tag ) {
		LOG.debug( "Adding fake Maple for error expression " + tag );
		local_inner_exp.addTranslatedExpression( "\"error" + StringEscapeUtils.escapeJava( tag ) + "\"" );
	}

	protected boolean handleNull( Object o, String message, Reason reason, String token, Exception exception ) {
		if ( o == null ) {
			String exceptionString = "";
			if ( LOG.isWarnEnabled() && exception != null ) {
				try {
					final StackTraceElement[] stackTrace = exception.getStackTrace();
					final StackTraceElement traceElement = stackTrace[ 0 ];
					exceptionString = traceElement.getClassName() + ":L"
						+ traceElement.getLineNumber() + ":" + exception.getMessage();
				} catch ( Exception e ) {
					//ignore
				}
			}
			if ( reason == Reason.MLP_ERROR ) {
				mlpError = true;
			}
			final String errorMessage = String.format(
				"Translation error in id '%s'\n\tmessage:%s\n\ttoken:%s\n\treason:%s,\n\texception:%s",
				this.id, message, token, reason, exceptionString );
			LOG.warn( errorMessage );
			final Matcher m = DLMF_ID_PATTERN.matcher( id );
			if ( m.matches() ) {
				final int chapter = Integer.parseInt( m.group( 1 ) );
				if ( !problemTokens.containsKey( token ) ) {
					problemTokens.put( token, new HashMap<>() );
				}
				final Map<Integer, Set<String>> tokenMap = problemTokens.get( token );
				if ( !tokenMap.containsKey( chapter ) ) {
					tokenMap.put( chapter, new HashSet<>() );
				}
				final Set<String> messages = tokenMap.get( chapter );
				messages.add( errorMessage );
			}
			if ( tolerant ) {
				appendLocalErrorExpression( token );
				return true;
			}
			throw new TranslationException(
				message,
				reason,
				token,
				exception
			);
		}
		return false;
	}

}
