package gov.nist.drmf.interpreter.cas.parser.components;

import gov.nist.drmf.interpreter.cas.logging.TranslatedExpression;
import gov.nist.drmf.interpreter.cas.parser.AbstractParser;
import gov.nist.drmf.interpreter.cas.parser.SemanticLatexParser;
import gov.nist.drmf.interpreter.common.grammar.Brackets;
import gov.nist.drmf.interpreter.common.grammar.ExpressionTags;
import gov.nist.drmf.interpreter.common.grammar.MathTermTags;
import mlp.PomTaggedExpression;

import java.util.List;

/**
 * @author Andre Greiner-Petter
 */
public class EmptyExpressionParser extends AbstractParser {
    @Override
    public boolean parse( PomTaggedExpression expression ) {
        String tag = expression.getTag();
        ExpressionTags expTag = ExpressionTags.getTagByKey(tag);
        if ( expTag == null ){
            ERROR_LOG.warning("Could not find tag: " + tag);
            return false;
        }
        switch( expTag ){
            case sequence:
                SequenceParser p = new SequenceParser();
                if ( p.parse( expression ) ){
                    translatedExp.addTranslatedExpression( p.getTranslatedExpressionObject() );
                    return true;
                } else return false;
            case fraction:
            case binomial:
            case square_root:
            case general_root:
                String[] comps = extractMultipleSubExpressions(expression);
                if ( isInnerError() ){
                    return false;
                }

                translatedExp.addTranslatedExpression(
                        SemanticLatexParser.getBasicFunctionParser().translate(
                                comps,
                                expTag.tag()
                        )
                );
                return true;
            case balanced_expression:
                List<PomTaggedExpression> sub_exps = expression.getComponents();
                if ( sub_exps.size() < 3 ){
                    ERROR_LOG.warning("Found empty expression and ignored it.");
                    return true;
                }
                PomTaggedExpression first = sub_exps.remove(0);
                PomTaggedExpression last = sub_exps.remove( sub_exps.size()-1 );

                // test open-close style of first-last
                if ( !testParanthesis(first, last) ){
                    ERROR_LOG.severe("Error in delimiters. The open delimiter doesn't fit with the closed delimiter.");
                    return false;
                }

                translatedExp.addTranslatedExpression(
                        Brackets.left_parenthesis.symbol +
                        parseGeneralExpression( sub_exps.remove(0), sub_exps ).toString() +
                        Brackets.left_parenthesis.counterpart
                );
                return true;
            case sub_super_script:
            case numerator:
            case denominator:
            case equation:
            default:
                ERROR_LOG.warning("Reached unknown or not yet supported expression tag: " + tag);
                return false;
        }
    }

    /**
     * A helper method to extract some sub-expressions. Useful for short
     * functions like \frac{a}{b}. The given argument is the parent expression
     * of several children. As an example a fraction expression has two children,
     * the numerator and the denominator.
     *
     * @param topExpression parent expression of underlying sub-expressions.
     * @return true if the parsing process finished successful
     */
    private String[] extractMultipleSubExpressions( PomTaggedExpression topExpression ){
        List<PomTaggedExpression> list = topExpression.getComponents();
        String[] components = new String[list.size()];
        for ( int i = 0; i < list.size(); i++ ){
            components[i] = parseGeneralExpression(list.get(i), null).toString();
        }
        return components;
    }

    private boolean testParanthesis( PomTaggedExpression first, PomTaggedExpression last ){
        try {
            MathTermTags ftag = MathTermTags.getTagByKey( first.getRoot().getTag() );
            MathTermTags ltag = MathTermTags.getTagByKey( last.getRoot().getTag() );
            if ( !ftag.equals( MathTermTags.left_delimiter ) ) return false;
            if ( !ltag.equals( MathTermTags.right_delimiter ) ) return false;
            String left = first.getRoot().getTermText();
            left = left.substring( left.length()-1 ); // last symbol (
            String right = last.getRoot().getTermText();
            right = right.substring( right.length()-1 ); // last symbol )
            Brackets lBracket = Brackets.getBracket(left);
            Brackets rBracket = Brackets.getBracket(right);
            return Brackets.getBracket(lBracket.counterpart).equals(rBracket);
        } catch ( Exception e ){
            return false;
        }
    }
}
