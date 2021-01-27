package gov.nist.drmf.interpreter.evaluation.common;

import gov.nist.drmf.interpreter.common.eval.Label;

/**
 * @author Andre Greiner-Petter
 */
public class SimpleCase {
    private int line;
    private String expression;
    private Label label;

    public SimpleCase( int line, String expr, Label label ){
        this.line = line;
        this.expression = expr;
        this.label = label;
    }

    public int getLine() {
        return line;
    }

    public String getExpression() {
        return expression;
    }

    public Label getLabel() {
        return label;
    }

    public String getEquationLabel() {
        if ( label != null ) return label.getLabel();
        return null;
    }
}
