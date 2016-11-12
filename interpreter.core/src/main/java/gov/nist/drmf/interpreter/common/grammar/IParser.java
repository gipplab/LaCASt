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
public interface IParser {
    /**
     * This method parses a given expression.
     * It returns true if the parsing process
     * finished without an error. Otherwise
     * it returns false and once can get the
     * error message by invoke {@link #getErrorMessage()}.
     * If the parsing process finished without an
     * error, the translated expression can get
     * by {@link #getTranslatedExpression()}.
     *
     * @param expression tagged expression
     * @return  true if the parsing process finished
     *          without an error.
     */
    boolean parse(PomTaggedExpression expression);

    /**
     * Returns the translated expression. This
     * value could be null, if the {@link #parse(PomTaggedExpression)}
     * returns false before.
     * @return  the translated expression given in
     *          {@link #parse(PomTaggedExpression)} or nul
     */
    @Nullable
    String getTranslatedExpression();

    /**
     * Returns the error message from {@link #parse(PomTaggedExpression)}
     * or null, if the parsing process finished without an
     * error.
     * @return error message or null
     */
    @Nullable
    String getErrorMessage();
}
