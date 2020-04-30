package gov.nist.drmf.interpreter.mlp.extensions;

import gov.nist.drmf.interpreter.common.exceptions.NotMatchableException;
import gov.nist.drmf.interpreter.common.grammar.Brackets;
import gov.nist.drmf.interpreter.common.grammar.ExpressionTags;
import gov.nist.drmf.interpreter.common.interfaces.IMatcher;
import gov.nist.drmf.interpreter.mlp.MLPWrapper;
import mlp.MathTerm;
import mlp.ParseException;
import mlp.PomTaggedExpression;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.regex.Matcher;

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
    private final List<MatchablePomTaggedExpression> components;

    /**
     * Next sibling, if any
     */
    private MatchablePomTaggedExpression nextSibling;

    /**
     * The library is a shared object among all nodes of one tree.
     * This is necessary to check the integrity within matches.
     */
    private final Map<String, PrintablePomTaggedExpression> matchLibrary;

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
     * It uses the standard instance the parser via {@link MLPWrapper#getStandardInstance()}.
     * @param expression the expression to create a matchable tree
     * @param wildcardPattern the regex to find wildcards (e.g., var\d+).
     * @throws ParseException if the {@link MLPWrapper} is unable to parse the expression
     * @throws NotMatchableException if the given expression cannot be matched
     */
    public MatchablePomTaggedExpression(String expression, String wildcardPattern)
            throws ParseException, NotMatchableException {
        this(MLPWrapper.getStandardInstance().simpleParse(expression), wildcardPattern);
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
        this(refRoot, wildcardPattern, new HashMap<>());
    }

    private MatchablePomTaggedExpression(
            PomTaggedExpression refRoot,
            String wildcardPattern,
            Map<String, PrintablePomTaggedExpression> matchLibrary
    ) throws NotMatchableException {
        super(refRoot.getRoot(), refRoot.getTag(), refRoot.getSecondaryTags());
        this.matchLibrary = matchLibrary;

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
        this.components = new LinkedList<>();

        MatchablePomTaggedExpression prevNode = null;
        for (PomTaggedExpression pte : comps) {
            MatchablePomTaggedExpression cpte = new MatchablePomTaggedExpression(pte, wildcardPattern, matchLibrary);
            super.addComponent(cpte);
            this.components.add(cpte);

            if (prevNode != null) {
                prevNode.setNextSibling(cpte);
                if (prevNode.isWildcard && cpte.isWildcard)
                    throw new NotMatchableException("Two consecutive wildcards may have no unique matches.");
            }
            prevNode = cpte;
        }
    }

    private void setNextSibling(MatchablePomTaggedExpression nextSibling) {
        this.nextSibling = nextSibling;
    }

    /**
     * Resets the previous matches, if any
     */
    private void depthResetMatches() {
        this.matchLibrary.clear();
    }

    /**
     * The default match is an exact match. If you want to allow prior and post non-matching tokens,
     * either use {@link #match(String, boolean)} or {@link #matchWithinPlace(PrintablePomTaggedExpression)}.
     * @param expression latex string
     * @return true if the input matches the tree
     */
    public boolean match(String expression) {
        return match(expression, true);
    }

    /**
     * Generates a parse tree of the given input and returns true if the input matches this
     * pattern tree.
     * @param expression latex string
     * @param exactMatch if true, the matcher does not allow tokens prior or after the match.
     * @return true if the input matches this tree
     */
    public boolean match(String expression, boolean exactMatch) {
        try {
            PrintablePomTaggedExpression ppte = MLPWrapper.getStandardInstance().parse(expression);
            if ( exactMatch )
                return match(ppte);
            else return matchWithinPlace(ppte);
        } catch (ParseException e) {
            LOG.warn("Cannot parse the given expression " + expression);
            return false;
        }
    }

    @Override
    public boolean match(PrintablePomTaggedExpression expression) {
        depthResetMatches();
        expression = (PrintablePomTaggedExpression)MLPWrapper.normalize(expression);
        return match(expression, new LinkedList<>(), MatcherConfig.getStrictConfig());
    }

    /**
     * Allows to match an expression in between other expressions. It allows non matching tokens
     * preceding and following the actual pattern.
     * @param expression the expression to match
     * @return true if there is a hit somewhere within the given expression
     */
    public boolean matchWithinPlace(PrintablePomTaggedExpression expression) {
        depthResetMatches();
        expression = (PrintablePomTaggedExpression)MLPWrapper.normalize(expression);
        MatcherConfig config = new MatcherConfig(true, false);

        if (ExpressionTags.sequence.equals(ExpressionTags.getTagByKey(this.getTag()))) {
            return sequenceInPlaceMatch(expression, config);
        }

        return match(expression, new LinkedList<>(), config);
    }

    private boolean sequenceInPlaceMatch(PrintablePomTaggedExpression expression, MatcherConfig config) {
        List<PrintablePomTaggedExpression> children = expression.getPrintableComponents();
        LinkedList<PrintablePomTaggedExpression> backup = new LinkedList<>();
        MatchablePomTaggedExpression matchBackUp = null;

        // until we hit non-wildcard element
        while ( components.get(0).isWildcard ) {
            matchBackUp = components.remove(0);
            getComponents().remove(0);
        }

        // start matching from here
        boolean findMatch = findNextMatch(expression, children, backup, config);
        if ( !findMatch ) return false;

        // rollback wildcards, but only take one hit
        rollbackSkippedWildcards(expression, backup, matchBackUp);

        return true;
    }

    private boolean findNextMatch(
            PrintablePomTaggedExpression expression,
            List<PrintablePomTaggedExpression> children,
            LinkedList<PrintablePomTaggedExpression> backup,
            MatcherConfig config
    ) {
        boolean currentMatch = match(expression, new LinkedList<>(), config);
        while ( !currentMatch ) {
            if ( children.size() <= 1 ) return false;
            this.depthResetMatches();

            PrintablePomTaggedExpression firstElement = children.remove(0);
            expression.getComponents().remove(0);
            backup.add(firstElement);

            currentMatch = match(expression, new LinkedList<>(), config);
        }

        return true;
    }

    private void rollbackSkippedWildcards(
            PrintablePomTaggedExpression expression,
            LinkedList<PrintablePomTaggedExpression> backup,
            MatchablePomTaggedExpression matchBackUp
    ) {
        if (matchBackUp != null) {
            PrintablePomTaggedExpression backupElement = backup.removeLast();
            matchBackUp.setMatch( backupElement );
            this.components.add(0, matchBackUp);
            addComponent(0, matchBackUp);
            expression.addComponent(0, backupElement);
        }

        while ( !backup.isEmpty() ) {
            expression.addComponent(0, backup.removeLast());
        }
    }

    private boolean match(
            PrintablePomTaggedExpression expression,
            List<PrintablePomTaggedExpression> followingExpressions,
            MatcherConfig config
    ) {
        try {
            // essentially there are two cases, either it is not a wildcard, that it must match directly the reference
            if (!isWildcard) return matchNonWildCard(expression, config);

            /*
             * it may happen that the previous check for the end of a wildcard accidentally matched
             * deeper wildcards within the subtree of the current (this) node. If so, this wildcard
             * (note that it is non-static) is not empty. This only happens if the previous check was
             * already successful. Hence, we can simply skip this note instead of clearing the matched
             * group and try to match it again.
             */
//            if ( !this.wildcardMatch.isEmpty() ) return true;
            return matchWildCard(expression, followingExpressions, config);
        } catch ( NotMatchableException nme ) {
            LOG.debug("Expression not matchable because: " + nme.getMessage());
            return false;
        }
    }

    private boolean matchNonWildCard(PrintablePomTaggedExpression expression, MatcherConfig config) {
        MathTerm otherRoot = expression.getRoot();
        MathTerm thisRoot = getRoot();

        // TODO might be too strict
        if (!thisRoot.getTermText().equals(otherRoot.getTermText())) {
            return false;
        }

        // since both term matches, we have to check their children
        // if this object doesn't have children, we can straight check the match
        if (this.components.isEmpty()) return expression.getComponents().isEmpty();

        LinkedList<PrintablePomTaggedExpression> refComponents = new LinkedList<>(expression.getPrintableComponents());

        return matchNonWildCardChildren(refComponents, config);
    }

    private boolean matchNonWildCardChildren(
            LinkedList<PrintablePomTaggedExpression> refComponents,
            MatcherConfig config
    ) {
        int idx = 0;
        while (idx < components.size() && !refComponents.isEmpty()) {
            PrintablePomTaggedExpression firstRef = refComponents.removeFirst();
            MatchablePomTaggedExpression matcherElement = components.get(idx);

            if (!matcherElement.match(firstRef, refComponents, config)) return false;

            idx++;
        }
        return (config.allowFollowingTokens() && !refComponents.isEmpty()) || idx == components.size();
    }

    private boolean matchWildCard(
            PrintablePomTaggedExpression expression,
            List<PrintablePomTaggedExpression> followingExpressions,
            MatcherConfig config
    ) {
        // or it is a wildcard, which means it can be essentially anything
        // note that a wildcard cannot have any children, which makes it easier
        List<PomTaggedExpression> matches = new LinkedList<>();

        // if there is no next element in the pattern, the entire rest matches this wildcard
        if (nextSibling == null) {
            matches.add(expression);
            while (!followingExpressions.isEmpty())
                matches.add(followingExpressions.remove(0));
            setMatch(matches);
            return true;
        }

        // otherwise, add elements, until the next element matches
        if (followingExpressions.isEmpty()) return false;

        matches.add(expression);
        PrintablePomTaggedExpression next = followingExpressions.remove(0);

        LinkedList<Brackets> bracketStack = new LinkedList<>();

        // fill up wild card until the next hit
        next = fillWildCardMatch(matches, bracketStack, next, followingExpressions, config);
        if ( next == null ) return false;

        // nextSibling has matched the next element in followingExpression... so put add back into the queue
        // and return true
        followingExpressions.add(0, next);
        setMatch(matches);
        return true;
    }

    private PrintablePomTaggedExpression fillWildCardMatch(
            List<PomTaggedExpression> matches,
            LinkedList<Brackets> bracketStack,
            PrintablePomTaggedExpression next,
            List<PrintablePomTaggedExpression> followingExpressions,
            MatcherConfig config
    ) {
        while (continueMatching(bracketStack, next, followingExpressions, config)) {
            if (followingExpressions.isEmpty() || isNotAllowedTokenForWildcardMatch(next))
                return null;

            matches.add(next);
            if ( !config.ignoreBracketLogic() ) updateBracketStack(bracketStack, next);

            next = followingExpressions.remove(0);
        }
        return next;
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

    private void setMatch(List<PomTaggedExpression> match) {
        PrintablePomTaggedExpression m;
        if ( match.size() > 1 ) {
            m = FakeMLPGenerator.generateEmptySequencePPTE();
            m.setComponents(match);
        } else if ( match.size() == 1 ) m = (PrintablePomTaggedExpression) match.get(0);
        else throw new IllegalArgumentException("Cannot set the hit for an empty match.");
        setMatch(m);
    }

    private void setMatch(PrintablePomTaggedExpression match) {
        if ( matchLibrary.containsKey(wildcardID) ) {
            String prev = matchLibrary.get(wildcardID).getTexString();
            String matchS = match.getTexString();
            if ( prev.startsWith("{") && prev.endsWith("}") ) prev = prev.substring(1, prev.length()-1);
            if ( matchS.startsWith("{") && matchS.endsWith("}") ) matchS = matchS.substring(1, matchS.length()-1);

            if ( !prev.equals(matchS) ) {
                String msg = String.format("%s matches clashed in '%s' vs '%s'", wildcardID, prev, matchS);
                throw new NotMatchableException(msg);
            } else return;
        }
        matchLibrary.put(wildcardID, match);
    }

    /**
     * Get the grouped matches if any.
     * @return map of grouped matches
     * @see #getMatches()
     */
    public Map<String, String> getStringMatches() {
        Map<String, String> out = new HashMap<>();
        Map<String, PrintablePomTaggedExpression> matches = getMatches();

        for (String key : matches.keySet()) {
            String str = matches.get(key).getTexString();
            Matcher m = Brackets.PARENTHESES_PATTERN.matcher(str);
            if ( m.matches() ) str = m.group(1);
            out.put(key, str.trim());
        }

        return out;
    }

    /**
     * Get the grouped matches as the {@link PomTaggedExpression} that matched the wildcards.
     * Since every wildcard may match sequences of nodes, the returned mapping maps the key
     * of the pattern group to the list of hits.
     * @return map of grouped matches
     * @see #getStringMatches()
     */
    public Map<String, PrintablePomTaggedExpression> getMatches() {
        return matchLibrary;
    }
}
