package gov.nist.drmf.interpreter.cas.translation.components;

import gov.nist.drmf.interpreter.cas.logging.TranslatedExpression;
import gov.nist.drmf.interpreter.cas.translation.AbstractListTranslator;
import gov.nist.drmf.interpreter.cas.translation.AbstractTranslator;
import gov.nist.drmf.interpreter.cas.translation.SemanticLatexTranslator;
import gov.nist.drmf.interpreter.common.constants.Keys;
import gov.nist.drmf.interpreter.common.exceptions.TranslationException;
import gov.nist.drmf.interpreter.common.grammar.Brackets;
import gov.nist.drmf.interpreter.common.grammar.ExpressionTags;
import gov.nist.drmf.interpreter.common.grammar.MathTermTags;
import gov.nist.drmf.interpreter.common.symbols.BasicFunctionsTranslator;
import mlp.MathTerm;
import mlp.PomTaggedExpression;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.List;

import static gov.nist.drmf.interpreter.cas.common.DLMFPatterns.*;

/**
 * There are two possible types of sequences in this code.
 *  1) It is an empty expression by itself, tagged with sequence.
 *      In that case, a sequence is simply a row of elements where
 *      each element is a stand alone element.
 *      Be aware, a sequence can be one element by it self.
 *      There will be no parenthesis added to this kind of sequence.
 *  2) It is a row of expressions wrapped by parenthesis.
 *      In that case, it is not really a sequence object (from MLP)
 *      but a sequence in parenthesis. It produces only one
 *      TranslatedExpression.
 *
 * @see ExpressionTags
 * @see Brackets
 * @see AbstractListTranslator
 * @see AbstractTranslator
 * @author Andre Greiner-Petter
 */
public class SequenceTranslator extends AbstractListTranslator {

    private static final Logger LOG = LogManager.getLogger(SequenceTranslator.class.getName());

    // the open bracket if needed
    @Nullable
    private Brackets open_bracket;

    private boolean setMode = false;

    private TranslatedExpression localTranslations;

    /**
     * Uses only for a general sequence expression.
     * If the tag is sequence we don't need to check any parenthesis.
     */
    public SequenceTranslator(AbstractTranslator superTranslator){
        super(superTranslator);
        localTranslations = new TranslatedExpression();
    }

    @Nullable
    @Override
    public TranslatedExpression getTranslatedExpressionObject() {
        return localTranslations;
    }

    /**
     * Use this if the sequence is wrapped by parenthesis.
     * In that we don't know the length of the sequence. The sequence
     * ends when we reach the next corresponding bracket, matches to
     * the open bracket.
     * @param open_bracket the following sequence is wrapped by brackets
     *                     the given bracket is the first open bracket of the following
     *                     sequence
     */
    public SequenceTranslator( AbstractTranslator superTranslator, Brackets open_bracket ){
        this(superTranslator);
        this.open_bracket = open_bracket;
    }

    public SequenceTranslator( AbstractTranslator superTranslator, Brackets open_bracket, boolean setMode ){
        this(superTranslator, open_bracket);
        this.setMode = setMode;
    }

    @Override
    public boolean translate( PomTaggedExpression exp, List<PomTaggedExpression> following ){
        if ( exp == null ) return translate(following);
        else if ( following == null ) return translate(exp);
        else return false;
    }

    /**
     * This method parses a PomTaggedExpression of type sequence and
     * only these expressions! There will be no parenthesis added
     *
     * @see ExpressionTags#sequence
     * @param expression with "sequence" tag!
     * @return true if the parsing process finish correctly
     *          otherwise false
     */
    @Override
    public boolean translate(PomTaggedExpression expression){
        if ( !ExpressionTags.sequence.tag().matches(expression.getTag()) ){
            LOG.error("You used the wrong translation method. " +
                    "The given expression is not a sequence! " +
                    expression.getTag());
            return false;
        }

        // temporally string
        String part;

        // get all sub elements
        List<PomTaggedExpression> exp_list = expression.getComponents();

        // run through each element
        while ( !exp_list.isEmpty() && !isInnerError() ){
            PomTaggedExpression exp = exp_list.remove(0);
            TranslatedExpression inner_translation = parseGeneralExpression( exp, exp_list );

            // only take the last object and check if it is
            // necessary to add a space character behind
            part = inner_translation.getLastExpression();
            TranslatedExpression global = super.getGlobalTranslationList();

            boolean lastMerged = false;
            if ( part == null ) {
                part = global.getLastExpression();
                if ( part == null )
                    return true;
                lastMerged = true;
            }

//            if ( part.matches( ".*\\s*\\)\\s*" ) ){
//                MathTerm tmp = new MathTerm(")", MathTermTags.right_parenthesis.tag());
//                exp = new PomTaggedExpression(tmp);
//            }

            part = checkMultiplyAddition(exp, exp_list, part);

            // finally add all elements to the inner list
            inner_translation.replaceLastExpression( part );
            if ( lastMerged )
                localTranslations.replaceLastExpression( inner_translation.toString() );
            else localTranslations.addTranslatedExpression( inner_translation );
        }

        // finally return value
        return !isInnerError();
    }

    /**
     * Use this function ONLY when you created an object of this class
     * with a given bracket {@link SequenceTranslator#SequenceTranslator(AbstractTranslator,Brackets)}.
     *
     * This method goes through a given list of expressions until it
     * reached the closed bracket that matches to the given open bracket
     * in the constructor.
     *
     * Than it will return true and organize merges all parts in the
     * global list of translated expressions.
     *
     * @param following_exp the descendants of a previous expression
     *                      with an open bracket
     * @return true when the translation finished without an error.
     */
    public boolean translate(List<PomTaggedExpression> following_exp) {
        if ( open_bracket == null ){
            LOG.error("Wrong translation method used. " +
                    "You have to specify an open bracket to translate it like a sequence " +
                    "that way.");
            return false;
        }

        // iterate through all elements
        while ( !following_exp.isEmpty() ){
            // take the next expression
            PomTaggedExpression exp = following_exp.remove(0);

            // otherwise investigate the term
            MathTerm term = exp.getRoot();
            //If this term is a bracket there are three possible options
            //  1) another open bracket
            //      -> reached a new sub sequence
            //  2) a closed bracket which is the counterpart of the first open bracket
            //      -> this sequence ends here
            //  3) another closed bracket
            //      -> there is a bracket error in the sequence

            // open or closed brackets
            Brackets bracket = ifIsBracketTransform(term, open_bracket);
//            if ( bracket == null && term.getTermText().matches( ABSOLUTE_VAL_TERM_TEXT_PATTERN )){
//                bracket = Brackets.abs_val;
//            }

            if ( bracket != null ){
                // another open bracket -> reached a new sub sequence
                // bracket cannot be null, because we checked the tag of the term before
                if ( bracket.opened ){
                    // create a new SequenceTranslator (2nd kind)
                    SequenceTranslator sp = new SequenceTranslator( super.getSuperTranslator(), bracket, setMode );
                    // translate the following expressions
                    if ( sp.translate(following_exp) ){
                        // if the translation finished correctly, there is nothing to do here
                        // only take all of the inner solutions
                        localTranslations.addTranslatedExpression( sp.getTranslatedExpressionObject() );
                        // we don't need to add/remove elements from global_exp here
                        continue;
                    } else {
                        // there was an error in the parsing process -> return false
                        return false;
                    }
                } else if ( // therefore, bracket is closed!
                        open_bracket.counterpart.equals( bracket.symbol ) ||
                                setMode
                        ){
                    // this sequence ends her
                    // first of all, merge all elements together
                    int num = localTranslations.mergeAll();

                    // now, always wrap brackets around this sequence
                    // if the brackets are |.| for absolute value, translate it as a function
                    String seq;
                    if ( open_bracket.equals( Brackets.left_latex_abs_val ) ||
                            open_bracket.equals( Brackets.abs_val_open ) ){
                        BasicFunctionsTranslator bft = getConfig().getBasicFunctionsTranslator();
                        seq = bft.translate(
                                new String[]{
                                        stripMultiParentheses(localTranslations.removeLastExpression())
                                },
                                Keys.KEY_ABSOLUTE_VALUE
                                );
                    } else if ( setMode ){ // in set mode, both parenthesis may not match!
                        seq = open_bracket.getAppropriateString();
                        seq += localTranslations.removeLastExpression();
                        seq += bracket.getAppropriateString();
                    } else { // otherwise, parenthesis must match each other, so close as it opened
                        seq = open_bracket.getAppropriateString();
                        seq += localTranslations.removeLastExpression();
                        seq += open_bracket.getCounterPart().getAppropriateString();
                    }

                    // check if need to add multiply here
//                    if ( seq.matches( ".*\\s*\\)\\s*" ) ){
//                        MathTerm tmp = new MathTerm(")", MathTermTags.right_parenthesis.tag());
//                        exp = new PomTaggedExpression(tmp);
//                    }

                    seq = checkMultiplyAddition(exp, following_exp, seq);

                    // wrap parenthesis around sequence, this is one component of the sequence now
                    localTranslations.addTranslatedExpression( seq ); // replaced it

                    // same for global_exp. But first delete all elements of this sequence
                    TranslatedExpression global = super.getGlobalTranslationList();
                    global.removeLastNExps( num );
                    global.addTranslatedExpression( seq );
                    return true;
                } else { // otherwise there was an error in the bracket arrangements
                    throw new TranslationException(
                            "Bracket-Error: open bracket "
                            + open_bracket.symbol
                            + " reached " + bracket.symbol,
                            TranslationException.Reason.WRONG_PARENTHESIS);
                }
            }

            // if this term is not a bracket, then the term is something
            // else and needs to be parsed in the common way:
            TranslatedExpression inner_trans = parseGeneralExpression(exp, following_exp);

            // check, if we need to add space here
            String last = inner_trans.getLastExpression();
            boolean inner = false;
            if ( last == null ) {
                TranslatedExpression global = super.getGlobalTranslationList();
                last = global.getLastExpression();
                inner = true;
            }

//            if ( last.matches( ".*\\s*\\)\\s*" ) ){
//                MathTerm tmp = new MathTerm(")", MathTermTags.right_parenthesis.tag());
//                exp = new PomTaggedExpression(tmp);
//            }

            last = checkMultiplyAddition(exp, following_exp, last);

            inner_trans.replaceLastExpression( last );
            if ( inner ) localTranslations.replaceLastExpression( inner_trans.toString() );
            else localTranslations.addTranslatedExpression( inner_trans );

            // if there was in error, its over here...
            if ( isInnerError() ) return false;
        }

        // this should not happen. It means the algorithm reached the end but a bracket is left open.
        throw new TranslationException(
                "Reached the end of sequence but a bracket is left open: " +
                open_bracket.symbol,
                TranslationException.Reason.WRONG_PARENTHESIS
        );
    }

    private String checkMultiplyAddition( PomTaggedExpression exp, List<PomTaggedExpression> exp_list, String part ){
        String MULTIPLY = getConfig().getMULTIPLY();
        TranslatedExpression global = getGlobalTranslationList();

        if ( part.matches( STRING_END_TREAT_AS_CLOSED_PARANTHESIS ) ){
            MathTerm tmp = new MathTerm(")", MathTermTags.right_parenthesis.tag());
            exp = new PomTaggedExpression(tmp);
        } else if ( part.matches( ".*\\*\\s*" ) ) {
            exp = new PomTaggedExpression(new MathTerm("*", MathTermTags.multiply.tag()));
        }

//        if ( part.endsWith(MULTIPLY) ){
//            return part;
//        } else
        if ( open_bracket != null &&
                (open_bracket.equals(Brackets.abs_val_close) || open_bracket.equals(Brackets.abs_val_open) ) &&
                exp_list != null &&
                !exp_list.isEmpty() ) {
            MathTerm mt = exp_list.get(0).getRoot();
            if ( mt != null && mt.getTermText().matches(ABSOLUTE_VAL_TERM_TEXT_PATTERN))
                return part;
        }

        if ( addMultiply( exp, exp_list ) /*&& !part.matches(".*\\*\\s*")*/ ){
            part += MULTIPLY;
            // the global list already got each element before,
            // so simply replace the last if necessary
            String tmp = global.getLastExpression();
            global.replaceLastExpression(tmp + MULTIPLY);
        } else if ( addSpace( exp, exp_list ) ) {
            part += SPACE;
            // the global list already got each element before,
            // so simply replace the last if necessary
            String tmp = global.getLastExpression();
            global.replaceLastExpression(tmp + SPACE);
        }
        return part;
    }

    /**
     * Returns true if there has to be a space symbol following the current expression.
     * @param currExp the current expression
     * @param exp_list the following expressions
     * @return true if the current expressions needs an white space symbol behind its translation
     */
    private boolean addSpace(PomTaggedExpression currExp, List<PomTaggedExpression> exp_list ){
        try {
            if ( exp_list == null || exp_list.size() < 1) return false;
            MathTerm curr = currExp.getRoot();
            MathTerm next = exp_list.get(0).getRoot();
            return !(curr.getTag().matches(PARENTHESIS_PATTERN)
                    || next.getTag().matches(PARENTHESIS_PATTERN)
                    || next.getTermText().matches(SPECIAL_SYMBOL_PATTERN_FOR_SPACES)
            );
        } catch ( Exception e ){ return false; }
    }

    /**
     *
     * @param term
     * @param currentOpenBracket
     * @return
     */
    public static Brackets ifIsBracketTransform(MathTerm term, Brackets currentOpenBracket) {
        if ( term == null || term.isEmpty() ) return null;
        if ( term.getTag() != null && (
                term.getTag().matches(PARENTHESIS_PATTERN) || term.getTermText().matches( ABSOLUTE_VAL_TERM_TEXT_PATTERN ))
                ) {
            Brackets bracket = Brackets.getBracket(term.getTermText());
            if ( currentOpenBracket != null && bracket != null &&
                    bracket.equals(Brackets.abs_val_open) &&
                    currentOpenBracket.equals(Brackets.abs_val_open) ) {
                return Brackets.abs_val_close;
            } else return bracket;
        } else return null;
    }
}
