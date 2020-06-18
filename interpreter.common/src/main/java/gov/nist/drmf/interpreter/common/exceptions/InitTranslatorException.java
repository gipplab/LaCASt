package gov.nist.drmf.interpreter.common.exceptions;

/**
 * Is thrown if the translator object is unable to initiated.
 * @author Andre Greiner-Petter
 */
public class InitTranslatorException extends Exception {
    public InitTranslatorException(Throwable cause) {
        super(cause);
    }

    public InitTranslatorException(String message) {
        super(message);
    }

    public InitTranslatorException(String message, Throwable cause) {
        super(message, cause);
    }
}
