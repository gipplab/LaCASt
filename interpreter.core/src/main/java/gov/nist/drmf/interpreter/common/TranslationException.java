package gov.nist.drmf.interpreter.common;

/**
 *
 *
 * Created by AndreG-P on 03.03.2017.
 */
public class TranslationException extends RuntimeException {
    public static String FROM_LANGUAGE_DEFAULT;
    public static String TO_LANGUAGE_DEFAULT;

    private Reason reason;

    public TranslationException( String message ){
        this ( FROM_LANGUAGE_DEFAULT, TO_LANGUAGE_DEFAULT, message );
    }

    public TranslationException( String message, Reason reason ){
        this(message);
        this.reason = reason;
    }

    public TranslationException( String message, Reason reason, Throwable throwable ){
        this( FROM_LANGUAGE_DEFAULT, TO_LANGUAGE_DEFAULT, message, throwable);
        this.reason = reason;
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
        MLP_ERROR("CRITICAL! POM-Tagger Error Reached");

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
