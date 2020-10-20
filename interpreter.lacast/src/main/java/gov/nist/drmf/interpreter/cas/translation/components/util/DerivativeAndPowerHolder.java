package gov.nist.drmf.interpreter.cas.translation.components.util;

import mlp.PomTaggedExpression;

/**
 * @author Andre Greiner-Petter
 */
public class DerivativeAndPowerHolder {
    private String differentiation = null;
    private PomTaggedExpression moveToEnd = null;
    private boolean complexDerivativeVar = false;

    public DerivativeAndPowerHolder(){}

    public void setDifferentiation(String differentiation) {
        this.differentiation = differentiation;
    }

    public void setMoveToEnd(PomTaggedExpression moveToEnd) {
        this.moveToEnd = moveToEnd;
    }

    public String getDifferentiation() {
        return differentiation;
    }

    public PomTaggedExpression getMoveToEnd() {
        return moveToEnd;
    }

    public boolean isComplexDerivativeVar() {
        return complexDerivativeVar;
    }

    public void setComplexDerivativeVar(boolean complexDerivativeVar) {
        this.complexDerivativeVar = complexDerivativeVar;
    }
}
