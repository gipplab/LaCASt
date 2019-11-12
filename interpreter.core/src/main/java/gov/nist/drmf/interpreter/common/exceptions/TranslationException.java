package gov.nist.drmf.interpreter.common.exceptions;

/**
 * Created by AndreG-P on 03.03.2017.
 */
public class TranslationException extends RuntimeException {
    private String toLang, fromLang;
    private TranslationExceptionReason reason;
//    private Object reason_Obj;

//    public TranslationException( String message ){
//        this ( "ukn", "ukn", message );
//    }
//
//    public TranslationException( String message, TranslationExceptionReason reason ){
//        this(message);
//        this.reason = reason;
//    }
//
//    public TranslationException( String message, Reason reason, Object reason_Obj ){
//        this(message);
//        this.reason = reason;
//        this.reason_Obj = reason_Obj;
//    }
//
//    public TranslationException( String message, Reason reason, Throwable throwable ){
//        super( message, throwable);
//        this.reason = reason;
//    }
//
//    public TranslationException( String message, Reason reason, Object reasonObj, Throwable throwable ){
//        this( message, reason, throwable);
//        this.reason_Obj = reasonObj;
//    }

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

    @Override
    public String toString(){
        String out = "(" + fromLang + " -> " + toLang + ") ";
        out += reason.toString() + " - ";
        return out + getLocalizedMessage();
    }
}
