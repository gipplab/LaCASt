package gov.nist.drmf.interpreter.common.process;

/**
 * @author Andre Greiner-Petter
 */
public class UnrecoverableProcessException extends RuntimeException {
    public UnrecoverableProcessException() {
        super("Unable to recover from process crash.");
    }

    public UnrecoverableProcessException(Exception e) {
        super("Unable to recover from process crash.", e);
    }
}
