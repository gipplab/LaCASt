package gov.nist.drmf.interpreter.pom.extensions;

import gov.nist.drmf.interpreter.common.latex.TeXPreProcessor;
import gov.nist.drmf.interpreter.common.meta.TriConsumer;
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

    /**
     * This contains the optional hits for semantic macros
     */
    private final Map<String, List<List<PrintablePomTaggedExpression>>> optionalSemanticMacroMatchLibrary;

    public GroupCaptures() {
        this.matchLibrary = new HashMap<>();
        this.optionalSemanticMacroMatchLibrary = new HashMap<>();
    }

    public GroupCaptures(GroupCaptures copy) {
        this.matchLibrary = new HashMap<>(copy.matchLibrary);
        this.optionalSemanticMacroMatchLibrary = new HashMap<>(copy.optionalSemanticMacroMatchLibrary);
    }

    /**
     * Clears the captured groups.
     */
    public void clear() {
        this.matchLibrary.clear();
        this.optionalSemanticMacroMatchLibrary.clear();
    }

    /**
     * Returns whether the matched lib is empty or not.
     * @return true if no groups were captured
     */
    public boolean isEmpty() {
        return this.matchLibrary.isEmpty() && optionalSemanticMacroMatchLibrary.isEmpty();
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

    /**
     * Sets the optional semantic macro capture for the given macro
     * @param macro the ID
     * @param match the captured match
     */
    public void setOptionalSemanticCapturedGroup(String macro, List<PrintablePomTaggedExpression> match) {
        String key = macro.replaceAll("\\\\", "");
        List<List<PrintablePomTaggedExpression>> prevMatches = optionalSemanticMacroMatchLibrary.computeIfAbsent(
               key , (m) -> new LinkedList<>() );
        prevMatches.add(match);
    }

    /**
     * Sets the optional semantic macro capture for the given macro
     * @param macro the ID
     * @param match the captured match
     */
    public void setOptionalSemanticCapturedGroup(String macro, PrintablePomTaggedExpression match) {
        List<PrintablePomTaggedExpression> matches = new LinkedList<>();
        matches.add(match);
        this.setOptionalSemanticCapturedGroup(macro, matches);
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
            String msg = String.format("Match violates group integrity: '%s' match clashed in '%s' vs '%s'", id, prevS, matchS);
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
        return buildCapturedGroupStrings(
                getCapturedGroups(),
                (out, matchList, key) -> {
                    String caption = PrintablePomTaggedExpressionUtility.getCaptionOfPPTEs("", matchList);
                    caption = TeXPreProcessor.trimIfWrappedInCurlyBrackets(caption);
                    out.put(key, caption.trim());
                }
        );
    }

    /**
     * Get the grouped optional matches found behind semantic macros. For example, {@code \EulerGamma{var0}}
     * may match {@code \EulerGamma'{x}}. If so, the general captured group {@link #getCapturedGroupStrings()} would
     * contain a hit for {@code var0} and this method contains an element for {@code optEulerGamma0}.
     *
     * The {@code 0} is referring to the position the optional group was captured. For example, in case the matcher
     * would match the same macro twice, e.g., {@code \EulerGamma{var0} + \EulerGamma{var0}}, this group may capture
     * multiple optional groups. Consider {@code \EulerGamma'{x} + \EulerGamma^2{x}} would match and
     * {@link #getCapturedGroupStrings()} contains {@code var0 -> x}. In that case, this method returns a map with two
     * entries {@code optEulerGamma0 -> '} and {@code optEulerGamma1 -> ^2}.
     *
     * @return the string representation of optional captured groups behind semantic macros. These groups are only
     * captured if the matching was configured with active {@link MatcherConfig#isSemanticMacroIgnoreTokenRule()}.
     */
    public Map<String, String> getOptionalSemanticCapturedGroupStrings() {
        return buildCapturedGroupStrings(
                optionalSemanticMacroMatchLibrary,
                (out, matchListList, key) -> {
                    for ( int i = 0; i < matchListList.size(); i++ ) {
                        List<PrintablePomTaggedExpression> matchList = matchListList.get(i);
                        String caption = PrintablePomTaggedExpressionUtility.getCaptionOfPPTEs("", matchList);
                        caption = TeXPreProcessor.trimIfWrappedInCurlyBrackets(caption);
                        out.put("opt" + key + i, caption.trim());
                    }
                }
        );
    }

    private <V> Map<String, String> buildCapturedGroupStrings(
            Map<String, V> matches,
            TriConsumer<Map<String, String>, V, String> consumer
    ) {
        Map<String, String> out = new HashMap<>();

        for (String key : matches.keySet()) {
            V matchList = matches.get(key);
            consumer.accept(out, matchList, key);
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

    /**
     * See {@link #getOptionalSemanticCapturedGroupStrings()} for a description of the behaviour.
     * @return map of captured optional groups behind semantic macros
     */
    public Map<String, List<PrintablePomTaggedExpression>> getOptionalSemanticCapturedGroups() {
        Map<String, List<PrintablePomTaggedExpression>> out = new HashMap<>();
        for ( String key : optionalSemanticMacroMatchLibrary.keySet() ) {
            List<List<PrintablePomTaggedExpression>> matchListList = optionalSemanticMacroMatchLibrary.get(key);
            for ( int i = 0; i < matchListList.size(); i++ ) {
                List<PrintablePomTaggedExpression> matchList = matchListList.get(i);
                out.put("opt" + key + i, matchList);
            }
        }
        return out;
    }
}
