package gov.nist.drmf.interpreter.common.eval;

public enum SymbolicTestIDs {
    SIMPLE("Simple"),
    CONV_EXP("ConvEXP"), CONV_HYP("ConvHYP"),
    EXPAND("EXP"), EXPAND_EXP("EXP+EXP"), EXPAND_HYP("EXP+HYP");

    private final String id;

    SymbolicTestIDs(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
