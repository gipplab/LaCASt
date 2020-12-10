package gov.nist.drmf.interpreter.pom.extensions;

import gov.nist.drmf.interpreter.common.interfaces.IMatcher;
import gov.nist.drmf.interpreter.pom.MLPWrapper;
import gov.nist.drmf.interpreter.pom.common.PomTaggedExpressionNormalizer;
import mlp.ParseException;
import mlp.PomTaggedExpression;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author Andre Greiner-Petter
 */
public abstract class AbstractMatchablePomTaggedExpression
        extends PomTaggedExpression
        implements IMatcher<PrintablePomTaggedExpression> {
    private static final Logger LOG = LogManager.getLogger(AbstractMatchablePomTaggedExpression.class.getName());

    /**
     * The instance that will be used to compile expressions;
     */
    private final MLPWrapper mlp;

    /**
     * The library is a shared object among all nodes of one tree.
     * This is necessary to check the integrity within matches.
     */
    private final GroupCaptures captures;

    /**
     * Essentially a copy of {@link PomTaggedExpression#getComponents()}
     */
    private final PomTaggedExpressionChildrenMatcher children;

    /**
     * The reference node from the actual {@link PomTaggedExpression} parse tree
     */
    private final PomTaggedExpression referenceNode;

    protected AbstractMatchablePomTaggedExpression(
            PomTaggedExpression refRoot,
            MLPWrapper mlp,
            GroupCaptures captures
    ) {
        super(refRoot.getRoot(), refRoot.getTag(), refRoot.getSecondaryTags());
        normalizeRoot(refRoot);

        this.mlp = mlp;
        this.captures = captures;
        this.children = new PomTaggedExpressionChildrenMatcher(this);
        this.referenceNode = refRoot;
    }

    private void normalizeRoot(PomTaggedExpression reference) {
        // if this the root, normalize the reference tree first
        if ( reference.getParent() == null )
            PomTaggedExpressionNormalizer.normalize(reference);
    }

    protected PomTaggedExpression getReferenceNode() {
        return referenceNode;
    }

    protected MLPWrapper getMLPWrapperInstance() {
        return mlp;
    }

    protected GroupCaptures getCaptures(){
        return captures;
    }

    protected PomTaggedExpressionChildrenMatcher getChildrenMatcher() {
        return children;
    }

    /**
     * Returns true if this node is an isolated wildcard, which means it does not have any siblings or no parent.
     * @return true if this is an isolated wildcard
     */
    public abstract boolean isIsolatedWildcard();

    /**
     * Generates a {@link PomMatcher} object from the given expression.
     * It uses the {@link MLPWrapper} given at initialization to generate
     * a parse tree.
     * @param expression the expression to match
     * @return a matcher object
     * @throws ParseException if the given expression cannot be parsed.
     */
    public PomMatcher matcher(String expression) throws ParseException {
        return matcher(mlp.parse(expression));
    }

    /**
     * Generates a {@link PomMatcher} object from the given expression.
     * It uses the {@link MLPWrapper} given at initialization to generate
     * a parse tree.
     * @param expression the expression to match
     * @return a matcher object
     * @throws ParseException if the given expression cannot be parsed.
     */
    public PomMatcher matcher(String expression, MatcherConfig config) throws ParseException {
        return matcher(mlp.parse(expression), config);
    }

    /**
     * Generates a {@link PomMatcher} object from the given expression.
     * This object can be used to safely search subtree matches and entire matches.
     * @param pte the expression to match
     * @return a matcher object
     */
    public PomMatcher matcher(PrintablePomTaggedExpression pte) {
        // normalize the expression first, otherwise are never able to match something
        PomTaggedExpressionNormalizer.normalize(pte);
        return new PomMatcher(this, pte);
    }

    /**
     * Generates a {@link PomMatcher} object from the given expression.
     * This object can be used to safely search subtree matches and entire matches.
     * @param pte the expression to match
     * @return a matcher object
     */
    public PomMatcher matcher(PrintablePomTaggedExpression pte, MatcherConfig config) {
        // normalize the expression first, otherwise are never able to match something
        PomTaggedExpressionNormalizer.normalize(pte);
        return new PomMatcher(this, pte, config);
    }

    /**
     * The default match is an exact match. If you want to allow prior and post non-matching tokens,
     * either use {@link #match(String, MatcherConfig)} or {@link #match(PrintablePomTaggedExpression)}.
     * @param expression latex string
     * @return true if the input matches the tree
     */
    public boolean match(String expression) {
        return match(expression, MatcherConfig.getExactMatchConfig());
    }

    /**
     * Generates a parse tree of the given input and returns true if the input matches this
     * pattern tree.
     * @param expression latex string
     * @param config configuration for the matcher
     * @return true if the input matches this tree
     */
    public boolean match(String expression, MatcherConfig config) {
        try {
            PrintablePomTaggedExpression ppte = mlp.parse(expression);
            return match(ppte, config);
        } catch (ParseException e) {
            LOG.warn("Cannot parse the given expression " + expression);
            return false;
        }
    }

    @Override
    public boolean match(PrintablePomTaggedExpression expression) {
        try {
            return match(expression, MatcherConfig.getExactMatchConfig());
        } catch (Exception e) {
            LOG.warn("Unable to match expression because " + e.getMessage());
            return false;
        }
    }

    /**
     * Allows to specify a config for the matcher
     * @param expression the expression to match
     * @param config the matcher configuration
     * @return true if it matches
     */
    public boolean match(PrintablePomTaggedExpression expression, MatcherConfig config) {
        try {
            return matchUnsafe(expression, config);
        } catch (Exception e) {
            LOG.warn(
                    String.format("Unable to match \"%s\". Exception: %s",
                            expression.getTexString(),
                            e.toString()
                    )
            );
            captures.clear();
            return false;
        }
    }

    /**
     * Throws exception if something went wrong. The other methods are safe and simply
     * return false if anything went wrong.
     * @param expression the expression to match
     * @param config the matching configuration
     * @return true if matches, otherwise false. may throw errors
     */
    public boolean matchUnsafe(PrintablePomTaggedExpression expression, MatcherConfig config) {
        captures.clear();
        expression = (PrintablePomTaggedExpression) PomTaggedExpressionNormalizer.normalize(expression);
        boolean matched;
        if ( config.allowLeadingTokens() ) {
            PomMatcher pomMatcher = new PomMatcher(this, expression, config);
            boolean result = pomMatcher.find();
            matched = result && (pomMatcher.lastMatchReachedEnd() || config.allowFollowingTokens());
        } else {
            matched = match(expression, new LinkedList<>(), config);
        }
        // in case we did not match, we should not provide any partial captured groups
        // the reason is, some may ask for the captured groups even though it didn't hit
        // but we cannot guarantee partial hit groups hence its better to reset all in such
        // cases to not provoke any errors in the future for other developers
        if ( !matched ) captures.clear();
        return matched;
    }

    /**
     * For public access, use the other public match functions.
     * This match is the main matcher method. It matches the given
     * {@param expression} with the following siblings {@param followingExpressions}.
     * Initially, {@param followingExpressions} is an empty list (the root of an
     * {@link PomTaggedExpression} has no siblings).
     *
     * Note that all nodes ({@param expression} and {@param followingExpressions}) must
     * share the same parent node. Otherwise it will create unknown artifacts and errors.
     *
     * @param expression the expression
     * @param followingExpressions the siblings of the expression
     * @param config the matcher config
     * @return true if it matched or false otherwise
     */
    abstract boolean match(
            PrintablePomTaggedExpression expression,
            List<PrintablePomTaggedExpression> followingExpressions,
            MatcherConfig config
    );

    /**
     * Get the grouped matches if any.
     * @return map of grouped matches
     * @see #getMatches()
     */
    public Map<String, String> getStringMatches() {
        return captures.getCapturedGroupStrings();
    }

    /**
     * Get the grouped matches as the {@link PomTaggedExpression} that matched the wildcards.
     * Since every wildcard may match sequences of nodes, the returned mapping maps the key
     * of the pattern group to the list of hits.
     * @return map of grouped matches
     * @see #getStringMatches()
     */
    public Map<String, List<PrintablePomTaggedExpression>> getMatches() {
        return captures.getCapturedGroups();
    }
}
