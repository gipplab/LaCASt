package gov.nist.drmf.interpreter.common.grammar;

/**
 * @author Andre Greiner-Petter
 */
public interface ITranslatorComponent<T> {
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
    boolean translate( T expression ) throws Exception;

    /**
     * Returns the string representation of the translated expression.
     * @return string (might be empty)
     */
    String getTranslatedExpression();
}
