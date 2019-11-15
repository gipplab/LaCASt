package gov.nist.drmf.interpreter.common.exceptions;

/**
 * @author Andre Greiner-Petter
 */
public enum TranslationExceptionReason {
    INVALID_LATEX_INPUT("The input LaTeX is invalid"),
    DLMF_MACRO_ERROR("Error while translating DLMF/DRMF Macro"),
    MISSING_TRANSLATION_INFORMATION("No translation possible for given token"),
    LATEX_MACRO_ERROR("Unknown LaTeX Command"),
    UNKNOWN_OR_MISSING_ELEMENT("An unknown or missing element occurred"),
    WRONG_PARENTHESIS("Parenthesis mismatch in expression"),
    MLP_ERROR("CRITICAL! POM-Tagger Error Reached"),
    IMPLEMENTATION_ERROR("The current implementation is wrong"),

    MAPLE_TRANSLATION_ERROR("An error in Maple occurred"),

//    NULL_ARGUMENT("Empty argument in DLMF/DRMF Macro"),
//    UNKNOWN_MACRO("Unknown DLMF/DRMF Macro"),
//    UNKNOWN_EXPRESSION_TAG("Unknown Expression Tag"),
//    UNKNOWN_MATHTERM_TAG("Unknown MathTerm Tag"),
//    UNKNOWN_SYMBOL("Unknown Symbol"),
//    UNKNOWN_OPERATION("Unknown Operation"),
//    UNKNOWN_MATH_CONSTANT("Unknown Mathematical Constant"),
//    UNKNOWN_GREEK_LETTER("Unknown Greek Letter"),
//
//    UNKNOWN_PARSE_TREE_STRUCTURE("The MLP parse tree has an unknown structure"),
//
//    ILLEGAL_EXTRA_INFO("Invalid additional information attached"),
//    ABBREVIATION("Abbreviations in Expression"),
//    PARSING_ERROR("Cannot parse the given input"),

    NULL("Unknown Error");

    private String name;

    TranslationExceptionReason(String name){
        this.name = name;
    }

    @Override
    public String toString(){
        return name;
    }
}
