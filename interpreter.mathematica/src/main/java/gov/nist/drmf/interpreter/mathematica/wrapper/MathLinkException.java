package gov.nist.drmf.interpreter.mathematica.wrapper;

public class MathLinkException extends RuntimeException {
    private final int errorCode;

    MathLinkException() {
        this(null);
    }

    MathLinkException(Throwable e) {
        super(e.getMessage(), e);
        this.errorCode = -1;
    }

    MathLinkException(int errorCode, Throwable e) {
        super(e.getMessage(), e);
        this.errorCode = errorCode;
    }

    public int getErrorCode() {
        return errorCode;
    }
}
