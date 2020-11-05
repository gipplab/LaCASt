package gov.nist.drmf.interpreter.pom.extensions;

import gov.nist.drmf.interpreter.common.exceptions.NotMatchableException;
import gov.nist.drmf.interpreter.pom.MLPWrapper;
import gov.nist.drmf.interpreter.pom.SemanticMLPWrapper;
import mlp.ParseException;
import mlp.PomTaggedExpression;
import org.intellij.lang.annotations.Language;

/**
 * This helper class provides more useful methods to create {@link MatchablePomTaggedExpression}
 * and {@link PomMatcher} objects.
 * @author Andre Greiner-Petter
 */
public final class PomMatcherBuilder {
    private PomMatcherBuilder() {}

    /**
     * This expression does not contain wildcards! If you want to use wildcards, use one of the
     * other constructors.
     * @param expression the expression to match without wildcards.
     * @return new matchable PTE
     * @throws ParseException if the given latex string cannot be parsed
     * @throws NotMatchableException if the given expression cannot be matched
     */
    public static MatchablePomTaggedExpression compile(String expression) throws ParseException, NotMatchableException {
        return compile(expression, "");
    }

    /**
     * This expression does not contain wildcards! If you want to use wildcards, use one of the
     * other constructors.
     * @param refRoot the expression to match without wildcards.
     * @return new matchable PTE
     * @throws NotMatchableException if the given expression cannot be matched
     */
    public static MatchablePomTaggedExpression compile(PomTaggedExpression refRoot) throws NotMatchableException {
        return compile(refRoot, "");
    }

    /**
     * It uses the standard instance the parser via {@link SemanticMLPWrapper#getStandardInstance()}.
     * @param expression the expression to create a matchable tree
     * @param wildcardPattern the regex to find wildcards (e.g., var\d+).
     * @return new matchable PTE
     * @throws ParseException if the {@link MLPWrapper} is unable to parse the expression
     * @throws NotMatchableException if the given expression cannot be matched
     */
    public static MatchablePomTaggedExpression compile(String expression, @Language("RegExp") String wildcardPattern) throws ParseException, NotMatchableException {
        return compile(SemanticMLPWrapper.getStandardInstance(), expression, wildcardPattern);
    }

    /**
     * Copy constructor to extend a {@link PomTaggedExpression} and its components to
     * a {@link MatchablePomTaggedExpression}.
     * @param refRoot         the reference {@link PomTaggedExpression}
     * @param wildcardPattern a regex that defines the set of wildcards
     * @return new matchable PTE
     * @throws NotMatchableException if the given expression cannot be matched
     */
    public static MatchablePomTaggedExpression compile(PomTaggedExpression refRoot, @Language("RegExp") String wildcardPattern) throws NotMatchableException {
        return compile(SemanticMLPWrapper.getStandardInstance(), refRoot, wildcardPattern);
    }

    /**
     * For better performance, it is recommended to have one MLPWrapper instance.
     * Hence, use one of the constructors to provide the instance you are using here.
     *
     * This constructor does not allow any wildcards.
     *
     * @param mlp the mlp wrapper to parse the expression
     * @param expression the expression to create a matchable tree
     * @return matchable PTE
     * @throws ParseException if the {@link MLPWrapper} is unable to parse the expression
     * @throws NotMatchableException if the given expression cannot be matched
     */
    public static MatchablePomTaggedExpression compile(MLPWrapper mlp, String expression)
            throws ParseException, NotMatchableException{
        return compile(mlp, expression, "");
    }

    /**
     * For better performance, it is recommended to have one MLPWrapper object.
     * So if not necessary,
     * @param mlp the mlp wrapper to parse the expression
     * @param expression the expression to create a matchable tree
     * @param wildcardPattern the regex to find wildcards (e.g., var\d+).
     * @return new matchable PTE
     * @throws ParseException if the {@link MLPWrapper} is unable to parse the expression
     * @throws NotMatchableException if the given expression cannot be matched
     */
    public static MatchablePomTaggedExpression compile(
            MLPWrapper mlp,
            String expression,
            @Language("RegExp") String wildcardPattern
    ) throws ParseException, NotMatchableException {
        return compile(mlp, mlp.parse(expression), wildcardPattern);
    }

    /**
     * Copy constructor to extend a {@link PomTaggedExpression} and its components to
     * a {@link MatchablePomTaggedExpression}.
     * @param mlp             the MLP engine to use for matching expressions
     * @param refRoot         the reference {@link PomTaggedExpression}
     * @param wildcardPattern a regex that defines the set of wildcards
     * @return matchable PTE
     * @throws NotMatchableException if the given expression cannot be matched
     */
    public static MatchablePomTaggedExpression compile(
            MLPWrapper mlp,
            PomTaggedExpression refRoot,
            @Language("RegExp") String wildcardPattern
    ) throws NotMatchableException {
        return compile(new MatchablePomTaggedExpressionConfig(mlp, wildcardPattern), refRoot);
    }

    /**
     * Compiles a matchable expression by providing the entire config object
     * @param config the configuration
     * @param expression the expression that is the pattern
     * @return a matchable version of the expression
     * @throws ParseException if the expression cannot be generated
     * @throws NotMatchableException if the expression is non-matchable
     */
    public static MatchablePomTaggedExpression compile(MatchablePomTaggedExpressionConfig config, String expression)
            throws ParseException, NotMatchableException {
        return compile(config, config.getMlpWrapper().parse(expression));
    }

    /**
     * Compiles a matchable expression by providing the entire config object
     * @param config the configuration
     * @param refRoot the expression that is the pattern
     * @return a matchable version of the expression
     * @throws NotMatchableException if the expression is non-matchable
     */
    public static MatchablePomTaggedExpression compile(MatchablePomTaggedExpressionConfig config, PomTaggedExpression refRoot)
            throws NotMatchableException{
        return new MatchablePomTaggedExpression(config, refRoot);
    }
}
