package gov.nist.drmf.interpreter.mlp.extensions;

import gov.nist.drmf.interpreter.mlp.FakeMLPGenerator;
import gov.nist.drmf.interpreter.mlp.PomTaggedExpressionUtility;
import mlp.ParseException;
import mlp.PomTaggedExpression;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author Andre Greiner-Petter
 */
public class PomMatcher {
    private static final Logger LOG = LogManager.getLogger(PomMatcher.class.getName());

    private MatcherConfig config = MatcherConfig.getInPlaceMatchConfig();

    private final MatchablePomTaggedExpression matcher;
    private final PomTaggedExpressionChildrenMatcher children;
    private final PrintablePomTaggedExpression orig;
    private final GroupCaptures refGroups;

    private PrintablePomTaggedExpression copy;

    private boolean inProcess = false;
    private boolean lastMatchWentUntilEnd = false;

    private MatchablePomTaggedExpression leadingBackUpWildcard;

    private final LinkedList<DepthExpressionsCache> remaining;
    private DepthExpressionsCache latestDepthExpression;

    private boolean wasReplaced = false;

    /**
     * To get an instance of this class, you should use {@link MatchablePomTaggedExpression#matcher(String)}
     * or {@link MatchablePomTaggedExpression#matcher(PrintablePomTaggedExpression)}.
     * @param mpte the underlying matchable parse tree
     * @param pte the parse tree to match
     */
    PomMatcher(
            MatchablePomTaggedExpression mpte,
            PrintablePomTaggedExpression pte
    ) {
        this.matcher = mpte;
        this.children = mpte.getChildrenMatcher();
        this.orig = pte;
        this.refGroups = mpte.getCaptures();
        this.leadingBackUpWildcard = null;
        this.remaining = new LinkedList<>();
        this.latestDepthExpression = null;
        this.init();
    }

    private void init() {
        if ( children.isFirstChildWildcard() ) {
            leadingBackUpWildcard = children.removeFirst();
        }
    }

    public MatcherConfig getConfig() {
        return config;
    }

    public void setConfig(MatcherConfig config) {
        this.config = config;
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
        boolean res = matcher.match(copy, MatcherConfig.getExactMatchConfig());
        if ( res ) lastMatchWentUntilEnd = true;
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
        if ( !inProcess ) {
            reset();
            inProcess = true;
        } else {
            refGroups.clear();
        }

        wasReplaced = false;
        lastMatchWentUntilEnd = false;
        boolean matched = false;
        while ( !remaining.isEmpty() ) {
            // get the remaining list of children to work on
            latestDepthExpression = remaining.removeFirst();
            List<PrintablePomTaggedExpression> elements = latestDepthExpression.remainingExpressions;
            LinkedList<PrintablePomTaggedExpression> backlog = new LinkedList<>();

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

                if (PomTaggedExpressionUtility.isSequence(matcher)) {
                    // than we take the first element, as the matcher...
                    MatchablePomTaggedExpression m = (MatchablePomTaggedExpression)matcher.getComponents().get(0);
                    // if the first worked, we can move forward
                    boolean innerTmpMatch = m.match(first, elements, config);
                    while ( innerTmpMatch && !elements.isEmpty() && m.getNextSibling() != null ) {
                        first = elements.remove(0);
                        m = (MatchablePomTaggedExpression)m.getNextSibling();
                        innerTmpMatch = m.match(first, elements, config);
                    }
                    // match is only valid, if the regex does not assume more tokens
                    matched = (innerTmpMatch && m.getNextSibling() == null);
                    if ( matched && elements.isEmpty() ) lastMatchWentUntilEnd = true;
                } else {
                    matched = matcher.match(first, elements, config);
                }


                if ( !matched ) {
                    backlog.addLast(first);
                    latestDepthExpression.passedExpressions.addLast(first);
                    // we may accidentally found partial hits before, we must reset these
                    refGroups.clear();
                }

                if ( matched && leadingBackUpWildcard != null ) {
                    // check backlog, otherwise its false
                    if ( backlog.isEmpty() ) matched = false;
                    else {
                        addLogicalGroupFromBacklog(backlog);
                    }
                }
            }

            if ( matched ) {
                // if we found a match, we have to roll back the elements, if there
                // are elements remaining
                if ( !elements.isEmpty() ) {
                    remaining.addFirst(latestDepthExpression);
                }

                // we must add hits also... could be nested hits actually, right? ;)
                getCapturedGroupsAsList().stream()
                        .filter( l -> l.size() > 1 )
                        .map( l -> new DepthExpressionsCache(latestDepthExpression.currentDepth+1, l) )
                        .forEach( remaining::addLast );

                return true;
            }
        }

        return false;
    }

    public PrintablePomTaggedExpression replaceAll( String expression ) throws ParseException {
        reset();
        while ( find() ) {
            String replaced = PomMatcherUtility.fillPatterns(expression, groups());
            PrintablePomTaggedExpression p = matcher.getMLPWrapperInstance().parse(replaced);
//            if ( PomTaggedExpressionUtility.isSequence(p) ) {
//                replacePreviousHit(p.getPrintableComponents());
//            } else {
                LinkedList<PrintablePomTaggedExpression> tmp = new LinkedList<>();
                tmp.add(p);
                replacePreviousHit(tmp);
//            }
        }
        return copy;
    }

    public PrintablePomTaggedExpression replaceAll( List<PrintablePomTaggedExpression> replacement ) {
        reset();
        while ( find() ) {
            replacePreviousHit(replacement);
        }
        return copy;
    }

    /**
     *
     * @param replacement
     * @return
     * @throws IllegalStateException
     */
    public PrintablePomTaggedExpression replacePreviousHit( List<PrintablePomTaggedExpression> replacement )
            throws IllegalStateException{
        if ( !inProcess || latestDepthExpression == null ) {
            throw new IllegalStateException("No previous hit recorded!");
        } else if ( wasReplaced ) return copy;

        boolean wasBalanced = latestDepthExpression.currentReferenceNode.getTexString().matches("^\\s*\\{.+}\\s*$");
        if ( wasBalanced && replacement.size() == 1 ) {
            PrintablePomTaggedExpression ppte = replacement.get(0);
            ppte.makeBalancedTexString();
        }

        PomTaggedExpression parent = latestDepthExpression.currentReferenceNode.getParent();
        if ( parent == null ) {
            // essentially means, that the currentMatchReference is the root, which means we replace the entire
            // expression by a new one
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

    private LinkedList<LinkedList<PrintablePomTaggedExpression>> getCapturedGroupsAsList() {
        LinkedList<LinkedList<PrintablePomTaggedExpression>>  result = new LinkedList<>();
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

    private void addLogicalGroupFromBacklog(LinkedList<PrintablePomTaggedExpression> backlog) {
        PrintablePomTaggedExpression l = backlog.getLast();
        String s = l.getTexString();
        if ( s.startsWith("^") || s.startsWith("_") ) {
            LinkedList<PomTaggedExpression> tmp = new LinkedList<>();
            tmp.add(backlog.removeFirst());
            tmp.add(l);
            refGroups.setCapturedGroup( leadingBackUpWildcard.getWildcardID(), tmp);
        } else {
            refGroups.setCapturedGroup( leadingBackUpWildcard.getWildcardID(), backlog.removeLast() );
        }
        backlog.clear();
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
