package gov.nist.drmf.interpreter.evaluation.common;

/**
 * @author Andre Greiner-Petter
 */
public enum Relations {
    EQUAL("=", "="),
    UNEQUAL("<>", "\\neq"),
    GREATER_THAN(">", ">"),
    GREATER_EQ_THAN(">=", "\\geq"),
    LESS_THAN("<", "<"),
    LESS_EQ_THAN("<=", "\\leq");

    private String symbol;
    private String texSymbol;

    Relations( String symbol, String texSymbol ) {
        this.symbol = symbol;
        this.texSymbol = texSymbol;
    }

    public String getSymbol(){
        return symbol;
    }

    public String getTexSymbol() {
        return texSymbol;
    }
}
