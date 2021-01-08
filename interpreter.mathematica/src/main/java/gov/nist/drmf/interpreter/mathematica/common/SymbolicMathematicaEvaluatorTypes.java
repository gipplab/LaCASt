package gov.nist.drmf.interpreter.mathematica.common;

import gov.nist.drmf.interpreter.common.eval.ISymbolicTestCases;
import gov.nist.drmf.interpreter.common.eval.SymbolicTestIDs;

/**
 * @author Andre Greiner-Petter
 */
public enum SymbolicMathematicaEvaluatorTypes implements ISymbolicTestCases {
    SIMPLE(
            SymbolicTestIDs.SIMPLE,
            "Simple Simplify",
            true,   // simple case is always on
            "",     // simple is just simplified, there are no inner pre-post commands
            ""
    );

    private SymbolicTestIDs id;
    private String name;
    private boolean activated;

    private String pre, post;

    SymbolicMathematicaEvaluatorTypes(SymbolicTestIDs id, String name, boolean activated, String pre, String post ){
        this.id = id;
        this.name = name;
        this.activated = activated;
        this.pre = pre;
        this.post = post;
    }

    @Override
    public boolean isActivated() {
        return activated;
    }

    @Override
    public void setActivated(boolean activated) {
        this.activated = activated;
    }

    @Override
    public String buildCommand(String cmd) {
        return pre + cmd + post;
    }

    @Override
    public SymbolicTestIDs getID() {
        return id;
    }

    @Override
    public String compactToString() {
        return name + ": " + ( activated ? "ON" : "OFF" );
    }
}
