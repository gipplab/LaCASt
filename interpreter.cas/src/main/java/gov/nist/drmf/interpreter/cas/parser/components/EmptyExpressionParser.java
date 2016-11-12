package gov.nist.drmf.interpreter.cas.parser.components;

import gov.nist.drmf.interpreter.cas.parser.AbstractParser;
import gov.nist.drmf.interpreter.cas.parser.SemanticLatexParser;
import gov.nist.drmf.interpreter.common.grammar.ExpressionTags;
import mlp.PomTaggedExpression;

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
                if ( parseSequence( expression ) ){
                    translatedExp += getSequence();
                    return true;
                } else return false;
            case fraction:
                if (extractMultipleSubExpressions(expression)){
                    translatedExp +=
                            "(" +
                                    getElements()[0] +
                                    ")/(" +
                                    getElements()[1] +
                            ")";
                    return true;
                } else return false;
            case binomial:
                if (extractMultipleSubExpressions(expression)){
                    translatedExp +=
                            "binomial(" +
                                    getElements()[0] +
                                    "," +
                                    getElements()[1] +
                            ")";
                    return true;
                } else return false;
            case square_root:
                AbstractParser inner_parser = new SemanticLatexParser();
                // TODO is it possible to have multiple components here?
                if ( inner_parser.parse( expression.getComponents().get(0) ) ) {
                    translatedExp += "sqrt(" + inner_parser.getTranslatedExpression() + ")";
                    return true;
                } else {
                    errorMessage += "Could not parse argument of square root: "+
                            System.lineSeparator()+
                            inner_parser.getErrorMessage()+
                            System.lineSeparator();
                    return false;
                }
            case general_root:
                if (extractMultipleSubExpressions(expression)){
                    translatedExp +=
                            "surd(" +
                                    getElements()[1] +
                                    "," +
                                    getElements()[2] +
                            ")";
                    return true;
                } else return false;
            case sub_super_script:
            case numerator:
            case denominator:
            case equation:
                translatedExp = SPACE;
                return true;
            default:
                errorMessage += "Unknown expression tag: "
                        + tag
                        + System.lineSeparator();
                return false;
        }
    }
}
