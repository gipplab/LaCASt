package gov.nist.drmf.interpreter.common.exceptions;

import gov.nist.drmf.interpreter.common.interfaces.ITranslatorComponent;

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

    public static TranslationException buildExceptionObj(
            ITranslatorComponent translator,
            String message,
            TranslationExceptionReason reason,
            Object obj
    ) {
        TranslationException te = new TranslationException(
                translator.getSourceLanguage(),
                translator.getTargetLanguage(),
                message,
                reason
        );
        te.setReasonObj(obj);
        return te;
    }

    public static TranslationException buildException(
            ITranslatorComponent translator,
            String message,
            TranslationExceptionReason reason
    ) {
        return new TranslationException(
                translator.getSourceLanguage(),
                translator.getTargetLanguage(),
                message,
                reason
        );
    }

    public static TranslationException buildException(
            ITranslatorComponent translator,
            String message,
            TranslationExceptionReason reason,
            Throwable throwable
    ) {
        return new TranslationException(
                translator.getSourceLanguage(),
                translator.getTargetLanguage(),
                message,
                reason,
                throwable
        );
    }
}
