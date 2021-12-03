package gov.nist.drmf.interpreter.mathematica.wrapper;

public class ExprFormatException extends RuntimeException {
    ExprFormatException(Throwable e) {
        super(e.getMessage(), e);
    }
}
