package gov.nist.drmf.interpreter.cas.parser.components;

import gov.nist.drmf.interpreter.cas.SemanticToCASInterpreter;
import gov.nist.drmf.interpreter.cas.parser.AbstractParser;
import gov.nist.drmf.interpreter.cas.parser.SemanticLatexParser;
import gov.nist.drmf.interpreter.common.grammar.ExpressionTags;
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
            errorMessage += "Could not find tag: " + tag;
            return false;
        }
        switch( expTag ){
            case sequence:
                SequenceParser p = new SequenceParser();
                if ( p.parse( expression ) ){
                    translatedExp += p.getTranslatedExpression();
                    extraInformation += p.getExtraInformation();
                    return true;
                } else {
                    errorMessage += p.getErrorMessage() + System.lineSeparator();
                    return false;
                }
            case fraction:
            case binomial:
            case square_root:
            case general_root:
                String[] comps = extractMultipleSubExpressions(expression);
                if ( isInnerError() ){
                    return false;
                }

                translatedExp +=
                        SemanticToCASInterpreter.FUNCTIONS.translate(
                                comps,
                                expTag.tag()
                        );
                return true;
            case sub_super_script:
            case numerator:
            case denominator:
            case equation:
            default:
                errorMessage += "Unknown or not yet supported expression tag: "
                        + tag
                        + System.lineSeparator();
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
            components[i] = parseGeneralExpression(list.get(i), null);
        }
        return components;
    }
}
