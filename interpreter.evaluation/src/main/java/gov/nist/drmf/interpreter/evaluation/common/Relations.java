package gov.nist.drmf.interpreter.evaluation.common;

/**
 * @author Andre Greiner-Petter
 */
public enum Relations {
    EQUAL("="),
    UNEQUAL("<>"),
    GREATER_THAN(">"),
    GREATER_EQ_THAN(">="),
    LESS_THAN("<"),
    LESS_EQ_THAN("<=");

    private String symbol;

    Relations( String symbol ) {
        this.symbol = symbol;
    }

    public String getSymbol(){
        return symbol;
    }
}
