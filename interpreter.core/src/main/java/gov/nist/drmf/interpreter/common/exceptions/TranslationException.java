package gov.nist.drmf.interpreter.common.exceptions;

/**
 *
 *
 * Created by AndreG-P on 03.03.2017.
 */
public class TranslationException extends RuntimeException {
    private String FROM_LANGUAGE_DEFAULT;
    private static String TO_LANGUAGE_DEFAULT;

    private Reason reason;
    private Object reason_Obj;

    public TranslationException( String message ){
        this ( "ukn", "ukn", message );
    }

    public TranslationException( String message, Reason reason ){
        this(message);
        this.reason = reason;
    }

    public TranslationException( String message, Reason reason, Object reason_Obj ){
        this(message);
        this.reason = reason;
        this.reason_Obj = reason_Obj;
    }

    public TranslationException( String message, Reason reason, Throwable throwable ){
        this( "ukn", "ukn", message, throwable);
        this.reason = reason;
    }

    public TranslationException( String message, Reason reason, Object reasonObj, Throwable throwable ){
        this( message, reason, throwable);
        this.reason_Obj = reasonObj;
    }

    public TranslationException( String from_language, String to_language, String message, Reason reason, Object reasonObj, Throwable throwable ){
        this( "(" + from_language + " -> " + to_language + ") " + message, reason, throwable);
        this.reason_Obj = reasonObj;
    }

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

    public Reason getReason(){
        return reason;
    }

    public void setReason(Reason reason){
        this.reason = reason;
    }

    public Object getReasonObj(){
        return reason_Obj;
    }

    public void setReason_Obj( Object reasonObj ){
        this.reason_Obj = reasonObj;
    }

    @Override
    public String toString(){
        return getLocalizedMessage();
    }

    public enum Reason{
        UNKNOWN_MACRO("Unknown DLMF/DRMF Macro"),
        UNKNOWN_EXPRESSION_TAG("Unknown Expression Tag"),
        UNKNOWN_MATHTERM_TAG("Unknown MathTerm Tag"),
        UNKNOWN_LATEX_COMMAND("Unknown LaTeX Command"),
        UNKNOWN_SYMBOL("Unknown Symbol"),
        UNKNOWN_OPERATION("Unknown Operation"),
        UNKNOWN_MATH_CONSTANT("Unknown Mathematical Constant"),
        UNKNOWN_GREEK_LETTER("Unknown Greek Letter"),
        WRONG_PARENTHESIS("Parenthesis Mismatch in Expression"),
        ABBREVIATION("Abbreviations in Expression"),
        NULL("NULL"),
        DLMF_MACRO_ERROR("Error while translating DLMF/DRMF Macro"),
        MLP_ERROR("CRITICAL! POM-Tagger Error Reached"),
        NULL_ARGUMENT("Empty argument in DLMF/DRMF Macro");

        private String name;

        Reason(String name){
            this.name = name;
        }

        @Override
        public String toString(){
            return name;
        }
    }
}
