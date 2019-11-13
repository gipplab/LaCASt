package gov.nist.drmf.interpreter.common.exceptions;

/**
 * Created by AndreG-P on 03.03.2017.
 */
public class TranslationException extends RuntimeException {
    private String toLang, fromLang;
    private TranslationExceptionReason reason;
    private Object reason_Obj;

    public TranslationException(
            String from_language,
            String to_language,
            String message,
            TranslationExceptionReason reason,
            Throwable throwable
    ){
        super(message, throwable);
        this.fromLang = from_language;
        this.toLang = to_language;
        this.reason = reason;
    }

    public TranslationException(
            String from_language,
            String to_language,
            String message,
            TranslationExceptionReason reason
    ){
        super(message);
        this.fromLang = from_language;
        this.toLang = to_language;
        this.reason = reason;
    }

    public TranslationExceptionReason getReason(){
        return reason;
    }

    public Object getReasonObj() {
        return reason_Obj;
    }

    public void setReasonObj(Object reason_Obj) {
        this.reason_Obj = reason_Obj;
    }

    @Override
    public String toString(){
        String out = "(" + fromLang + " -> " + toLang + ") ";
        out += reason.toString();
        out += ": " + getLocalizedMessage();
        out += reason_Obj != null ? " [" + reason_Obj + "]" : "";
        return out;
    }
}
