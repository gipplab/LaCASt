package gov.nist.drmf.interpreter.pom.moi;

import gov.nist.drmf.interpreter.pom.extensions.GroupCaptures;
import gov.nist.drmf.interpreter.pom.extensions.PomMatcher;

/**
 * A dependency pattern represents the attribute of a directed edge in the MOI dependency graph, i.e.,
 * it is an attribute of {@link MOIDependency}.
 *
 * The attribute safes how the dependency was created, i.e., based on which pattern match.
 *
 * @see MOIDependency
 * @see MOIDependencyGraph
 * @see MOINode
 * @author Andre Greiner-Petter
 */
public class DependencyPattern {

    private final String pattern;

    private final PomMatcher matcher;

    public DependencyPattern(String pattern, PomMatcher matcher) {
        this.pattern = pattern;
        this.matcher = matcher;
    }

    public String getPattern() {
        return pattern;
    }

    public boolean exactMatch() {
        return this.matcher.latestHitMatchedExact();
    }

    public GroupCaptures getMatchedGroups() {
        return this.matcher.copyGroups();
    }
}
