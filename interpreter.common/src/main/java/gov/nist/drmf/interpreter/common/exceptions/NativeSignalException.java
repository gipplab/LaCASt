package gov.nist.drmf.interpreter.common.exceptions;

/**
 * @author Andre Greiner-Petter
 */
public class NativeSignalException extends Exception {
    public NativeSignalException() {
        super();
    }

    public NativeSignalException(int signal) {
        super("Sub-process finished with signal " + signal);
    }

}
