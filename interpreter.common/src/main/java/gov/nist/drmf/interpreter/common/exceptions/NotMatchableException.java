package gov.nist.drmf.interpreter.common.exceptions;

/**
 * This exception indicates that it is impossible to create a valid matchable
 * blueprint of a given expression.
 *
 * @author Andre Greiner-Petter
 */
public class NotMatchableException extends IllegalArgumentException {
    public NotMatchableException() {
        super();
    }

    public NotMatchableException(String message) {
        super(message);
    }

    public NotMatchableException(Throwable cause) {
        super(cause);
    }
}
