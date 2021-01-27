package gov.nist.drmf.interpreter.maple.common;

import gov.nist.drmf.interpreter.common.eval.ISymbolicTestCases;
import gov.nist.drmf.interpreter.common.eval.SymbolicTestIDs;

/**
 * @author Andre Greiner-Petter
 */
public enum SymbolicMapleEvaluatorTypes implements ISymbolicTestCases {
    SIMPLE(SymbolicTestIDs.SIMPLE,    "Simple Simplify",                          true,  "",          ""),
    CONV_EXP(SymbolicTestIDs.CONV_EXP, "EXPonential Conversion (ConvEXP)",         false, "convert(",  ", exp)" ),
    CONV_HYP(SymbolicTestIDs.CONV_HYP, "HYPergeometric Conversion (ConvHYP)",      false, "convert(",  ", hypergeom)" ),
    EXPAND(SymbolicTestIDs.EXPAND, "Expansion (EXP)",                                false, "expand(",   ")"),
    EXPAND_EXP(SymbolicTestIDs.EXPAND_EXP, "Expansion via EXP (EXP+EXP)",            false, "expand(",   ", exp)"),
    EXPAND_HYP(SymbolicTestIDs.EXPAND_HYP, "Expansion via HYP (EXP+HYP)",            false, "expand(",   ", hypergeom)");

    private SymbolicTestIDs id;
    private String name;
    private boolean activated;

    private String pre, post;

    SymbolicMapleEvaluatorTypes(SymbolicTestIDs id, String name, boolean activated, String pre, String post ){
        this.id = id;
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
    public SymbolicTestIDs getID(){
        return id;
    }

    @Override
    public String compactToString(){
        return id.getId() + ": " + ( activated ? "ON" : "OFF" );
    }

    @Override
    public String toString(){
        return name + ": " + ( activated ? "ON" : "OFF" );
    }
}
