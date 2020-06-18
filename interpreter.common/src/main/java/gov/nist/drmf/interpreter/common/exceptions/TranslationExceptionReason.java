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
    INSTANTIATION_ERROR("Unable to load a translation component"),
    MAPLE_TRANSLATION_ERROR("An error in Maple occurred"),
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
