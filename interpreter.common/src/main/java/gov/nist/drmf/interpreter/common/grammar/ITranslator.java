package gov.nist.drmf.interpreter.common.grammar;

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
     * This method parses a given expression.
     * It returns true if the parsing process
     * finished without an error.
     *
     * @param expression tagged expression
     * @return  true if the parsing process finished
     *          without an error.
     * @throws Exception If the translation process failed.
     */
    String translate( String expression ) throws TranslationException;
}
