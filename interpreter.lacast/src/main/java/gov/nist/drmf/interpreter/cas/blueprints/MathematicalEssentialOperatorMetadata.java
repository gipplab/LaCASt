package gov.nist.drmf.interpreter.cas.blueprints;

import gov.nist.drmf.interpreter.pom.common.grammar.LimDirections;
import gov.nist.drmf.interpreter.pom.common.grammar.LimitedExpressions;

import java.util.LinkedList;
import java.util.List;

/**
 * This class contains the limits of sums, prods, ints, and lims.
 * Note that the vars and bounds are NOT translated yet.
 *
 * @author Andre Greiner-Petter
 */
public class MathematicalEssentialOperatorMetadata {
    public static String DEFAULT_UPPER_LIMIT = "\\infty";

    private final List<String> vars, upper, lower;
    private LimDirections direction;

    private boolean isLimitOverSet = false;

    public MathematicalEssentialOperatorMetadata() {
        this.vars = new LinkedList<>();
        this.upper = new LinkedList<>();
        this.lower = new LinkedList<>();
    }

    public MathematicalEssentialOperatorMetadata(List<String> vars, List<String> lower, List<String> upper) {
        this.vars = vars;
        this.upper = upper;
        this.lower = lower;
    }

    public void setLimitOverSet(boolean limitOverSet) {
        isLimitOverSet = limitOverSet;
    }

    public void setDirection(LimDirections direction) {
        this.direction = direction;
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

    public LimDirections getDirection() {
        return direction;
    }

    public boolean isLimitOverSet() {
        return isLimitOverSet;
    }

    public void overwriteUpperLimit(String newUpper) {
        for ( int i = 0; i < upper.size(); i++ ) {
            upper.set(i, newUpper);
        }
    }

    public BoundaryStrings getArguments(int index, boolean indef, String arg, LimitedExpressions category) {
        BoundaryStrings out = new BoundaryStrings();
        if ( indef ) {
            out.args = new String[]{vars.get(index), arg};
            out.categoryKey = category.getIndefKey();
            return out;
        }

        if ( isLimitOverSet || direction != null ) {
            out.args = new String[] {
                    vars.get(index),
                    lower.get(index),
                    arg
            };
            out.categoryKey = isLimitOverSet ?
                    category.getSetKey() :
                    category.getDirectionKey(direction);
            return out;
        }

        out.args = new String[]{
                vars.get(index),
                lower.get(index),
                upper.get(index),
                arg,
        };
        out.categoryKey = category.getKey();
        return out;
    }

    public class BoundaryStrings {
        private String[] args;
        private String categoryKey;

        public String[] getArgs() {
            return args;
        }

        public String getCategoryKey() {
            return categoryKey;
        }
    }
}
