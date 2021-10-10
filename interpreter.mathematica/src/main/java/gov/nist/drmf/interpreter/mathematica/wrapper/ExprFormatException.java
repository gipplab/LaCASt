package gov.nist.drmf.interpreter.mathematica.wrapper;

public class ExprFormatException extends Throwable {
    public ExprFormatException(com.wolfram.jlink.ExprFormatException e) {
        super(e.getMessage(), e);
    }
}
