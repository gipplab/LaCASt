package gov.nist.drmf.interpreter.cas.parser.components;

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

    public SequenceParser(){

    }

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
            errorMessage = "You used the wrong parse method. The given expression is not a sequence! " +
                    expression.getTag() + System.lineSeparator();
            return false;
        }

        String sequence = "";
        List<PomTaggedExpression> exp_list = expression.getComponents();

        while ( !exp_list.isEmpty() ){
            PomTaggedExpression exp = exp_list.remove(0);
            sequence += parseGeneralExpression( exp, exp_list );

            /*
            if ( exp_list.size() >= 1 ){
                if ( ( !exp_list.get(0).getRoot().isEmpty() &&
                        exp_list.get(0).getRoot().getTag().matches(PARENTHESIS_PATTERN) )
                        ){//||
                        //( !exp.getRoot().isEmpty() &&
                        //        exp.getRoot().getTag().matches(PARENTHESIS_PATTERN) ) ){

                } else sequence += SPACE;
            }
            */
            if ( spaceOrNot( exp_list ) ) sequence += SPACE;
        }

        if ( isInnerError() ) return false;
        translatedExp += sequence;
        //translatedExp += open_bracket == null ? "" : open_bracket.counterpart;
        return true;
    }

    @Override
    public boolean parse(List<PomTaggedExpression> following_exp) {
        while ( !following_exp.isEmpty() ){
            // take the next expression
            PomTaggedExpression exp = following_exp.remove(0);

            // if this expression has no term, it cannot be a single symbol
            /*if ( !containsTerm(exp) ) {
                // in that case, use the empty expression parser
                EmptyExpressionParser parser = new EmptyExpressionParser();
                if ( parser.parse(exp) ){
                    translatedExp += parser.getTranslatedExpression();
                    continue;
                } else {
                    errorMessage += parser.getErrorMessage();
                    return false;
                }
            }*/
            if ( !containsTerm(exp) ){
                translatedExp += parseGeneralExpression(exp, following_exp);
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
                        translatedExp += sp.translatedExp;
                        continue;
                    } else {
                        errorMessage += sp.errorMessage;
                        return false;
                    }
                } else if ( open_bracket.counterpart.equals( bracket.symbol ) ){
                    // this sequence ends here
                    translatedExp =
                            open_bracket.symbol +
                            translatedExp +
                            open_bracket.counterpart;
                    return true;
                } else {
                    errorMessage += "Bracket-Error: open bracket "
                            + open_bracket.symbol
                            + " reached " + bracket.symbol + System.lineSeparator();
                    return false;
                }
            }

            // if this term is not a bracket, then the term is something
            // else and needs to be parsed in the common way:
            /*
            MathTermParser mp = new MathTermParser();
            if ( mp.parse( term ) ){
                translatedExp += mp.getTranslatedExpression();
            } else {
                errorMessage += mp.getErrorMessage();
                return false;
            }
            */
            translatedExp += parseGeneralExpression(exp, following_exp);
            if ( spaceOrNot( following_exp ) ) translatedExp += SPACE;
            if ( isInnerError() ) return false;
        }

        // this should not happen. It means the algorithm reached the end but a bracket is
        // left open.
        errorMessage +=
                "Reached the end of sequence but a bracket is left open: " +
                        open_bracket.symbol + System.lineSeparator();
        return false;
    }

    private boolean spaceOrNot( List<PomTaggedExpression> exp_list ){
        if ( exp_list.size() >= 1 ){
            if ( ( !exp_list.get(0).getRoot().isEmpty() &&
                    exp_list.get(0).getRoot().getTag().matches(PARENTHESIS_PATTERN) )
                    ){//||
                //( !exp.getRoot().isEmpty() &&
                //        exp.getRoot().getTag().matches(PARENTHESIS_PATTERN) ) ){
                return false;
            } else return true;
        }
        return false;
    }
}
