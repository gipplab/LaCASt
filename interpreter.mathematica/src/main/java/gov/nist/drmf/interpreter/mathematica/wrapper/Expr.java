package gov.nist.drmf.interpreter.mathematica.wrapper;

public class Expr {
    private final com.wolfram.jlink.Expr expr;

    public Expr(com.wolfram.jlink.Expr expr) {
        this.expr = expr;
    }

    public Expr[] args() {
        int len = expr.args().length;
        Expr[] out = new Expr[len];
        for ( int i =1; i<len;i++  ) {
            out[i] = new Expr(expr.args()[i]);
        }
        return out;
    }

    public int length() {
        return expr.length();
    }

    public boolean trueQ() {
        return expr.trueQ();
    }

    public boolean numberQ() {
        return expr.numberQ();
    }

    public double asDouble() throws ExprFormatException {
        try {
            return expr.asDouble();
        } catch (com.wolfram.jlink.ExprFormatException e) {
            throw new ExprFormatException(e);
        }
    }

    public Expr head() {
        return new Expr(expr.head());
    }

    public boolean listQ() {
        return expr.listQ();
    }

    public String asString() throws ExprFormatException {
        try {
            return expr.asString();
        } catch (com.wolfram.jlink.ExprFormatException e) {
            throw new ExprFormatException(e);
        }
    }

    @Override
    public String toString() {
        return expr.toString();
    }
}
