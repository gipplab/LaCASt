package gov.nist.drmf.interpreter.mlp.extensions;

import gov.nist.drmf.interpreter.common.TeXPreProcessor;
import gov.nist.drmf.interpreter.common.exceptions.NotMatchableException;
import gov.nist.drmf.interpreter.common.grammar.Brackets;
import gov.nist.drmf.interpreter.common.grammar.ExpressionTags;
import gov.nist.drmf.interpreter.common.interfaces.IMatcher;
import gov.nist.drmf.interpreter.mlp.MLPWrapper;
import gov.nist.drmf.interpreter.mlp.MathTermUtility;
import gov.nist.drmf.interpreter.mlp.PomTaggedExpressionUtility;
import gov.nist.drmf.interpreter.mlp.SemanticMLPWrapper;
import mlp.MathTerm;
import mlp.ParseException;
import mlp.PomTaggedExpression;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.intellij.lang.annotations.Language;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

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
     * If a single node a is wrapped in brackets {a}, it represents a single sequence object.
     * This kind of wildcard only matches a single node even if it is at the end of a sequence.
     */
    private boolean isSingleSequenceWildcard;

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
     * The instance that will be used to compile expressions;
     */
    private final MLPWrapper mlp;

    /**
     * This expression does not contain wildcards! If you want to use wildcards, use one of the
     * other constructors.
     * @param expression the expression to match without wildcards.
     */
    public MatchablePomTaggedExpression(String expression) throws ParseException {
        this(expression, "");
    }

    /**
     * This expression does not contain wildcards! If you want to use wildcards, use one of the
     * other constructors.
     * @param refRoot the expression to match without wildcards.
     */
    public MatchablePomTaggedExpression(PomTaggedExpression refRoot) {
        this(refRoot, "");
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
        this(SemanticMLPWrapper.getStandardInstance(), expression, wildcardPattern);
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
        this(SemanticMLPWrapper.getStandardInstance(), refRoot, wildcardPattern, new GroupCaptures());
    }

    /**
     * For better performance, it is recommended to have one MLPWrapper instance.
     * Hence, use one of the constructors to provide the instance you are using here.
     *
     * This constructor does not allow any wildcards.
     *
     * @param mlp the mlp wrapper to parse the expression
     * @param expression the expression to create a matchable tree
     * @throws ParseException if the {@link MLPWrapper} is unable to parse the expression
     * @throws NotMatchableException if the given expression cannot be matched
     */
    public MatchablePomTaggedExpression(MLPWrapper mlp, String expression)
            throws ParseException, NotMatchableException {
        this(mlp, mlp.parse(expression), "", new GroupCaptures());
    }

    /**
     * For better performance, it is recommended to have one MLPWrapper object.
     * So if not necessary,
     * @param mlp the mlp wrapper to parse the expression
     * @param expression the expression to create a matchable tree
     * @param wildcardPattern the regex to find wildcards (e.g., var\d+).
     * @throws ParseException if the {@link MLPWrapper} is unable to parse the expression
     * @throws NotMatchableException if the given expression cannot be matched
     */
    public MatchablePomTaggedExpression(MLPWrapper mlp, String expression, @Language("RegExp") String wildcardPattern)
            throws ParseException, NotMatchableException {
        this(mlp, mlp.parse(expression), wildcardPattern, new GroupCaptures());
    }

    /**
     * Copy constructor to extend a {@link PomTaggedExpression} and its components to
     * a {@link MatchablePomTaggedExpression}.
     * @param mlp             the MLP engine to use for matching expressions
     * @param refRoot         the reference {@link PomTaggedExpression}
     * @param wildcardPattern a regex that defines the set of wildcards
     * @throws NotMatchableException if the given expression cannot be matched
     */
    public MatchablePomTaggedExpression(MLPWrapper mlp, PomTaggedExpression refRoot, String wildcardPattern)
            throws NotMatchableException {
        this(mlp, refRoot, wildcardPattern, new GroupCaptures());
    }

    private MatchablePomTaggedExpression(
            MLPWrapper mlpWrapper,
            PomTaggedExpression refRoot,
            String wildcardPattern,
            GroupCaptures captures
    ) throws NotMatchableException {
        super(refRoot.getRoot(), refRoot.getTag(), refRoot.getSecondaryTags());
        this.mlp = mlpWrapper;
        this.captures = captures;
        this.children = new PomTaggedExpressionChildrenMatcher(this);

        // if this the root, normalize the reference tree first
        if ( refRoot.getParent() != null )
            MLPWrapper.normalize(refRoot);

        Map<String, String> refFeatures = refRoot.getNamedFeatures();
        for (String k : refFeatures.keySet())
            super.setNamedFeature(k, refFeatures.get(k));

        String text = refRoot.getRoot().getTermText();

        if (!wildcardPattern.isBlank() && text.matches(wildcardPattern)) {
            if (!refRoot.getComponents().isEmpty())
                throw new NotMatchableException("A wildcard node cannot have children.");
            this.isWildcard = true;
            this.wildcardID = text;
            this.isSingleSequenceWildcard = PrintablePomTaggedExpressionUtils.isSingleElementInBrackets(refRoot);
        } else {
            this.isWildcard = false;
            this.wildcardID = null;
            this.isSingleSequenceWildcard = false;
        }

        List<PomTaggedExpression> comps = refRoot.getComponents();

        MatchablePomTaggedExpression prevNode = null;
        for (PomTaggedExpression pte : comps) {
            MatchablePomTaggedExpression cpte = new MatchablePomTaggedExpression(mlp, pte, wildcardPattern, captures);
            this.children.add(cpte);
            checkValidity(prevNode, cpte);
            prevNode = cpte;
        }
    }

    private void checkValidity(MatchablePomTaggedExpression prevNode, MatchablePomTaggedExpression cpte) {
        if (prevNode != null) {
            prevNode.setNextSibling(cpte);
            if (prevNode.isWildcard && cpte.isWildcard && !prevNode.isSingleSequenceWildcard)
                throw new NotMatchableException("Two consecutive wildcards may have no unique matches.");
        }
    }

    MLPWrapper getMLPWrapperInstance() {
        return mlp;
    }

    PomTaggedExpressionChildrenMatcher getChildrenMatcher() {
        return children;
    }

    GroupCaptures getCaptures(){
        return captures;
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
        MLPWrapper.normalize(pte);
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
        MLPWrapper.normalize(pte);
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
     * @param expression
     * @param config
     * @return
     */
    public boolean matchUnsafe(PrintablePomTaggedExpression expression, MatcherConfig config) {
        captures.clear();
        expression = (PrintablePomTaggedExpression)MLPWrapper.normalize(expression);
        boolean matched = false;
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
    boolean match (
            PrintablePomTaggedExpression expression,
            List<PrintablePomTaggedExpression> followingExpressions,
            MatcherConfig config
    ) {
        try {
            // essentially there are two cases, either it is not a wildcard, that it must match directly the reference
            if (!isWildcard) return matchNonWildCard(expression, followingExpressions, config);
            return matchWildCard(expression, followingExpressions, config);
        } catch ( NotMatchableException nme ) {
            LOG.debug("Expression not matchable because: " + nme.getMessage() +
                    "; Expression: " + expression.getTexString());
            return false;
        }
    }

    private boolean matchNonWildCard(
            PrintablePomTaggedExpression expression,
            List<PrintablePomTaggedExpression> followingExpressions,
            MatcherConfig config
    ){
        MathTerm otherRoot = expression.getRoot();
        MathTerm thisRoot = getRoot();

        while (config.ignoreNumberOfAts() && MathTermUtility.isAt(otherRoot)) {
            if ( followingExpressions.isEmpty() ) {
                return true;
            }
            expression = followingExpressions.remove(0);
            otherRoot = expression.getRoot();
        }

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
            return captureUntilEnd(expression, followingExpressions, config);
        }

        // otherwise, add elements, until the next element matches
        if (followingExpressions.isEmpty()) return false;
        return matchWildcardUntilEnd(expression, followingExpressions, config);
    }

    private boolean captureUntilEnd(
            PrintablePomTaggedExpression expression,
            List<PrintablePomTaggedExpression> followingExpressions,
            MatcherConfig config
    ) {
        if ( isNotAllowedTokenForWildcardMatch(expression, config) ) return false;

        List<PomTaggedExpression> matches = new LinkedList<>();
        matches.add(expression);

        if ( !isSingleSequenceWildcard ) {
            while (!followingExpressions.isEmpty()){
                expression = followingExpressions.remove(0);
                if ( isNotAllowedTokenForWildcardMatch(expression, config) ) return false;
                matches.add(expression);
            }
        }

        captures.setCapturedGroup(wildcardID, matches);
        return true;
    }

    private boolean matchWildcardUntilEnd(
            PrintablePomTaggedExpression expression,
            List<PrintablePomTaggedExpression> followingExpressions,
            MatcherConfig config
    ) {
        if ( isNotAllowedTokenForWildcardMatch(expression, config) ) return false;

        List<PomTaggedExpression> matches = new LinkedList<>();
        matches.add(expression);
        PrintablePomTaggedExpression next = followingExpressions.remove(0);
        LinkedList<Brackets> bracketStack = new LinkedList<>();

        // fill up wild card until the next hit
        while (!isSingleSequenceWildcard && continueMatching(bracketStack, next, followingExpressions, config)) {
            if (followingExpressions.isEmpty() || isNotAllowedTokenForWildcardMatch(next, config)) {
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

    /**
     * In case the config does not allow following tokens after the hit, we have to check here
     * not only if the very next sibling matches, but also (if so) if the post-next-sibling tokens
     * force to check for the next
     * @param bracketStack
     * @param next
     * @param followingExpressions
     * @param config
     * @return
     */
    private boolean continueMatching(
            LinkedList<Brackets> bracketStack,
            PrintablePomTaggedExpression next,
            List<PrintablePomTaggedExpression> followingExpressions,
            MatcherConfig config
    ) {
        // by building rule, the very next sibling cannot be a wildcard as well, so just check if it hits
        if ( !config.ignoreBracketLogic() && !bracketStack.isEmpty() ) return true;

        boolean nextSiblingMatched = nextSibling.match(next, followingExpressions, config);
        if ( !config.allowFollowingTokens() && nextSiblingMatched ) {
            // TODO we may need to continue... but how do we find out?

            // ok in case the next sibling is the end of the pattern, than the following
            // expressions must be empty to stop continue matching
            if ( nextSibling.nextSibling == null ) return !followingExpressions.isEmpty();

            // otherwise, and now it gets complicated... we must continue matching ALL siblings
            // to see if it works in the end
            LinkedList<PrintablePomTaggedExpression> copyFollowingExpressions = new LinkedList<>(followingExpressions);
            next = copyFollowingExpressions.removeFirst();
            MatchablePomTaggedExpression nextSiblingReference = nextSibling.nextSibling;
            while ( nextSiblingReference.match(next, copyFollowingExpressions, config) ) {
                if ( copyFollowingExpressions.isEmpty() ) return false;
                next = copyFollowingExpressions.removeFirst();
                nextSiblingReference = nextSiblingReference.nextSibling;
                if ( nextSiblingReference == null ) return true;
            }
            return true;
        }
        // the opposite, because we asking if we shall continue matching wildcards. If the next sibling
        // matched, we reached the end. Hence the method return false to stop continue matching a wild card
        return !nextSiblingMatched;
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

    private boolean isNotAllowedTokenForWildcardMatch(PomTaggedExpression pte, MatcherConfig config) {
        String mathTerm = pte.getRoot().getTermText();
        String pattern = config.getIllegalTokensForWildcard(this.wildcardID);
        return mathTerm != null && mathTerm.matches(pattern);
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
