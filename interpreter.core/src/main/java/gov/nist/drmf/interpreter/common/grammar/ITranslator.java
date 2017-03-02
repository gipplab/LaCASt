package gov.nist.drmf.interpreter.common.grammar;

import com.sun.istack.internal.Nullable;

/**
 * A general translation has at least two methods.
 * One to translate the expression and one to get
 * the result of the parsed expression.
 *
 * @author Andre Greiner-Petter
 */
public interface ITranslator<T> {
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
     */
    boolean translate(T expression);

    /**
     * Returns the translated expression. This
     * value could be null, if the {@link #translate(T)}
     * returns false before.
     * @return  the translated expression given in
     *          {@link #translate(T)} or nul
     */
    @Nullable
    String getTranslatedExpression();
}
