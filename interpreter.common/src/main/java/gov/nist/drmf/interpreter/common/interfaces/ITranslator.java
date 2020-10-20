package gov.nist.drmf.interpreter.common.interfaces;

import gov.nist.drmf.interpreter.common.TranslationInformation;
import gov.nist.drmf.interpreter.common.exceptions.TranslationException;

import java.util.Arrays;

/**
 * A general translation has at least two methods.
 * One to translate the expression and one to get
 * the result of the parsed expression.
 *
 * @author Andre Greiner-Petter
 */
public interface ITranslator {
    /**
     * Translates the given expression.
     * @param expression expression
     * @return the string representation of the translation
     * @throws TranslationException If the translation process failed.
     */
    default String translate( String expression ) throws TranslationException {
        TranslationInformation ti = translateToObject(expression);
        return ti.getTranslatedExpression();
    }

    /**
     * Equivalent to {@link #translate(String)} but it returns a {@link TranslationInformation} object
     * rather than just the string translation.
     * @param expression expression
     * @return all information about the translation process
     * @throws TranslationException if an error due translation occurs.
     */
    TranslationInformation translateToObject( String expression ) throws TranslationException;
}
