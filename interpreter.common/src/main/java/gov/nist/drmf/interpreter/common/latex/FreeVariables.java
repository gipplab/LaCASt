package gov.nist.drmf.interpreter.common.latex;

import gov.nist.drmf.interpreter.common.meta.ListExtender;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

import static java.util.function.Predicate.not;

/**
 * @author Andre Greiner-Petter
 */
public class FreeVariables {
    private static final Logger LOG = LogManager.getLogger(FreeVariables.class.getName());

    private LinkedList<String> freeVariables;

    private Set<String> temporarilySuppressedVars;

    private int suppressionStartPosition;

    public FreeVariables() {
        freeVariables = new LinkedList<>();
        temporarilySuppressedVars = new HashSet<>();
        suppressionStartPosition = -1;
    }

    public FreeVariables(FreeVariables fvars) {
        freeVariables = new LinkedList<>(fvars.freeVariables);
        temporarilySuppressedVars = new HashSet<>(fvars.temporarilySuppressedVars);
        suppressionStartPosition = fvars.suppressionStartPosition;
    }

    public void clear() {
        freeVariables.clear();
        temporarilySuppressedVars.clear();
        suppressionStartPosition = -1;
    }

    public void addFreeVariables(FreeVariables otherVars) {
        addFreeVariables(otherVars.freeVariables);
    }

    public void replaceFreeVariables(FreeVariables otherVars) {
        freeVariables.clear();
        temporarilySuppressedVars.clear();
        freeVariables.addAll(otherVars.freeVariables);
        temporarilySuppressedVars.addAll( otherVars.temporarilySuppressedVars );
        suppressionStartPosition = otherVars.suppressionStartPosition;
    }

    public void suppressingVars(String... vars) {
        suppressionStartPosition = freeVariables.size();
        if ( vars == null ) return;
        suppressingVars(Arrays.asList(vars));
    }

    public void suppressingVars(Collection<String> vars) {
        suppressionStartPosition = freeVariables.size();
        if ( vars == null ) return;
        LOG.debug("Suppress free variables " + vars);
        temporarilySuppressedVars.addAll(vars);
    }

    public void releaseVars(String... vars) {
        if ( vars == null ) {
            suppressionStartPosition = -1;
            return;
        }
        releaseVars(Arrays.asList(vars));
    }

    public void releaseVars(Collection<String> vars) {
        LOG.debug("Release free variables " + vars);
        deleteFreeVariablesSinceSuppression(vars);
        suppressionStartPosition = -1;
        if ( vars == null ) return;
        temporarilySuppressedVars.removeAll(vars);
    }

    public void releaseAllVars() {
        LOG.debug("Release all free variables");
        suppressionStartPosition = -1;
        temporarilySuppressedVars.clear();
    }

    public void addFreeVariable(String... vars) {
        addFreeVariables(Arrays.asList(vars));
    }

    public void addFreeVariables(List<String> vars) {
        if ( vars == null || vars.size() == 0 ) return;
        ListExtender.addAll(
                freeVariables, vars, not(temporarilySuppressedVars::contains)
        );
    }

    public void deleteFreeVariablesSinceSuppression(String... vars) {
        if ( vars == null ) return;
        deleteFreeVariablesSinceSuppression(Arrays.asList(vars));
    }

    public void deleteFreeVariablesSinceSuppression(Collection<String> vars) {
        if ( vars == null ) return;
        Set<String> varsSet = new TreeSet<>(vars);
        for ( int i = freeVariables.size() - 1; i >= suppressionStartPosition && i >= 0; i-- ) {
            String v = freeVariables.get(i);
            if ( varsSet.contains(v) ) freeVariables.remove(i);
        }
    }

    public boolean removeLastVariable(String var) {
        if ( freeVariables.isEmpty() ) return false;
        if ( freeVariables.getLast().equals(var) ) {
            freeVariables.removeLast();
            return true;
        } return false;
    }

    public Set<String> getFreeVariables() {
        return new TreeSet<>(freeVariables);
    }
}
