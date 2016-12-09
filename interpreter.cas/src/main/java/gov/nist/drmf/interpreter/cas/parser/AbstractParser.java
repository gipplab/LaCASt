package gov.nist.drmf.interpreter.cas.parser;

import gov.nist.drmf.interpreter.cas.logging.InformationLogger;
import gov.nist.drmf.interpreter.cas.logging.TranslatedExpression;
import gov.nist.drmf.interpreter.cas.parser.components.*;
import gov.nist.drmf.interpreter.common.Keys;
import gov.nist.drmf.interpreter.common.grammar.Brackets;
import gov.nist.drmf.interpreter.common.grammar.IParser;
import gov.nist.drmf.interpreter.common.grammar.MathTermTags;
import gov.nist.drmf.interpreter.mlp.extensions.FeatureSetUtility;
import mlp.FeatureSet;
import mlp.MathTerm;
import mlp.PomTaggedExpression;

import java.util.List;
import java.util.SortedSet;
import java.util.logging.Logger;

/**
 *
 *
 * @author Andre Greiner-Petter
 */
public abstract class AbstractParser implements IParser {
    public static final String SPACE = " ";

    public static final String OPEN_PARENTHESIS_PATTERN =
            "(left)[-\\s](parenthesis|bracket|brace)";

    public static final String CLOSE_PARENTHESIS_PATTERN =
            "(right)[-\\s](parenthesis|bracket|brace)";

    public static final String PARENTHESIS_PATTERN =
            "(right|left)[-\\s](parenthesis|bracket|brace)";

    public static final String CHAR_BACKSLASH = "\\";

    public static String MULTIPLY;

    protected static InformationLogger INFO_LOG;

    protected static Logger ERROR_LOG;

    protected TranslatedExpression local_inner_exp = new TranslatedExpression();

    protected static TranslatedExpression global_exp;

    private boolean inner_Error = false;

    /**
     * This method simply handles a general expression and invoke
     * all special parses if needed!
     * @param exp
     * @param exp_list
     * @return
     */
    protected TranslatedExpression parseGeneralExpression(
            PomTaggedExpression exp,
            List<PomTaggedExpression> exp_list){
        // create inner local parser (recursive)
        AbstractParser inner_parser = null;
        // if there was an inner error
        boolean return_value;

        // handle all different cases
        // first, does this expression contains a term?
        if ( !containsTerm(exp) ){
            inner_parser = new EmptyExpressionParser();
            return_value = inner_parser.parse(exp);
        } else { // if not handle all different cases of terms
            MathTerm term = exp.getRoot();
            // first, is this a DLMF macro?
            if ( isDLMFMacro(term) ){ // BEFORE FUNCTION!
                MacroParser mp = new MacroParser();
                return_value = mp.parse(exp, exp_list);
                inner_parser = mp;
            } // second, it could be a sub sequence
            else if ( isSubSequence(term) ){
                Brackets bracket = Brackets.getBracket(term.getTermText());
                SequenceParser sp = new SequenceParser(bracket);
                return_value = sp.parse(exp_list);
                inner_parser = sp;
            } // this is special, could be a function like cos
            else if ( isFunction(term) ){
                FunctionParser fp = new FunctionParser();
                return_value = fp.parse(exp, exp_list);
                inner_parser = fp;
            } // otherwise it is a general math term
            else {
                MathTermParser mp = new MathTermParser();
                return_value = mp.parse(exp, exp_list);
                inner_parser = mp;
            }
        }

        inner_Error = !return_value;
        return inner_parser.local_inner_exp;
    }

    private boolean isDLMFMacro( MathTerm term ){
        MathTermTags tag = MathTermTags.getTagByKey(term.getTag());
        if ( tag != null && tag.equals( MathTermTags.dlmf_macro ) )
            return true;
        FeatureSet dlmf = term.getNamedFeatureSet(Keys.KEY_DLMF_MACRO);
        if ( dlmf != null ){
            SortedSet<String> role = dlmf.getFeature(Keys.FEATURE_ROLE);
            if ( role != null && role.first().matches(Keys.FEATURE_VALUE_CONSTANT) )
                return false;
            else return true;
        } else return false;
    }

    protected boolean isSubSequence( MathTerm term ){
        String tag = term.getTag();
        if ( tag.matches(OPEN_PARENTHESIS_PATTERN) ) {
            return true;
        } else if ( tag.matches(CLOSE_PARENTHESIS_PATTERN) ){
            ERROR_LOG.severe("Reached a closed bracket " + term.getTermText() +
                    " but there was not a corresponding" +
                    " open bracket before.");
            return false;
        } else return false;
    }

    private boolean isFunction( MathTerm term ){
        MathTermTags tag = MathTermTags.getTagByKey(term.getTag());
        if ( tag == null ) {
            return FeatureSetUtility.isFunction(term);
        }
        if ( tag.equals( MathTermTags.function ) ) return true;
        return false;
    }

    public boolean containsTerm( PomTaggedExpression e ){
        MathTerm t = e.getRoot();
        return (t != null && !t.isEmpty());
    }

    /**
     * Simple test if the given string is wrapped by parenthesis.
     * It only returns true if there is an open bracket at start and
     * at the end AND the first open one is really closed in the end.
     * Something like (1)/(2) would return false.
     * @param str with or without brackets
     * @return false if there are no brackets
     */
    protected boolean testBrackets( String str ){
        if ( !str.matches(Brackets.OPEN_PATTERN + ".*" + Brackets.CLOSED_PATTERN) )
            return false;

        int open = 0;
        for ( int i = 1; i < str.length(); i++ ){
            if ( (""+str.charAt(i)).matches( Brackets.OPEN_PATTERN ) )
                open++;
            else if ( (""+str.charAt(i)).matches( Brackets.CLOSED_PATTERN ) )
                open--;
        }
        return open == -1;
    }

    @Override
    public abstract boolean parse(PomTaggedExpression expression);

    @Override
    public String getTranslatedExpression() {
        // TODO
        return local_inner_exp.getTranslatedExpression();
    }

    public TranslatedExpression getTranslatedExpressionObject(){
        return local_inner_exp;
    }

    public TranslatedExpression getGlobalExpressionObject(){
        return global_exp;
    }

    protected boolean isInnerError(){
        return inner_Error;
    }
}
