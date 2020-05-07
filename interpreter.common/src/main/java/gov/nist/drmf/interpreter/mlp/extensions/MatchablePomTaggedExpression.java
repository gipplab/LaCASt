package gov.nist.drmf.interpreter.mlp.extensions;

import gov.nist.drmf.interpreter.common.exceptions.NotMatchableException;
import gov.nist.drmf.interpreter.common.grammar.Brackets;
import gov.nist.drmf.interpreter.common.grammar.ExpressionTags;
import gov.nist.drmf.interpreter.common.interfaces.IMatcher;
import gov.nist.drmf.interpreter.mlp.MLPWrapper;
import gov.nist.drmf.interpreter.mlp.SemanticMLPWrapper;
import mlp.MathTerm;
import mlp.ParseException;
import mlp.PomTaggedExpression;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * This object is essentially an extension of the classic PomTaggedExpression.
 * However, it is matchable. That means, this object is tree-like class that supports
 * wildcards. Another {@link PomTaggedExpression} may matches this object or may not match it,
 * depending on the wildcards.
 *
 * @author Andre Greiner-Petter
 */
public class MatchablePomTaggedExpression extends PomTaggedExpression implements IMatcher<PrintablePomTaggedExpression> {
    private static final Logger LOG = LogManager.getLogger(MatchablePomTaggedExpression.class.getName());

    /**
     * If this node is a wildcard or not
     */
    private final boolean isWildcard;

    /**
     * The wildcard ID
     */
    private final String wildcardID;

    /**
     * Essentially a copy of {@link PomTaggedExpression#getComponents()}
     */
    private final PomTaggedExpressionChildrenMatcher children;

    /**
     * Next sibling, if any
     */
    private MatchablePomTaggedExpression nextSibling;

    /**
     * The library is a shared object among all nodes of one tree.
     * This is necessary to check the integrity within matches.
     */
    private final GroupCaptures captures;

    /**
     * For better performance, it is recommended to have one MLPWrapper object.
     * So if not necessary,
     * @param mlp the mlp wrapper to parse the expression
     * @param expression the expression to create a matchable tree
     * @param wildcardPattern the regex to find wildcards (e.g., var\d+).
     * @throws ParseException if the {@link MLPWrapper} is unable to parse the expression
     * @throws NotMatchableException if the given expression cannot be matched
     */
    public MatchablePomTaggedExpression(MLPWrapper mlp, String expression, String wildcardPattern)
            throws ParseException, NotMatchableException {
        this(mlp.simpleParse(expression), wildcardPattern);
    }

    /**
     * It uses the standard instance the parser via {@link SemanticMLPWrapper#getStandardInstance()}.
     * @param expression the expression to create a matchable tree
     * @param wildcardPattern the regex to find wildcards (e.g., var\d+).
     * @throws ParseException if the {@link MLPWrapper} is unable to parse the expression
     * @throws NotMatchableException if the given expression cannot be matched
     */
    public MatchablePomTaggedExpression(String expression, String wildcardPattern)
            throws ParseException, NotMatchableException {
        this(SemanticMLPWrapper.getStandardInstance().simpleParse(expression), wildcardPattern);
    }

    /**
     * Copy constructor to extend a {@link PomTaggedExpression} and its components to
     * a {@link MatchablePomTaggedExpression}.
     * @param refRoot         the reference {@link PomTaggedExpression}
     * @param wildcardPattern a regex that defines the set of wildcards
     * @throws NotMatchableException if the given expression cannot be matched
     */
    public MatchablePomTaggedExpression(PomTaggedExpression refRoot, String wildcardPattern)
            throws NotMatchableException {
        this(refRoot, wildcardPattern, new GroupCaptures());
    }

    private MatchablePomTaggedExpression(
            PomTaggedExpression refRoot,
            String wildcardPattern,
            GroupCaptures captures
    ) throws NotMatchableException {
        super(refRoot.getRoot(), refRoot.getTag(), refRoot.getSecondaryTags());
        this.captures = captures;
        this.children = new PomTaggedExpressionChildrenMatcher(this, captures);

        // if this the root, normalize the reference tree first
        if ( refRoot.getParent() != null )
            MLPWrapper.normalize(refRoot);

        Map<String, String> refFeatures = refRoot.getNamedFeatures();
        for (String k : refFeatures.keySet())
            super.setNamedFeature(k, refFeatures.get(k));

        String text = refRoot.getRoot().getTermText();

        if (text.matches(wildcardPattern)) {
            if (!refRoot.getComponents().isEmpty())
                throw new NotMatchableException("A wildcard node cannot have children.");
            this.isWildcard = true;
            this.wildcardID = text;
        } else {
            this.isWildcard = false;
            this.wildcardID = null;
        }

        List<PomTaggedExpression> comps = refRoot.getComponents();

        MatchablePomTaggedExpression prevNode = null;
        for (PomTaggedExpression pte : comps) {
            MatchablePomTaggedExpression cpte = new MatchablePomTaggedExpression(pte, wildcardPattern, captures);
            this.children.add(cpte);

            if (prevNode != null) {
                prevNode.setNextSibling(cpte);
                if (prevNode.isWildcard && cpte.isWildcard)
                    throw new NotMatchableException("Two consecutive wildcards may have no unique matches.");
            }
            prevNode = cpte;
        }
    }

    /**
     * @return true if this node is a wildcard.
     */
    public boolean isWildcard() {
        return isWildcard;
    }

    /**
     * @return the wildcard id (is null if this node is not a wildcard).
     */
    public String getWildcardID() {
        return wildcardID;
    }

    /**
     * Sets the next sibling
     * @param nextSibling next sibling
     */
    private void setNextSibling(MatchablePomTaggedExpression nextSibling) {
        this.nextSibling = nextSibling;
    }

    /**
     * The default match is an exact match. If you want to allow prior and post non-matching tokens,
     * either use {@link #match(String, MatcherConfig)} or {@link #match(PrintablePomTaggedExpression)}.
     * @param expression latex string
     * @return true if the input matches the tree
     */
    public boolean match(String expression) {
        return match(expression, MatcherConfig.getStrictConfig());
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
            PrintablePomTaggedExpression ppte = SemanticMLPWrapper.getStandardInstance().parse(expression);
            return match(ppte, config);
        } catch (ParseException e) {
            LOG.warn("Cannot parse the given expression " + expression);
            return false;
        }
    }

    @Override
    public boolean match(PrintablePomTaggedExpression expression) {
        try {
            return match(expression, MatcherConfig.getStrictConfig());
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
            if ( config.allowFollowingTokens() ) {
                return matchWithinPlace(expression, config);
            } else {
                captures.clear();
                expression = (PrintablePomTaggedExpression)MLPWrapper.normalize(expression);
                return match(expression, new LinkedList<>(), config);
            }
        } catch (Exception e) {
            LOG.warn("Unable to match expression because " + e.getMessage());
            return false;
        }
    }

    /**
     * Allows to match an expression in between other expressions. It allows non matching tokens
     * preceding and following the actual pattern.
     * @param expression the expression to match
     * @return true if there is a hit somewhere within the given expression
     */
    private boolean matchWithinPlace(PrintablePomTaggedExpression expression, MatcherConfig config) {
        captures.clear();
        expression = (PrintablePomTaggedExpression)MLPWrapper.normalize(expression);
        if (ExpressionTags.sequence.equals(ExpressionTags.getTagByKey(this.getTag()))) {
            return children.sequenceInPlaceMatch(expression, config);
        }

        return match(expression, new LinkedList<>(), config);
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
    boolean match (
            PrintablePomTaggedExpression expression,
            List<PrintablePomTaggedExpression> followingExpressions,
            MatcherConfig config
    ) {
        try {
            // essentially there are two cases, either it is not a wildcard, that it must match directly the reference
            if (!isWildcard) return matchNonWildCard(expression, config);
            return matchWildCard(expression, followingExpressions, config);
        } catch ( NotMatchableException nme ) {
            LOG.debug("Expression not matchable because: " + nme.getMessage());
            return false;
        }
    }

    private boolean matchNonWildCard(
            PrintablePomTaggedExpression expression,
            MatcherConfig config
    ){
        MathTerm otherRoot = expression.getRoot();
        MathTerm thisRoot = getRoot();

        // TODO might be too strict
        if (!thisRoot.getTermText().equals(otherRoot.getTermText())) {
            return false;
        }

        // since both term matches, we have to check their children
        // if this object doesn't have children, we can straight check the match
        if (this.children.isEmpty()) return expression.getComponents().isEmpty();

        LinkedList<PrintablePomTaggedExpression> refComponents =
                new LinkedList<>(expression.getPrintableComponents());
        return children.matchNonWildCardChildren(refComponents, config);
    }

    private boolean matchWildCard(
            PrintablePomTaggedExpression expression,
            List<PrintablePomTaggedExpression> followingExpressions,
            MatcherConfig config
    ) {
        // or it is a wildcard, which means it can be essentially anything
        // note that a wildcard cannot have any children, which makes it easier

        // if there is no next element in the pattern, the entire rest matches this wildcard
        if (nextSibling == null) {
            captureUntilEnd(expression, followingExpressions);
            return true;
        }

        // otherwise, add elements, until the next element matches
        if (followingExpressions.isEmpty()) return false;
        return matchWildcardUntilEnd(expression, followingExpressions, config);
    }

    private void captureUntilEnd(
            PrintablePomTaggedExpression expression,
            List<PrintablePomTaggedExpression> followingExpressions
    ) {
        List<PomTaggedExpression> matches = new LinkedList<>();
        matches.add(expression);
        while (!followingExpressions.isEmpty())
            matches.add(followingExpressions.remove(0));
        captures.setCapturedGroup(wildcardID, matches);
    }

    private boolean matchWildcardUntilEnd(
            PrintablePomTaggedExpression expression,
            List<PrintablePomTaggedExpression> followingExpressions,
            MatcherConfig config
    ) {
        List<PomTaggedExpression> matches = new LinkedList<>();
        matches.add(expression);
        PrintablePomTaggedExpression next = followingExpressions.remove(0);
        LinkedList<Brackets> bracketStack = new LinkedList<>();

        // fill up wild card until the next hit
        while (continueMatching(bracketStack, next, followingExpressions, config)) {
            if (followingExpressions.isEmpty() || isNotAllowedTokenForWildcardMatch(next)) {
                return false;
            }

            matches.add(next);

            if ( !config.ignoreBracketLogic() ) updateBracketStack(bracketStack, next);

            next = followingExpressions.remove(0);
        }

        if ( next == null ) return false;

        // nextSibling has matched the next element in followingExpression... so put add back into the queue
        // and return true
        followingExpressions.add(0, next);
        captures.setCapturedGroup(wildcardID, matches);
        return true;
    }

    private boolean continueMatching(
            LinkedList<Brackets> bracketStack,
            PrintablePomTaggedExpression next,
            List<PrintablePomTaggedExpression> followingExpressions,
            MatcherConfig config
    ) {
        // by building rule, the very next sibling cannot be a wildcard as well, so just check if it hits
        if ( !config.ignoreBracketLogic() && !bracketStack.isEmpty() ) return true;
        else return !nextSibling.match(next, followingExpressions, config);
    }

    private void updateBracketStack(
            LinkedList<Brackets> bracketStack,
            PrintablePomTaggedExpression next
    ) throws NotMatchableException {
        Brackets br = Brackets.getBracket( next.getRoot().getTermText() );
        if ( br == null ) return;

        if ( br.opened ) {
            bracketStack.addLast(br);
        } else if ( !bracketStack.isEmpty() ) {
            checkLastBracketEncounter(bracketStack, br);
        } else {
            throw new NotMatchableException(
                    "Not matching parenthesis. Found " + br + " but non was opened before.");
        }
    }

    private void checkLastBracketEncounter(LinkedList<Brackets> bracketStack, Brackets br) {
        if ( bracketStack.getLast().isCounterPart(br) )
            bracketStack.removeLast();
        else throw new NotMatchableException("Not matching parenthesis. Found " + br +
                " but last opening was " + bracketStack.getLast());
    }

    private boolean isNotAllowedTokenForWildcardMatch(PomTaggedExpression pte) {
        String mathTerm = pte.getRoot().getTermText();
        return mathTerm != null && mathTerm.matches("[,;.]");
    }

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
    public Map<String, PrintablePomTaggedExpression> getMatches() {
        return captures.getCapturedGroups();
    }
}
