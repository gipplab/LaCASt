package gov.nist.drmf.interpreter.mlp.extensions;

import gov.nist.drmf.interpreter.common.exceptions.NotMatchableException;
import gov.nist.drmf.interpreter.common.grammar.Brackets;
import gov.nist.drmf.interpreter.mlp.MLPWrapper;
import gov.nist.drmf.interpreter.mlp.MathTermUtility;
import mlp.MathTerm;
import mlp.PomTaggedExpression;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.intellij.lang.annotations.Language;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * This object is essentially an extension of the classic PomTaggedExpression.
 * However, it is matchable. That means, this object is tree-like class that supports
 * wildcards. Another {@link PomTaggedExpression} may matches this object or may not match it,
 * depending on the wildcards.
 *
 * @see PomMatcherBuilder
 * @author Andre Greiner-Petter
 */
public class MatchablePomTaggedExpression extends AbstractMatchablePomTaggedExpression {
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
     * Next sibling, if any
     */
    private MatchablePomTaggedExpression nextSibling;

    /**
     * For better performance, it is recommended to have one MLPWrapper object.
     * So if not necessary,
     * @param mlp the mlp wrapper to parse the expression
     * @param refRoot the expression to create a matchable tree
     * @param wildcardPattern the regex to find wildcards (e.g., var\d+).
     * @throws NotMatchableException if the given expression cannot be matched
     * @see PomMatcherBuilder
     */
    public MatchablePomTaggedExpression(MLPWrapper mlp, PomTaggedExpression refRoot, @Language("RegExp") String wildcardPattern)
            throws NotMatchableException {
        this(mlp, refRoot, wildcardPattern, new GroupCaptures());
    }

    private MatchablePomTaggedExpression(
            MLPWrapper mlpWrapper,
            PomTaggedExpression refRoot,
            String wildcardPattern,
            GroupCaptures captures
    ) throws NotMatchableException {
        super(refRoot, mlpWrapper, captures);

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
            this.isSingleSequenceWildcard = PrintablePomTaggedExpressionUtility.isSingleElementInBrackets(refRoot);
        } else {
            this.isWildcard = false;
            this.wildcardID = null;
            this.isSingleSequenceWildcard = false;
        }

        List<PomTaggedExpression> comps = refRoot.getComponents();

        MatchablePomTaggedExpression prevNode = null;
        for (PomTaggedExpression pte : comps) {
            MatchablePomTaggedExpression cpte = new MatchablePomTaggedExpression(mlpWrapper, pte, wildcardPattern, captures);
            this.getChildrenMatcher().add(cpte);
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

    @Override
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
        if (this.getChildrenMatcher().isEmpty()) return expression.getComponents().isEmpty();

        LinkedList<PrintablePomTaggedExpression> refComponents =
                new LinkedList<>(expression.getPrintableComponents());
        return getChildrenMatcher().matchNonWildCardChildren(refComponents, config);
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

        getCaptures().setCapturedGroup(wildcardID, matches);
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
        getCaptures().setCapturedGroup(wildcardID, matches);
        return true;
    }

    /**
     * In case the config does not allow following tokens after the hit, we have to check here
     * not only if the very next sibling matches, but also (if so) if the post-next-sibling tokens
     * force to check for the next
     * @param bracketStack stack of opened brackets
     * @param next next element
     * @param followingExpressions list of siblings of {@param next}
     * @param config configuration
     * @return true if matching should be continued
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
}
