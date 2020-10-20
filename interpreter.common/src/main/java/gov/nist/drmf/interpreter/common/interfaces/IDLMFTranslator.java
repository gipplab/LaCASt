package gov.nist.drmf.interpreter.common.interfaces;

import gov.nist.drmf.interpreter.common.TranslationInformation;
import gov.nist.drmf.interpreter.common.exceptions.TranslationException;

/**
 * This extension of an translator allows to pass DLMF labels due the translation process
 */
public interface IDLMFTranslator extends ITranslator {
    /**
     * Translates the given expression.
     * @param expression expression
     * @param label a dlmf label such as '1.1.E2' or '1.4' or '12.3.Ex1' etc.
     * @return the string representation of the translation
     * @throws TranslationException If the translation process failed.
     */
    default String translate( String expression, String label ) throws TranslationException {
        TranslationInformation ti = translateToObject(expression, label);
        return ti.getTranslatedExpression();
    }

    /**
     * Equivalent to {@link #translate(String)} but it returns a {@link TranslationInformation} object
     * rather than just the string translation.
     * @param expression expression
     * @param label a dlmf label such as '1.1.E2' or '1.4' or '12.3.Ex1' etc.
     * @return all information about the translation process
     * @throws TranslationException if an error due translation occurs.
     */
    TranslationInformation translateToObject( String expression, String label ) throws TranslationException;
}
