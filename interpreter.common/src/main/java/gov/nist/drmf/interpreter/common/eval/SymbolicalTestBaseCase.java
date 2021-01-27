package gov.nist.drmf.interpreter.common.eval;

import java.io.Serializable;

/**
 * @author Andre Greiner-Petter
 */
public class SymbolicalTestBaseCase implements Serializable {

    private String lhs, rhs;

    private String testExpression;

    public SymbolicalTestBaseCase(String testExpression) {
        this.lhs = testExpression;
        this.rhs = "";
        this.testExpression = testExpression;
    }

    public SymbolicalTestBaseCase(String lhs, String rhs, String testExpression) {
        this.lhs = lhs;
        this.rhs = rhs;
        this.testExpression = testExpression;
    }

    public String getLhs() {
        return lhs;
    }

    public String getRhs() {
        return rhs;
    }

    public String getTestExpression() {
        return testExpression;
    }
}
