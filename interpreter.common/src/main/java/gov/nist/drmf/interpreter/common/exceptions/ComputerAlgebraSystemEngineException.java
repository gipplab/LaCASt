package gov.nist.drmf.interpreter.common.exceptions;

/**
 * @author Andre Greiner-Petter
 */
public class ComputerAlgebraSystemEngineException extends Exception {
    /**
     * Wrap original exception
     * @param exception original exception
     */
    public ComputerAlgebraSystemEngineException( Exception exception ) {
        super(exception);
    }
}
