package gov.nist.drmf.interpreter.mlp.extensions;

import gov.nist.drmf.interpreter.common.grammar.ExpressionTags;
import gov.nist.drmf.interpreter.mlp.FakeMLPGenerator;
import gov.nist.drmf.interpreter.mlp.PomTaggedExpressionUtility;
import mlp.PomTaggedExpression;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Andre Greiner-Petter
 */
public class PomMatcher {
    private final MatchablePomTaggedExpression matcher;
    private final PrintablePomTaggedExpression orig;
    private final GroupCaptures refGroups;

    private PrintablePomTaggedExpression copy;

    private boolean inProcess = false;

    private MatchablePomTaggedExpression leadingBackUpWildcard;

    private LinkedList<LinkedList<PrintablePomTaggedExpression>> remaining;
    private LinkedList<PrintablePomTaggedExpression> previousCache;

    private MatcherConfig defaultFindMatcherConfig = new MatcherConfig(true, false);

    /**
     * To get an instance of this class, you should use {@link MatchablePomTaggedExpression#matcher(String)}
     * or {@link MatchablePomTaggedExpression#matcher(PrintablePomTaggedExpression)}.
     * @param mpte the underlying matchable parse tree
     * @param pte the parse tree to match
     * @param refGroups the group captures object from {@param mpte}.
     */
    PomMatcher(
            MatchablePomTaggedExpression mpte,
            PrintablePomTaggedExpression pte,
            GroupCaptures refGroups
    ) {
        this.matcher = mpte;
        this.orig = pte;
        this.refGroups = refGroups;
        this.leadingBackUpWildcard = null;
        remaining = new LinkedList<>();
        previousCache = new LinkedList<>();
        this.init();
    }

    private void init() {
        if ( matcher.childrenMatcher().isFirstChildWildcard() ) {
            leadingBackUpWildcard = matcher.childrenMatcher().removeFirst();
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
        return matcher.match(copy, new MatcherConfig(false, false));
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

        if ( remaining.isEmpty() ) {
            return false;
        }

        boolean matched = false;
        while ( !remaining.isEmpty() && !matched ) {
            // get the remaining list of children to work on
            LinkedList<PrintablePomTaggedExpression> elements = remaining.removeFirst();
            LinkedList<PrintablePomTaggedExpression> backlog = new LinkedList<>();

            while ( !elements.isEmpty() && !matched ) {
                PrintablePomTaggedExpression first = elements.removeFirst();

                // first, we add the children of this element to the list to tests
                // but only if there are children lists to test
                if ( first.getPrintableComponents().size() > 0 ) {
                    remaining.addLast( first.getPrintableComponents() );
                }

                if (PomTaggedExpressionUtility.isSequence(matcher)) {
                    // than we take the first element, as the matcher...
                    MatchablePomTaggedExpression m = (MatchablePomTaggedExpression)matcher.getComponents().get(0);
                    // if the first worked, we can move forward
                    boolean innerTmpMatch = m.match(first, elements, defaultFindMatcherConfig);
                    while ( innerTmpMatch && !elements.isEmpty() && m.getNextSibling() != null ) {
                        first = elements.removeFirst();
                        m = (MatchablePomTaggedExpression)m.getNextSibling();
                        innerTmpMatch = m.match(first, elements, defaultFindMatcherConfig);
                    }
                    matched = innerTmpMatch;
                } else {
                    matched = matcher.match(first, elements, defaultFindMatcherConfig);
                }

                if ( !matched ) backlog.addLast(first);

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
                    remaining.addFirst(elements);
                }

                // we must add hits also... could be nested hits actually, right? ;)
                remaining.addAll( getCapturedGroupsAsList() );

                return matched;
            }
        }

        return matched;
    }

    private void addLogicalGroupFromBacklog(LinkedList<PrintablePomTaggedExpression> backlog) {
        PrintablePomTaggedExpression l = backlog.getLast();
        String s = l.getTexString();
        if ( s.startsWith("^") || s.startsWith("_") ) {
            LinkedList<PomTaggedExpression> tmp = new LinkedList<>();
            tmp.add(backlog.removeFirst());
            tmp.add(l);
            refGroups.setCapturedGroup(leadingBackUpWildcard.getWildcardID(), tmp);
        } else {
            refGroups.setCapturedGroup( leadingBackUpWildcard.getWildcardID(), backlog.removeLast() );
        }
        backlog.clear();
    }

    /**
     * Resets the matcher (the next {@link #find()} starts at the beginning again.
     */
    public void reset() {
        refGroups.clear();
        copy = new PrintablePomTaggedExpression(orig);

        // reset lists
        remaining.clear();
        LinkedList<PrintablePomTaggedExpression> tmp = new LinkedList<>();
        tmp.add(copy);
        remaining.add(tmp);
        previousCache.clear();
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

    /**
     * Returns the captured groups during a {@link #find()} or {@link #match()}.
     * The result will be empty (not null) if there were no match before.
     * @return the captured groups, where the key is the name of the wildcard and
     * the value the captured value.
     */
    public Map<String, String> groups() {
        return matcher.getStringMatches();
    }
}
