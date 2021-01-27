package gov.nist.drmf.interpreter.pom.extensions;

import gov.nist.drmf.interpreter.common.latex.TeXPreProcessor;
import mlp.PomTaggedExpression;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author Andre Greiner-Petter
 */
public class GroupCaptures {
    private static final Logger LOG = LogManager.getLogger(GroupCaptures.class.getName());

    /**
     * The library is a shared object among all nodes of one tree.
     * This is necessary to check the integrity within matches.
     */
    private final Map<String, List<PrintablePomTaggedExpression>> matchLibrary;

    public GroupCaptures() {
        this.matchLibrary = new HashMap<>();
    }

    public GroupCaptures(GroupCaptures copy) {
        this.matchLibrary = new HashMap<>(copy.matchLibrary);
    }

    /**
     * Clears the captured groups.
     */
    public void clear() {
        this.matchLibrary.clear();
    }

    /**
     * Returns whether the matched lib is empty or not.
     * @return true if no groups were captured
     */
    public boolean isEmpty() {
        return this.matchLibrary.isEmpty();
    }

    /**
     * Sets the captured for the given id
     * @param id group id
     * @param match the list of captures (might contain only one hit)
     * @return true if the match is valid. False if this captured was captured before and the new match clashes with
     * the previous hit.
     * For example if the group 'var1' captured 'x' before but is trying to capture 'y' it will throw the
     * exception. If it captures multiple times the same object, nothing happens.
     */
    public boolean setCapturedGroup(String id, List<PrintablePomTaggedExpression> match) {
        if ( match == null || match.size() < 1 ) throw new IllegalArgumentException("Cannot set the hit for an empty match.");
        if ( matchLibrary.containsKey(id) ) {
            return checkGroupIntegrity(id, match);
        } else {
            matchLibrary.put(id, match);
            return true;
        }
    }

    /**
     * Sets the captured for the given id
     * @param id group id
     * @param match the capture
     * @return true if the match is valid. False if this captured was captured before and the new match clashes with
     * the previous hit.
     * For example if the group 'var1' captured 'x' before but is trying to capture 'y' it will throw the
     * exception. If it captures multiple times the same object, nothing happens.
     */
    public boolean setCapturedGroup(String id, PrintablePomTaggedExpression match) {
        List<PrintablePomTaggedExpression> matches = new LinkedList<>();
        matches.add(match);
        return setCapturedGroup(id, matches);
    }

    private boolean checkGroupIntegrity(String id, List<PrintablePomTaggedExpression> matches) {
        List<PrintablePomTaggedExpression> previousMatches = matchLibrary.get(id);

        if ( previousMatches == null || previousMatches.isEmpty() ) return true;
        if ( previousMatches.size() != matches.size() ) {
            String msg = String.format("Match violates group integrity: %s matches clashed in size. " +
                    "Previous length '%d' vs now '%d'", id, previousMatches.size(), matches.size());
            LOG.debug(msg);
            return false;
        }

        for ( int i = 0; i < previousMatches.size(); i++ ) {
            if ( !checkIntegrity( id, previousMatches.get(i), matches.get(i) ) ) return false;
        }
        return true;
    }

    private boolean checkIntegrity(String id, PrintablePomTaggedExpression prev, PrintablePomTaggedExpression match) {
        String prevS = prev.getTexString();
        String matchS = match.getTexString();

        prevS = TeXPreProcessor.trimCurlyBrackets(prevS);
        matchS = TeXPreProcessor.trimCurlyBrackets(matchS);

        if ( !prevS.equals(matchS) ) {
            String msg = String.format("Match violates group integrity: %s matches clashed in '%s' vs '%s'", id, prevS, matchS);
            LOG.debug(msg);
            return false;
        } else return true;
    }

    /**
     * Get the grouped matches if any.
     * @return map of grouped matches
     * @see #getCapturedGroups()
     */
    public Map<String, String> getCapturedGroupStrings() {
        Map<String, String> out = new HashMap<>();
        Map<String, List<PrintablePomTaggedExpression>> matches = getCapturedGroups();

        for (String key : matches.keySet()) {
            List<PrintablePomTaggedExpression> matchList = matches.get(key);
            String caption = PrintablePomTaggedExpressionUtility.getCaptionOfPPTEs("", matchList);
            caption = TeXPreProcessor.trimIfWrappedInCurlyBrackets(caption);
            out.put(key, caption.trim());
        }

        return out;
    }

    /**
     * Get the grouped matches as the {@link PomTaggedExpression} that matched the wildcards.
     * Since every wildcard may match sequences of nodes, the returned mapping maps the key
     * of the pattern group to the list of hits.
     * @return map of grouped matches
     * @see #getCapturedGroupStrings()
     */
    public Map<String, List<PrintablePomTaggedExpression>> getCapturedGroups() {
        return matchLibrary;
    }
}
