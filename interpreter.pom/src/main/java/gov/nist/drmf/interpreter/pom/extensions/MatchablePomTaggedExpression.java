package gov.nist.drmf.interpreter.pom.extensions;

import gov.nist.drmf.interpreter.common.exceptions.NotMatchableException;
import gov.nist.drmf.interpreter.pom.common.FakeMLPGenerator;
import gov.nist.drmf.interpreter.pom.common.MathTermUtility;
import gov.nist.drmf.interpreter.pom.common.PomTaggedExpressionUtility;
import gov.nist.drmf.interpreter.pom.common.grammar.Brackets;
import mlp.MathTerm;
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
     * Previous sibling, if any
     */
    private MatchablePomTaggedExpression previousSibling;

    /**
     * The number of font manipulations that are attached to this matchable node
     */
    private final List<String> fontManipulations;

    /**
     * The number of following siblings (next siblings until null)
     */
    private int numberOfFollowingSiblings = 0;

    /**
     * Keep Kryo happy for serialization
     */
    MatchablePomTaggedExpression() {
        this(
                new MatchablePomTaggedExpressionConfig(),
                FakeMLPGenerator.generateEmptyPPTE()
        );
    }

    /**
     * For better performance, it is recommended to have one MLPWrapper object.
     * So if not necessary,
     * @param config the config to build a matchable pom tagged expression
     * @param refRoot the expression to create a matchable tree
     * @throws NotMatchableException if the given expression cannot be matched
     * @see PomMatcherBuilder
     */
    public MatchablePomTaggedExpression(
            MatchablePomTaggedExpressionConfig config,
            PomTaggedExpression refRoot
    ) throws NotMatchableException {
        super(refRoot, config.getMlpWrapper(), config.getCaptures());

        Map<String, String> refFeatures = refRoot.getNamedFeatures();
        for (String k : refFeatures.keySet())
            super.setNamedFeature(k, refFeatures.get(k));

        this.fontManipulations = PomTaggedExpressionUtility.getFontManipulations(refRoot);
        String text = refRoot.getRoot().getTermText();

        String wildcardPattern = config.getWildcardPattern();
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
            MatchablePomTaggedExpression cpte = new MatchablePomTaggedExpression(config, pte);
            this.getChildrenMatcher().add(cpte);
            setupSibling(config, prevNode, cpte);
            prevNode = cpte;
        }
    }

    private void setupSibling(
            MatchablePomTaggedExpressionConfig config,
            MatchablePomTaggedExpression prevNode,
            MatchablePomTaggedExpression cpte
    ) {
        if ( prevNode == null ) return;

        // updateSibling
        prevNode.setNextSibling(cpte);
        cpte.setPreviousSibling(prevNode);
        cpte.increaseSiblingCounter();

        // in case there are two consecutive wild cards, we may throw an error or call the fallback (if configured)
        if (prevNode.isWildcard && cpte.isWildcard && !prevNode.isSingleSequenceWildcard && !cpte.isSingleSequenceWildcard) {
            if ( !config.fallbackConsecutiveWildcards() ) {
                throw new NotMatchableException("Two consecutive wildcards may have no unique matches.");
            } else {
                LOG.debug("[Non-Matchable-Fallback] Flip previous wildcard to a single sequence wildcard.");
                MathTerm mathTerm = prevNode.getRoot();
                String wildCard = mathTerm.getTermText();
                mathTerm.setTermText("{" + wildCard + "}");
                PomTaggedExpression referencePTE = prevNode.getReferenceNode();
                referencePTE.setRoot(mathTerm);
                prevNode.isSingleSequenceWildcard = true;
            }
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
     * Returns true if this node has font rules attached. A wildcard might have font rules attached,
     * in which case it might be treated differently.
     * @return true if there are font rules attached to this node, such as <code>\mathsf</code>, <code>\overline</code>
     * etc.
     */
    public boolean containsFontRules() {
        return !this.fontManipulations.isEmpty();
    }

    /**
     * Sets the next sibling
     * @param nextSibling next sibling
     */
    private void setNextSibling(MatchablePomTaggedExpression nextSibling) {
        this.nextSibling = nextSibling;
    }

    /**
     * Sets the previous sibling
     * @param previousSibling previous sibling
     */
    private void setPreviousSibling(MatchablePomTaggedExpression previousSibling) {
        this.previousSibling = previousSibling;
    }

    /**
     * Increases the number of following siblings by one
     */
    private void increaseSiblingCounter() {
        MatchablePomTaggedExpression ref = this.previousSibling;
        while ( ref != null ) {
            ref.numberOfFollowingSiblings++;
            ref = ref.previousSibling;
        }
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
            LOG.trace("Expression not matchable because: " + nme.getMessage() +
                    "; Element: " + expression.getTexString() + " in [" + expression.getRootTexString() + "].");
            return false;
        }
    }

    private boolean matchNonWildCard(
            PrintablePomTaggedExpression expression,
            List<PrintablePomTaggedExpression> followingExpressions,
            MatcherConfig config
    ){
        MathTerm otherRoot = expression.getRoot();
        while (config.ignoreNumberOfAts() && MathTermUtility.isAt(otherRoot)) {
            if ( followingExpressions.isEmpty() ) {
                return true;
            }
            expression = followingExpressions.remove(0);
            otherRoot = expression.getRoot();
        }



        String otherString = PomTaggedExpressionUtility.getAppropriateFontTex(expression);
        String thisString = PomTaggedExpressionUtility.getAppropriateFontTex(this);
        // TODO we might want to loose this test based on config (maybe ignore feature set, font manipulation, etc).
        if (!thisString.equals(otherString)) {
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

        // however, a wildcard might contain font manipulations, in which case they must match!
        if ( !matchesAccents(expression, config) ) return false;

        // if there is no next element in the pattern, the entire rest matches this wildcard
        if (nextSibling == null) {
            return captureUntilEnd(expression, followingExpressions, config);
        }

        // otherwise, add elements, until the next element matches
        // if there are no following elements however, the match failed
        return !followingExpressions.isEmpty() && matchWildcardUntilEnd(expression, followingExpressions, config);

    }

    private boolean captureUntilEnd(
            PrintablePomTaggedExpression expression,
            List<PrintablePomTaggedExpression> followingExpressions,
            MatcherConfig config
    ) {
        if ( isNotAllowedTokenForWildcardMatch(expression, config) ) return false;

        List<PomTaggedExpression> matches = new LinkedList<>();
        PomTaggedExpressionUtility.removeFontManipulations(expression, fontManipulations);
        matches.add(expression);

        if ( !isSingleSequenceWildcard && fontManipulations.isEmpty() ) {
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
        PomTaggedExpressionUtility.removeFontManipulations(expression, fontManipulations);
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
        // if this node contains font manipulations, we must immediately stop here
        if ( !fontManipulations.isEmpty() ) return false;

        // by building rule, the very next sibling cannot be a wildcard as well, so just check if it hits
        if ( !config.ignoreBracketLogic() && !bracketStack.isEmpty() ) return true;

        // if the next match is a singleSequenceWildcard, we only continue matching (greedy) if the
        // the following expression has more than one element left (the next element, so followingExpressions must be empty)
        if ( nextSibling.isSingleSequenceWildcard ) {
            return this.numberOfFollowingSiblings < followingExpressions.size()+1;
        }

        boolean nextSiblingMatched = nextSibling.match(next, followingExpressions, config);
        if ( !config.allowFollowingTokens() && nextSiblingMatched ) {
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

    private boolean matchesAccents(PomTaggedExpression pte, MatcherConfig config) {
        List<String> otherFontManipulations = PomTaggedExpressionUtility.getFontManipulations(pte);

        if ( this.fontManipulations.size() > otherFontManipulations.size() ) return false;

        for ( int i = 0; i < this.fontManipulations.size() && i < otherFontManipulations.size(); i++ ) {
            String matchManipulation = this.fontManipulations.get(i);
            String otherManipulation = otherFontManipulations.get(i);
            if ( !matchManipulation.equals(otherManipulation) ) return false;
        }
        return true;
    }
}
