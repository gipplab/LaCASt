package gov.nist.drmf.interpreter.common.interfaces;

import gov.nist.drmf.interpreter.common.TranslationInformation;
import gov.nist.drmf.interpreter.common.exceptions.TranslationException;

/**
 * This extension of an translator allows to pass DLMF labels due the translation process
 */
public interface IDLMFTranslator<T> extends ITranslator {
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
    default TranslationInformation translateToObject( String expression, String label ) throws TranslationException {
        return translateToObject(expression, label, null);
    }

    /**
     * Equivalent to {@link #translateToObject(String, String)} but triggers given translation features in addition.
     * @param expression expression
     * @param label a dlmf label such as '1.1.E2'
     * @param translationFeatures a translation feature
     * @return all information about the translation process
     * @throws TranslationException if an error occurred due translating the expression
     */
    TranslationInformation translateToObject( String expression, String label, TranslationFeature<T> translationFeatures ) throws TranslationException;

    /**
     * Equivalent to {@link #translateToObject(String, String, TranslationFeature)} but without a given label
     * @param expression expression
     * @param translationFeatures a translation feature
     * @return the translation information
     */
    default TranslationInformation translateToObjectFeatured( String expression, TranslationFeature<T> translationFeatures ) {
        return translateToObject(expression, null, translationFeatures);
    }
}
