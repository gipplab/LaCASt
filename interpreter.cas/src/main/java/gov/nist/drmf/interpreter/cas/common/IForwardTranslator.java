package gov.nist.drmf.interpreter.cas.common;

import gov.nist.drmf.interpreter.cas.logging.TranslatedExpression;
import gov.nist.drmf.interpreter.common.grammar.ITranslatorComponent;
import mlp.PomTaggedExpression;

import javax.annotation.Nullable;

/**
 * @author Andre Greiner-Petter
 */
public interface IForwardTranslator extends ITranslatorComponent<PomTaggedExpression> {
    /**
     * Returns the translated expression.
     * @return  the translated expression given
     */
    @Nullable
    TranslatedExpression getTranslatedExpressionObject();

    /**
     * Returns the string representation of the translated expression.
     * @return string (might be empty)
     */
    default String getTranslatedExpression() {
        if ( getTranslatedExpressionObject() == null ) return "";
        else return getTranslatedExpressionObject().getTranslatedExpression();
    }

}
