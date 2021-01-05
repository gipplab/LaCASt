package gov.nist.drmf.interpreter.maple.common;

import gov.nist.drmf.interpreter.common.eval.ISymbolicTestCases;

/**
 * @author Andre Greiner-Petter
 */
public enum SymbolicMapleEvaluatorTypes implements ISymbolicTestCases {
    SIMPLE("Simple",    "Simple Simplify",                          true,  "",          ""),
    CONV_EXP("ConvEXP", "EXPonential Conversion (ConvEXP)",         false, "convert(",  ", exp)" ),
    CONV_HYP("ConvHYP", "HYPergeometric Conversion (ConvHYP)",      false, "convert(",  ", hypergeom)" ),
    EXPAND("EXP", "Expansion (EXP)",                                false, "expand(",   ")"),
    EXPAND_EXP("EXP+EXP", "Expansion via EXP (EXP+EXP)",            false, "expand(",   ", exp)"),
    EXPAND_HYP("EXP+HYP", "Expansion via HYP (EXP+HYP)",            false, "expand(",   ", hypergeom)");

    private String shortName;
    private String name;
    private boolean activated;

    private String pre, post;

    SymbolicMapleEvaluatorTypes(String shortName, String name, boolean activated, String pre, String post ){
        this.shortName = shortName;
        this.name = name;
        this.activated = activated;
        this.pre = pre;
        this.post = post;
    }

    public String getName() {
        return name;
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
    public String buildCommand( String expr ){
        return pre + expr + post;
    }

    @Override
    public String getShortName(){
        return shortName;
    }

    @Override
    public String compactToString(){
        return shortName + ": " + ( activated ? "ON" : "OFF" );
    }

    @Override
    public String toString(){
        return name + ": " + ( activated ? "ON" : "OFF" );
    }
}
