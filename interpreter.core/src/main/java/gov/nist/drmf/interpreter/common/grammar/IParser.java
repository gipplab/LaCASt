package gov.nist.drmf.interpreter.common.grammar;

import com.sun.istack.internal.Nullable;
import mlp.PomTaggedExpression;

/**
 * A general parser has at least two methods.
 * One to parse the expression and one to get
 * the result of the parsed expression.
 *
 * @author Andre Greiner-Petter
 */
public interface IParser<T> {
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
    boolean parse(T expression);

    /**
     * Returns the translated expression. This
     * value could be null, if the {@link #parse(T)}
     * returns false before.
     * @return  the translated expression given in
     *          {@link #parse(T)} or nul
     */
    @Nullable
    String getTranslatedExpression();
}
