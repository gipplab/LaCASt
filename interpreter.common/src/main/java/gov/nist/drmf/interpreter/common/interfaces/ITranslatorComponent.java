package gov.nist.drmf.interpreter.common.interfaces;

import gov.nist.drmf.interpreter.common.TranslationProcessConfig;
import gov.nist.drmf.interpreter.common.exceptions.TranslationException;

/**
 * @author Andre Greiner-Petter
 */
public interface ITranslatorComponent<IN, OUT> {
    /**
     * This method parses a given expression.
     * It returns true if the parsing process
     * finished without an error.
     * If the parsing process finished without an
     * error, the translated expression can get
     * by {@link #getTranslatedExpression()}.
     *
     * @param expression tagged expression
     * @return  true if the parsing process finished
     *          without an error.
     * @throws Exception If the translation process failed.
     */
    OUT translate( IN expression ) throws TranslationException;

    /**
     * Returns the string representation of the translated expression.
     * @return string (might be empty)
     */
    String getTranslatedExpression();

    /**
     * @return the configuration of the translator
     */
    TranslationProcessConfig getConfig();

    /**
     * Returns the source language of the forward translator. Until we cracked the holy grail of MathIR,
     * this always returns DLMF.
     * @return the source language
     */
    default String getSourceLanguage() {
        return getConfig().getFROM_LANGUAGE();
    }

    /**
     * Returns the target language, e.g., Maple or Mathematica.
     * @return the target language
     */
    default String getTargetLanguage() {
        return getConfig().getTO_LANGUAGE();
    }
}
