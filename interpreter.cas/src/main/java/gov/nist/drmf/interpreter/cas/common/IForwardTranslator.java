package gov.nist.drmf.interpreter.cas.common;

import gov.nist.drmf.interpreter.cas.logging.TranslatedExpression;
import gov.nist.drmf.interpreter.common.grammar.ITranslator;
import mlp.PomTaggedExpression;

import javax.annotation.Nullable;

/**
 * @author Andre Greiner-Petter
 */
public interface IForwardTranslator extends ITranslator<PomTaggedExpression> {
    /**
     * Returns the translated expression. This
     * value could be null, if the {@link #translate(Object)}
     * returns false before.
     * @return  the translated expression given in
     *          {@link #translate(Object)} or null
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
