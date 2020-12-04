package gov.nist.drmf.interpreter.pom.extensions;

import gov.nist.drmf.interpreter.pom.common.FakeMLPGenerator;
import gov.nist.drmf.interpreter.pom.common.PomTaggedExpressionUtility;
import mlp.ParseException;
import mlp.PomTaggedExpression;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

/**
 * @author Andre Greiner-Petter
 */
public class PomMatcher {
    private static final Logger LOG = LogManager.getLogger(PomMatcher.class.getName());

    private final MatcherConfig config;

    private final AbstractMatchablePomTaggedExpression matcher;
    private final MatchablePomTaggedExpression matcherFirstElement;
    private final PomTaggedExpressionChildrenMatcher children;
    private final PrintablePomTaggedExpression orig;
    private final GroupCaptures refGroups;

    private final boolean isSequenceMatcher;

    private PrintablePomTaggedExpression copy;

    private boolean inProcess = false;
    private boolean lastMatchWentUntilEnd = false;

    private MatchablePomTaggedExpression leadingBackUpWildcard;

    private final LinkedList<DepthExpressionsCache> remaining;
    private DepthExpressionsCache latestDepthExpression;

    // this is necessary to avoid double replacements
    private boolean wasReplaced = false;

    // this is just to check if any of the "replacement" methods actually found something or not
    // so the user can ask if there was a replacement applied or not
    private boolean hasReplacedAnything = false;

    private boolean latestHitMatchedWithoutPassingElements = false;
    private boolean firstRound = true;

    /**
     * Keep Kryo happy for serialization
     */
    private PomMatcher() {
        this(new MatchablePomTaggedExpression(), null, null);
    }

    PomMatcher(
            AbstractMatchablePomTaggedExpression mpte,
            PrintablePomTaggedExpression pte
    ) {
        this(mpte, pte, MatcherConfig.getInPlaceMatchConfig());
    }

    /**
     * To get an instance of this class, you should use {@link MatchablePomTaggedExpression#matcher(String)}
     * or {@link MatchablePomTaggedExpression#matcher(PrintablePomTaggedExpression)}.
     * @param mpte the underlying matchable parse tree
     * @param pte the parse tree to match
     */
    PomMatcher(
            AbstractMatchablePomTaggedExpression mpte,
            PrintablePomTaggedExpression pte,
            MatcherConfig config
    ) {
        this.config = config;
        this.matcher = mpte;
        this.isSequenceMatcher = PomTaggedExpressionUtility.isSequence(matcher);
        this.children = mpte.getChildrenMatcher();
        this.orig = pte;
        this.refGroups = mpte.getCaptures();
        this.leadingBackUpWildcard = null;
        this.remaining = new LinkedList<>();
        this.latestDepthExpression = null;

        updateLeadingWildcard();
        this.matcherFirstElement = isSequenceMatcher ? (MatchablePomTaggedExpression)matcher.getComponents().get(0) : null;
    }

    private void updateLeadingWildcard() {
        // in case it starts with a wildcard, its more efficient to handle that later. remove it and continue
        if ( children.isFirstChildWildcard() ) {
            leadingBackUpWildcard = children.hideFirstWildcard();
        }
    }

    /**
     * Exact matches the initialized expression. Returns true if it matched,
     * otherwise not. The captured groups can be accessed via {@link #groups()}.
     *
     * Calling this method between {@link #find()} resets the finding process!
     *
     * @return true if it exact matches
     */
    public boolean match() {
        reset();
        MatcherConfig internalConfig = new MatcherConfig(config);
        internalConfig.allowLeadingTokens(false);
        internalConfig.allowFollowingTokens(false);
        children.undoHiddenFirstWildcard();
        boolean res = matcher.match(copy, internalConfig);
        if ( res ) {
            lastMatchWentUntilEnd = true;
            latestHitMatchedWithoutPassingElements = true;
        }
        updateLeadingWildcard();
        return res;
    }

    /**
     * Allows to find the next match within the expression if any. It stops and
     * return true, if found a match. You can call the method again to find the
     * next match within the same expression. It follows the same pattern as
     * {@link java.util.regex.Matcher}. Via {@link #reset()}, you can reset the
     * finding sequence. Via {@link #groups()} you get the groups after a successful
     * hit. Note that {@link #match()} will reset the sequence also!
     *
     * Once it hits the end, it will always return false until one resets the
     * cache via {@link #reset()}.
     *
     * @return true if the matcher found a hit somewhere in the expression, otherwise
     * false.
     */
    public boolean find() {
        saveInProgressReset();
        wasReplaced = false;
        lastMatchWentUntilEnd = false;
        latestHitMatchedWithoutPassingElements = false;

        // if the config does not allow leading tokens that does not match, its a simple match method
        // and "find" does not make sense
        if ( !config.allowLeadingTokens() && !remaining.isEmpty() ) {
            latestDepthExpression = remaining.removeFirst();
            latestDepthExpression.currentReferenceNode = latestDepthExpression.remainingExpressions.removeFirst();
            return matcher.match(copy, config);
        }

        while ( !remaining.isEmpty() ) {
            // get the remaining list of children to work on
            latestDepthExpression = remaining.removeFirst();
            List<PrintablePomTaggedExpression> elements = latestDepthExpression.remainingExpressions;
            LinkedList<PrintablePomTaggedExpression> backlog = new LinkedList<>();

            if ( findNextMatch(elements, backlog) ) {
                storeLatestMatch(elements);
                latestHitMatchedWithoutPassingElements = backlog.isEmpty();
                return true;
            }
        }

        return false;
    }

    public boolean latestHitMatchedExact() {
        return firstRound
                && latestHitMatchedWithoutPassingElements
                && lastMatchWentUntilEnd
                && latestDepthExpression.currentDepth <= 1;
    }

    private void saveInProgressReset() {
        if ( !inProcess ) {
            reset();
            inProcess = true;
        } else {
            firstRound = false;
            refGroups.clear();
        }
    }

    private boolean findNextMatch(
            List<PrintablePomTaggedExpression> elements,
            LinkedList<PrintablePomTaggedExpression> backlog
    ) {
        boolean matched = false;
        while ( !elements.isEmpty() && !matched ) {
            PrintablePomTaggedExpression first = elements.remove(0);
            latestDepthExpression.currentReferenceNode = first;

            // first, we add the children of this element to the list to tests
            // but only if there are children lists to test
            if ( first.getPrintableComponents().size() > 0 ) {
                remaining.addLast( new DepthExpressionsCache(
                        latestDepthExpression.currentDepth+1, first.getPrintableComponents())
                );
            }

            matched = findNextMatchFromIndex(first, elements);

            if ( matched && leadingBackUpWildcard != null ) {
                if ( !backlog.isEmpty() ) {
                    matched = addLogicalGroupFromBacklog(backlog);
                } else matched = false;
            }

            updateBacklog(matched, first, backlog);
        }

        return matched;
    }

    private boolean findNextMatchFromIndex(
            PrintablePomTaggedExpression first,
            List<PrintablePomTaggedExpression> elements
    ) {
        boolean matched = false;
        if (isSequenceMatcher) {
            matched = findNextMatchFromIndexSequencer(first, elements);
        } else if ( !PomTaggedExpressionUtility.isAt(first) ){
            matched = matcher.match(first, elements, config);
        }
        return matched;
    }

    private boolean findNextMatchFromIndexSequencer(
            PrintablePomTaggedExpression first,
            List<PrintablePomTaggedExpression> elements
    ) {
        // than we take the first element, as the matcher...
        MatchablePomTaggedExpression m = matcherFirstElement;
        // if the first worked, we can move forward
        boolean innerTmpMatch = m.match(first, elements, config);
        while ( innerTmpMatch && !elements.isEmpty() && m.getNextSibling() != null ) {
            first = elements.remove(0);
            m = (MatchablePomTaggedExpression)m.getNextSibling();
            if ( config.ignoreNumberOfAts() && PomTaggedExpressionUtility.isAt(m) ) {
                continue;
            }
            innerTmpMatch = m.match(first, elements, config);
        }

        return matchConsideringMoreTokens(innerTmpMatch, m, elements);
    }

    private boolean matchConsideringMoreTokens(
            boolean innerTmpMatch,
            MatchablePomTaggedExpression m,
            List<PrintablePomTaggedExpression> elements
    ) {
        // match is only valid, if the regex does not assume more tokens
        boolean matched = (innerTmpMatch && m.getNextSibling() == null);
        if ( matched && elements.isEmpty() ) lastMatchWentUntilEnd = true;
        return matched;
    }

    private void updateBacklog(
            boolean matched,
            PrintablePomTaggedExpression first,
            LinkedList<PrintablePomTaggedExpression> backlog
    ) {
        if ( !matched ) {
            backlog.addLast(first);
            latestDepthExpression.passedExpressions.addLast(first);
            // we may accidentally found partial hits before, we must reset these
            refGroups.clear();
        }
    }

    private void storeLatestMatch(List<PrintablePomTaggedExpression> elements) {
        // if we found a match, we have to roll back the elements, if there
        // are elements remaining
        if ( !elements.isEmpty() && latestDepthExpression != null ) {
            remaining.addFirst(latestDepthExpression);
        }

        // we must add hits also... could be nested hits actually, right? ;)
        getCapturedGroupsAsList().stream()
                .filter( l -> l.size() > 1 )
                .map( l -> new DepthExpressionsCache(latestDepthExpression.currentDepth+1, l) )
                .forEach( remaining::addLast );
    }

    /**
     * This method replaces the wildcards in the given expression by the identified groups in each hit.
     * Afterward, every match is replaced by the updated expression.
     *
     * For example, consider your pattern is <code>x var1^2</code> with <code>var1</code> as the wildcard.
     * Now you match <code>x y^2 + x z^2</code> and replace every match by <code>(var1/2)</code>.
     * This would return the root of the tree <code>x (y/2)^2 + x (z/2)^2</code>.
     * @param expression the expression containing wildcards. The wildcards are replaced after a new match was found
     * @return the updated root
     * @throws ParseException if the updated expression (replaced wildcards) cannot be parsed
     */
    public PrintablePomTaggedExpression replacePattern(String expression) throws ParseException {
        reset();
        this.hasReplacedAnything = false;
        while ( find() ) {
            String replaced = PomMatcherUtility.fillPatterns(expression, groups());
            PrintablePomTaggedExpression p = matcher.getMLPWrapperInstance().parse(replaced);
            LinkedList<PrintablePomTaggedExpression> tmp = new LinkedList<>();
            tmp.add(p);
            replacePreviousHit(tmp);
            this.hasReplacedAnything = true;
        }
        return copy;
    }

    /**
     * Replaces every match by the provided replacement. This method is different to {@link #replacePattern(String)} because
     * it does not replace wild cards by the matches.
     * @param replacement the replacement
     * @return root of updated tree
     * @see #replacePreviousHit(List)
     */
    public PrintablePomTaggedExpression replace( List<PrintablePomTaggedExpression> replacement ) {
        reset();
        this.hasReplacedAnything = false;
        while ( find() ) {
            replacePreviousHit(replacement);
            this.hasReplacedAnything = true;
        }
        return copy;
    }

    /**
     * After calling {@link #replacePattern(String)} or {@link #replace(List)}, this method can be
     * used to ask if anything was replaced or not.
     * @return true if {@link #replacePattern(String)} or {@link #replace(List)} found any hit and performed
     * replacements, false otherwise.
     */
    public boolean performedReplacements() {
        return this.hasReplacedAnything;
    }

    /**
     * Replaces only the previous hit (identified by {@link #find()} by the given list of {@link PrintablePomTaggedExpression}.
     * The list can be a single element but not null. Calling the method multiple times without calling {@link #match()}
     * or {@link #find()} has no effect and simply returns the replaced expression again and again.
     * @param replacement the replacements
     * @return the root of the new tree with replaced expressions
     * @throws IllegalStateException if there were no match encountered
     * @throws NullPointerException if the provided list is null
     */
    public PrintablePomTaggedExpression replacePreviousHit( List<PrintablePomTaggedExpression> replacement )
            throws IllegalStateException{
        if ( !inProcess || latestDepthExpression == null ) {
            throw new IllegalStateException("No previous hit recorded!");
        } else if ( wasReplaced ) return copy;

        balanceReplacement(replacement);

        PomTaggedExpression parent = latestDepthExpression.currentReferenceNode.getParent();
        if ( parent == null ) {
            // essentially means, that the currentMatchReference is the root, which means we replace the entire
            // expression by a new one
            return replaceEntireExpression(replacement);
        }

        // clear the existing children
        PrintablePomTaggedExpression pparent = (PrintablePomTaggedExpression) parent;

        // add replacement to the passed components, since we won't check them again
        replacement.forEach( latestDepthExpression.passedExpressions::addLast );

        // create new components list for the parent
        LinkedList<PrintablePomTaggedExpression> newComponents =
                new LinkedList<>(latestDepthExpression.passedExpressions);
        newComponents.addAll( latestDepthExpression.remainingExpressions );
        pparent.setPrintableComponents( newComponents );

        wasReplaced = true;

        // does this work or did we use copies of copies?
        return copy;
    }

    private void balanceReplacement(List<PrintablePomTaggedExpression> replacement) {
        boolean wasBalanced =
                latestDepthExpression.currentReferenceNode.getTexString().matches("^\\s*\\{.+}\\s*$");
        if ( wasBalanced && replacement.size() == 1 ) {
            PrintablePomTaggedExpression ppte = replacement.get(0);
            ppte.makeBalancedTexString();
        }
    }

    private PrintablePomTaggedExpression replaceEntireExpression(List<PrintablePomTaggedExpression> replacement) {
        wasReplaced = true;
        if ( replacement.isEmpty() ) copy = FakeMLPGenerator.generateEmptySequencePPTE();
        else if ( replacement.size() == 1 ) copy = replacement.get(0);
        else {
            PrintablePomTaggedExpression r = FakeMLPGenerator.generateEmptySequencePPTE();
            replacement.forEach( r::addComponent );
            copy = r;
        }
        return copy;
    }

    private LinkedList<LinkedList<PrintablePomTaggedExpression>> getCapturedGroupsAsList() {
        LinkedList<LinkedList<PrintablePomTaggedExpression>>  result = new LinkedList<>();

        if ( matcher.isIsolatedWildcard() ) return result;

        Map<String, PrintablePomTaggedExpression> groups = matcher.getMatches();
        List<String> keys = new LinkedList<>(groups.keySet());

        // well, its better to keep them in order, otherwise its really strange what next find may return
        Collections.sort(keys);
        for ( String k : keys ) {
            PrintablePomTaggedExpression capturedGroup = groups.get(k);
            LinkedList<PrintablePomTaggedExpression> tmp;
            if ( PomTaggedExpressionUtility.isSequence(capturedGroup) ) {
                tmp = new LinkedList<>(capturedGroup.getPrintableComponents());
            } else {
                tmp = new LinkedList<>();
                tmp.addFirst(capturedGroup);
            }
            result.addLast(tmp);
        }
        return result;
    }

    private boolean addLogicalGroupFromBacklog(LinkedList<PrintablePomTaggedExpression> backlog) {
        PrintablePomTaggedExpression l = backlog.getLast();
        String s = l.getTexString();
        boolean capturedSuccessful;
        if ( s.startsWith("^") || s.startsWith("_") ) {
            LinkedList<PomTaggedExpression> tmp = new LinkedList<>();
            tmp.add(backlog.removeFirst());
            tmp.add(l);
            capturedSuccessful = refGroups.setCapturedGroup( leadingBackUpWildcard.getWildcardID(), tmp);
        } else {
            capturedSuccessful = refGroups.setCapturedGroup( leadingBackUpWildcard.getWildcardID(), backlog.removeLast() );
        }
        backlog.clear();
        return capturedSuccessful;
    }

    /**
     * Returns true if after the previous hit there were no tokens remaining to test.
     * @return true if there are no tokens remaining after a previous hit. If there were
     * no previous hits found, it always returns false. If the previous hit was an
     * exact match, it surely returns true.
     */
    boolean lastMatchReachedEnd() {
        return lastMatchWentUntilEnd;
    }

    /**
     * Resets the matcher (the next {@link #find()} starts at the beginning again.
     */
    public void reset() {
        inProcess = false;
        firstRound = true;
        lastMatchWentUntilEnd = false;
        refGroups.clear();
        copy = new PrintablePomTaggedExpression(orig);

        // reset lists
        remaining.clear();
        DepthExpressionsCache depthExpressionsCache = new DepthExpressionsCache(0);
        depthExpressionsCache.remainingExpressions.add(copy);
        remaining.add(depthExpressionsCache);

        wasReplaced = false;
    }

    /**
     * Returns the captured groups during a {@link #find()} or {@link #match()}.
     * The result will be empty (not null) if there were no match before.
     * @return the captured groups, where the key is the name of the wildcard and
     * the value the captured value.
     */
    public Map<String, String> groups() {
        return matcher.getStringMatches();
    }

    public GroupCaptures copyGroups() {
        return new GroupCaptures(matcher.getCaptures());
    }

    private static class DepthExpressionsCache {
        private final int currentDepth;
        private PrintablePomTaggedExpression currentReferenceNode = null;
        private final LinkedList<PrintablePomTaggedExpression> passedExpressions;
        private final LinkedList<PrintablePomTaggedExpression> remainingExpressions;

        DepthExpressionsCache(int depth) {
            this(depth, new LinkedList<>());
        }

        DepthExpressionsCache(int depth, List<PrintablePomTaggedExpression> expressions) {
            this.currentDepth = depth;
            this.remainingExpressions = new LinkedList<>(expressions);
            this.passedExpressions = new LinkedList<>();
        }
    }
}
