package gov.nist.drmf.interpreter.cas.parser.components;

import com.sun.istack.internal.Nullable;
import gov.nist.drmf.interpreter.cas.logging.TranslatedExpression;
import gov.nist.drmf.interpreter.cas.parser.AbstractListParser;
import gov.nist.drmf.interpreter.common.grammar.Brackets;
import gov.nist.drmf.interpreter.common.grammar.ExpressionTags;
import mlp.MathTerm;
import mlp.PomTaggedExpression;

import java.util.List;

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
 * @see AbstractListParser
 * @see gov.nist.drmf.interpreter.cas.parser.AbstractParser
 * @author Andre Greiner-Petter
 */
public class SequenceParser extends AbstractListParser {
    // the open bracket if needed
    @Nullable
    private Brackets open_bracket;

    /**
     * Uses only for a general sequence expression.
     * If the tag is sequence we don't need to check any parenthesis.
     */
    public SequenceParser(){}

    /**
     * Use this if the sequence is wrapped by parenthesis.
     * In that we don't know the length of the sequence. The sequence
     * ends when we reach the next corresponding bracket, matches to
     * the open bracket.
     * @param open_bracket the following sequence is wrapped by brackets
     *                     the given bracket is the first open bracket of the following
     *                     sequence
     */
    public SequenceParser( Brackets open_bracket ){
        this.open_bracket = open_bracket;
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
    public boolean parse(PomTaggedExpression expression){
        if ( !ExpressionTags.sequence.tag().matches(expression.getTag()) ){
            ERROR_LOG.severe("You used the wrong parser method. " +
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
            TranslatedExpression inner_translation =
                    parseGeneralExpression( exp, exp_list );

            // only take the last object and check if it is
            // necessary to add a space character behind
            part = inner_translation.getLastExpression();
            if ( addSpace( exp, exp_list ) ) {
                part += SPACE;
                // the global list already got each element before,
                // so simply replace the last if necessary
                global_exp.replaceLastExpression(part);
            }

            // finally add all elements to the inner list
            inner_translation.replaceLastExpression(part);
            local_inner_exp.addTranslatedExpression( inner_translation );
        }

        // finally return value
        return !isInnerError();
    }

    /**
     * Use this function ONLY when you created an object of this class
     * with a given bracket {@link SequenceParser#SequenceParser(Brackets)}.
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
     * @return true when the parser finished without an error.
     */
    @Override
    public boolean parse(List<PomTaggedExpression> following_exp) {
        if ( open_bracket == null ){
            ERROR_LOG.severe("Wrong parser method used. " +
                    "You have to specify an open bracket to parse it like a sequence " +
                    "that way.");
            return false;
        }

        // iterate through all elements
        while ( !following_exp.isEmpty() ){
            // take the next expression
            PomTaggedExpression exp = following_exp.remove(0);

            // if the next expression does not contains
            // a math term object, simply parse it as a general expression
            if ( !containsTerm(exp) ){
                TranslatedExpression inner_translation =
                        parseGeneralExpression(exp, following_exp);

                // don't forget to check spaces:
                String last = inner_translation.removeLastExpression();
                if ( addSpace( exp, following_exp ) ){
                    last += SPACE;
                    global_exp.replaceLastExpression( last );
                }
                inner_translation.addTranslatedExpression( last );
                local_inner_exp.addTranslatedExpression( inner_translation );
                // don't need to add elements to global_exp because they were
                // already added from other methods.

                // if there was in error, we can stop here
                if ( isInnerError() ) return false;
                // otherwise take the next expression
                else continue;
            }

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
            if ( term.getTag().matches(PARENTHESIS_PATTERN) ){
                // get the bracket
                Brackets bracket = Brackets.getBracket( term.getTermText() );

                // another open bracket -> reached a new sub sequence
                // bracket cannot be null, because we checked the tag of the term before
                //noinspection ConstantConditions
                if ( bracket.opened ){
                    // create a new SequenceParser (2nd kind)
                    SequenceParser sp = new SequenceParser( bracket );
                    // parse the following expressions
                    if ( sp.parse(following_exp) ){
                        // if the parser finished correctly, there is nothing to do here
                        // only take all of the inner solutions
                        local_inner_exp.addTranslatedExpression( sp.local_inner_exp );
                        // we don't need to add/remove elements from global_exp here
                        continue;
                    } else {
                        // there was an error in the parsing process -> return false
                        return false;
                    }
                } else if ( open_bracket.counterpart.equals( bracket.symbol ) ){
                    // this sequence ends her
                    // first of all, merge all elements together
                    int num = local_inner_exp.mergeAll();

                    // now, always wrap elements around this sequence
                    String seq =
                            open_bracket.symbol +
                                    local_inner_exp.removeLastExpression() + // removed all
                                    open_bracket.counterpart;

                    // wrap parenthesis around sequence, this is one component of the sequence now
                    local_inner_exp.addTranslatedExpression( seq ); // replaced it

                    // same for global_exp. But first delete all elements of this sequence
                    global_exp.removeLastNExps( num );
                    global_exp.addTranslatedExpression( seq );
                    return true;
                } else { // otherwise there was an error in the bracket arrangements
                    ERROR_LOG.severe("Bracket-Error: open bracket "
                            + open_bracket.symbol
                            + " reached " + bracket.symbol);
                    return false;
                }
            }

            // if this term is not a bracket, then the term is something
            // else and needs to be parsed in the common way:
            TranslatedExpression inner_trans = parseGeneralExpression(exp, following_exp);

            // check, if we need to add space here
            String last = inner_trans.removeLastExpression();
            if ( addSpace( exp, following_exp ) ) {
                last += SPACE;
                global_exp.replaceLastExpression( last );
            }
            inner_trans.addTranslatedExpression( last );
            local_inner_exp.addTranslatedExpression( inner_trans );

            // if there was in error, its over here...
            if ( isInnerError() ) return false;
        }

        // this should not happen. It means the algorithm reached the end but a bracket is
        // left open.
        ERROR_LOG.severe(
                "Reached the end of sequence but a bracket is left open: " +
                        open_bracket.symbol);
        return false;
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
                    || next.getTag().matches(PARENTHESIS_PATTERN));
        } catch ( Exception e ){ return true; }
    }
}
