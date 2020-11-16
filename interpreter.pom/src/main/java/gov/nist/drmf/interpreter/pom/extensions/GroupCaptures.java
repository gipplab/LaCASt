package gov.nist.drmf.interpreter.pom.extensions;

import gov.nist.drmf.interpreter.common.TeXPreProcessor;
import gov.nist.drmf.interpreter.common.exceptions.NotMatchableException;
import gov.nist.drmf.interpreter.pom.common.grammar.Brackets;
import gov.nist.drmf.interpreter.pom.common.FakeMLPGenerator;
import mlp.PomTaggedExpression;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

/**
 * @author Andre Greiner-Petter
 */
public class GroupCaptures {
    /**
     * The library is a shared object among all nodes of one tree.
     * This is necessary to check the integrity within matches.
     */
    private final Map<String, PrintablePomTaggedExpression> matchLibrary;

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
     * @throws NotMatchableException
     * Will be thrown if this captured was captured before and the new match clashes with the previous hit.
     * For example if the group 'var1' captured 'x' before but is trying to capture 'y' it will throw the
     * exception. If it captures multiple times the same object, nothing happens.
     */
    public void setCapturedGroup(String id, List<PomTaggedExpression> match)
            throws NotMatchableException {
        PrintablePomTaggedExpression m;
        if ( match.size() > 1 ) {
            m = FakeMLPGenerator.generateEmptySequencePPTE();
            m.setComponents(match);
        } else if ( match.size() == 1 ) m = (PrintablePomTaggedExpression) match.get(0);
        else throw new IllegalArgumentException("Cannot set the hit for an empty match.");
        setCapturedGroup(id, m);
    }

    /**
     * Sets the captured for the given id
     * @param id group id
     * @param match the capture
     * @throws NotMatchableException
     * Will be thrown if this captured was captured before and the new match clashes with the previous hit.
     * For example if the group 'var1' captured 'x' before but is trying to capture 'y' it will throw the
     * exception. If it captures multiple times the same object, nothing happens.
     */
    public void setCapturedGroup(String id, PrintablePomTaggedExpression match)
            throws NotMatchableException {
        if ( matchLibrary.containsKey(id) ) {
            checkGroupIntegrity(id, match);
        } else {
            matchLibrary.put(id, match);
        }
    }

    private void checkGroupIntegrity(String id, PrintablePomTaggedExpression match) {
        String prev = matchLibrary.get(id).getTexString();
        String matchS = match.getTexString();

        prev = TeXPreProcessor.trimCurlyBrackets(prev);
        matchS = TeXPreProcessor.trimCurlyBrackets(matchS);

        if ( !prev.equals(matchS) ) {
            String msg = String.format("%s matches clashed in '%s' vs '%s'", id, prev, matchS);
            throw new NotMatchableException(msg);
        }
    }

    /**
     * Get the grouped matches if any.
     * @return map of grouped matches
     * @see #getCapturedGroups()
     */
    public Map<String, String> getCapturedGroupStrings() {
        Map<String, String> out = new HashMap<>();
        Map<String, PrintablePomTaggedExpression> matches = getCapturedGroups();

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
     * @see #getCapturedGroupStrings()
     */
    public Map<String, PrintablePomTaggedExpression> getCapturedGroups() {
        return matchLibrary;
    }
}
