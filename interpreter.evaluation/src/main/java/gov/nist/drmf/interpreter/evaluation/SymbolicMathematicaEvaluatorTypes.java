package gov.nist.drmf.interpreter.evaluation;

/**
 * @author Andre Greiner-Petter
 */
public enum SymbolicMathematicaEvaluatorTypes implements ISymbolicTestCases {
    SIMPLE(
            "Simple",
            "Simple Simplify",
            true,   // simple case is always on
            "",     // simple is just simplified, there are no inner pre-post commands
            ""
    );

    private String shortName;
    private String name;
    private boolean activated;

    private String pre, post;

    SymbolicMathematicaEvaluatorTypes(String shortName, String name, boolean activated, String pre, String post ){
        this.shortName = shortName;
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
    public String getShortName() {
        return shortName;
    }

    @Override
    public String compactToString() {
        return name + ": " + ( activated ? "ON" : "OFF" );
    }
}
