package gov.nist.drmf.interpreter.mathematica.wrapper;

public class MathLinkException extends Exception {

    private final int errorCode;

    public MathLinkException(com.wolfram.jlink.MathLinkException e) {
        super(e.getMessage(), e);
        this.errorCode = e.getErrCode();
    }

    public int getErrorCode() {
        return errorCode;
    }
}
