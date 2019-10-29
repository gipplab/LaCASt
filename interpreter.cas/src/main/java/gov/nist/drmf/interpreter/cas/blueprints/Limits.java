package gov.nist.drmf.interpreter.cas.blueprints;

import java.util.LinkedList;
import java.util.List;

/**
 * This class contains the limits of sums, prods, ints, and lims.
 * Note that the vars and bounds are NOT translated yet.
 *
 * @author Andre Greiner-Petter
 */
public class Limits {
    public static String DEFAULT_UPPER_LIMIT = "\\infty";

    private List<String> vars, upper, lower;

    private boolean isLimitOverSet = false;

    public Limits(List<String> vars, List<String> lower, List<String> upper) {
        this.vars = vars;
        this.upper = upper;
        this.lower = lower;
    }

    public void setLimitOverSet(boolean limitOverSet) {
        isLimitOverSet = limitOverSet;
    }

    public List<String> getVars() {
        return vars;
    }

    public List<String> getUpper() {
        return upper;
    }

    public List<String> getLower() {
        return lower;
    }

    public boolean isLimitOverSet() {
        return isLimitOverSet;
    }

    public void overwriteUpperLimit(String newUpper) {
        for ( int i = 0; i < upper.size(); i++ ) {
            upper.set(i, newUpper);
        }
    }
}
