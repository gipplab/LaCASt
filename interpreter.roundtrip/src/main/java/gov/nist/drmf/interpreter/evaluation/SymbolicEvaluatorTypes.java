package gov.nist.drmf.interpreter.evaluation;

/**
 * @author Andre Greiner-Petter
 */
public enum SymbolicEvaluatorTypes {
    SIMPLE("Simple",    "Simple Simplify",                         true,  "", ""),
    CONV_EXP("ConvEXP", "EXPonential Conversion (ConvEXP)",        false, "convert(",  ", exp)" ),
    CONV_HYP("ConvHYP", "HYPergeometric Conversion (ConvHYP)",     false, "convert(",  ", hypergeom)" ),
    EXPAND("EXP", "Expansion (EXP)",                           false, "expand(",   ")"),
    EXPAND_EXP("EXP+EXP", "Expansion via EXP (EXP+EXP)",           false, "expand(",   ", exp)"),
    EXPAND_HYP("EXP+HYP", "Expansion via HYP (EXP+HYP)",           false, "expand(",   ", hypergeom)");

    private String shortName;
    private String name;
    private boolean activated;

    private String pre, post;

    SymbolicEvaluatorTypes( String shortName, String name, boolean activated, String pre, String post ){
        this.shortName = shortName;
        this.name = name;
        this.activated = activated;
        this.pre = pre;
        this.post = post;
    }

    public String getName() {
        return name;
    }

    public boolean isActivated() {
        return activated;
    }

    public void setActivated(boolean activated) {
        this.activated = activated;
    }

    public String buildCommand( String expr ){
        return pre + expr + post;
    }

    public String getShortName(){
        return shortName;
    }

    public String compactToString(){
        return shortName + ": " + ( activated ? "ON" : "OFF" );
    }

    @Override
    public String toString(){
        return name + ": " + ( activated ? "ON" : "OFF" );
    }
}
