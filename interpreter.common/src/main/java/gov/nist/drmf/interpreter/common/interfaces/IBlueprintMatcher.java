package gov.nist.drmf.interpreter.common.interfaces;

import java.util.List;

/**
 * @author Andre Greiner-Petter
 */
public interface IBlueprintMatcher {
    /**
     * True if the given expression matches this blueprint matcher
     * @param expression the expression
     * @return true if it matches
     */
    boolean match(String expression);
}
