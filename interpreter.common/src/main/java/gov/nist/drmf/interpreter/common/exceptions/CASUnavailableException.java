package gov.nist.drmf.interpreter.common.exceptions;

/**
 * Indicates that the native code of a computer algebra system is
 * not available because it is either not installed on the system or
 * not properly configured to be used in the VM.
 *
 * @author Andre Greiner-Petter
 */
public class CASUnavailableException extends RuntimeException {
    public CASUnavailableException() {
        super();
    }

    public CASUnavailableException(Exception e) {
        super(e);
    }

    public CASUnavailableException(String msg) {
        super(msg);
    }

    public CASUnavailableException(String msg, Throwable e) {
        super(msg, e);
    }
}
