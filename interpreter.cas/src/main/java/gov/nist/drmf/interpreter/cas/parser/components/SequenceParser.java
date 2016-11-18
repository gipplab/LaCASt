package gov.nist.drmf.interpreter.cas.parser.components;

import gov.nist.drmf.interpreter.cas.logging.TranslatedExpression;
import gov.nist.drmf.interpreter.cas.parser.AbstractListParser;
import gov.nist.drmf.interpreter.common.grammar.Brackets;
import gov.nist.drmf.interpreter.common.grammar.ExpressionTags;
import mlp.MathTerm;
import mlp.PomTaggedExpression;

import java.util.List;

/**
 * @author Andre Greiner-Petter
 */
public class SequenceParser extends AbstractListParser {
    private Brackets open_bracket;

    public SequenceParser(){}

    public SequenceParser( Brackets open_bracket ){
        this.open_bracket = open_bracket;
    }

    /**
     * This method parses a PomTaggedExpression of type sequence and
     * only these expressions!
     *
     * @param expression with "sequence" tag!
     * @return true if the parsing process finish correctly
     *          otherwise false
     */
    @Override
    public boolean parse(PomTaggedExpression expression){
        if ( !ExpressionTags.sequence.tag().matches(expression.getTag()) ){
            ERROR_LOG.severe("You used the wrong parse method. The given expression is not a sequence! " +
                    expression.getTag());
            return false;
        }

        String part = "";
        List<PomTaggedExpression> exp_list = expression.getComponents();

        while ( !exp_list.isEmpty() ){
            PomTaggedExpression exp = exp_list.remove(0);
            part = parseGeneralExpression( exp, exp_list ).toString();
            if ( addSpace( exp, exp_list ) ) part += SPACE;
            translatedExp.addTranslatedExpression( part );
        }

        if ( isInnerError() ) return false;
        //translatedExp.addTranslatedExpression(sequence);
        return true;
    }

    @Override
    public boolean parse(List<PomTaggedExpression> following_exp) {
        while ( !following_exp.isEmpty() ){
            // take the next expression
            PomTaggedExpression exp = following_exp.remove(0);

            if ( !containsTerm(exp) ){
                translatedExp.addTranslatedExpression(
                        parseGeneralExpression(exp, following_exp).toString()
                );
                if ( isInnerError() ) return false;
                else continue;
            }

            // otherwise investigate the term
            MathTerm term = exp.getRoot();
            /**
             * If this term is a bracket there are three possible options
             *  1) another open bracket -> reached a sub sequence
             *  2) a closed bracket which is the counterpart of the first open bracket
             *          -> this sequence ends here
             *  3) another closed bracket -> there is a bracket error in the sequence
             */
            if ( term.getTag().matches(PARENTHESIS_PATTERN) ){
                Brackets bracket = Brackets.getBracket( term.getTermText() );
                if ( bracket.opened ){
                    // a sub-sequence starts here
                    SequenceParser sp = new SequenceParser( bracket );
                    if ( sp.parse(following_exp) ){
                        translatedExp.addTranslatedExpression(sp.translatedExp.toString());
                        continue;
                    } else {
                        return false;
                    }
                } else if ( open_bracket.counterpart.equals( bracket.symbol ) ){
                    // this sequence ends here
                    String seq = translatedExp.toString(); // get whole sequence from start
                    translatedExp = new TranslatedExpression(); // clear sequence complete

                    // wrap parenthesis around sequence, this is one component of the sequence now
                    translatedExp.addTranslatedExpression(
                            open_bracket.symbol +
                            seq +
                            open_bracket.counterpart
                    );
                    return true;
                } else {
                    ERROR_LOG.severe("Bracket-Error: open bracket "
                            + open_bracket.symbol
                            + " reached " + bracket.symbol);
                    return false;
                }
            }

            // if this term is not a bracket, then the term is something
            // else and needs to be parsed in the common way:
            String next_element = parseGeneralExpression(exp, following_exp).toString();
            if ( addSpace( exp, following_exp ) ) next_element += SPACE;
            translatedExp.addTranslatedExpression(next_element);
            if ( isInnerError() ) return false;
        }

        // this should not happen. It means the algorithm reached the end but a bracket is
        // left open.
        ERROR_LOG.severe(
                "Reached the end of sequence but a bracket is left open: " +
                        open_bracket.symbol);
        return false;
    }

    private boolean addSpace(PomTaggedExpression currExp, List<PomTaggedExpression> exp_list ){
        try {
            if ( exp_list == null || exp_list.size() < 1) return false;
            MathTerm curr = currExp.getRoot();
            MathTerm next = exp_list.get(0).getRoot();
            if ( curr.getTag().matches(PARENTHESIS_PATTERN)
                    || next.getTag().matches(PARENTHESIS_PATTERN))
                return false;
            else return true;
        } catch ( Exception e ){ return true; }
    }
}
