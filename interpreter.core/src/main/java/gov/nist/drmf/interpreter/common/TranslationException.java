package gov.nist.drmf.interpreter.common;

/**
 * Created by AndreG-P on 03.03.2017.
 */
public class TranslationException extends RuntimeException {
    public TranslationException( String from_language, String to_language, String message ){
        super(
                "(" + from_language + " -> " + to_language + ") " + message
        );
    }

    public TranslationException( String from_language, String to_language, String message, Throwable throwable ){
        super(
                "(" + from_language + " -> " + to_language + ") " + message,
                throwable
        );
    }
}
