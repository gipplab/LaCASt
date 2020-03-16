package gov.nist.drmf.interpreter.common.constants;

/**
 * @author Andre Greiner-Petter
 */
public final class Keys {
    private Keys() {}

    // Key value for LaTeX
    public static final String KEY_LATEX = "LaTeX";

    // Key value for Maple
    public static final String KEY_MAPLE = "Maple";

    public static final String KEY_LINK_SUFFIX = "-Link";

    public static final String KEY_COMMENT_SUFFIX = "-Comment";

    public static final String KEY_ALTERNATIVE_SUFFX = "-Alternatives";

    public static final String KEY_EXTRA_PACKAGE_SUFFIX = "-Package";

    public static final String KEY_MAPLE_BIN = "maple_bin";

    public static final String KEY_SYSTEM_LOGGING = "log4j2.configurationFile";

    // Key value for Mathematica
    public static final String KEY_MATHEMATICA = "Mathematica";

    public static final String KEY_MATHEMATICA_MATH_DIR = "mathematica_math";

    // Key value for DLMF
    public static final String KEY_DLMF = "DLMF";

    public static final String KEY_DLMF_MACRO = "dlmf-macro";

    public static final String KEY_DLMF_MACRO_OPTIONAL_PREFIX = "dlmf-alternative-";

    public static final String KEY_ABSOLUTE_VALUE = "absolute value";

    public static final String
            NUM_OF_VARS     = "Number of Variables",
            NUM_OF_ATS      = "Number of Ats",
            NUM_OF_PARAMS   = "Number of Parameters",
            SLOT_OF_DIFF    = "Slot of Differentiation";

    public static final String
            FEATURE_SET_AT      = "at",
            FEATURE_AREAS       = "Areas",
            FEATURE_ALPHABET    = "Alphabet",
            FEATURE_ROLE        = "Role",
            FEATURE_MEANINGS    = "Meanings",
            FEATURE_DESCRIPTION = "Description",
            FEATURE_CONSTRAINTS = "Constraints",
            FEATURE_BRANCH_CUTS = "Branch Cuts";

    public static final String
            FEATURE_VALUE_GREEK     = "Greek",
            FEATURE_VALUE_SYMBOL    = "symbol",
            FEATURE_VALUE_FUNCTION  = "function",
            FEATURE_VALUE_CONSTANT  = "mathematical constant",
            FEATURE_VALUE_IGNORE    = "ignore";

    public static final String
            MLP_KEY_MULTIPLICATION  = "General Multiplication",
            MLP_KEY_ADDITION        = "Addition",
            MLP_KEY_FRACTION        = "fraction",
            MLP_KEY_UNDERSCORE      = "underscore";

    public static final String
            MLP_KEY_EQ  = "equals",
            MLP_KEY_NEQ = "relation_neq",
            MLP_KEY_LEQ = "relation_leq",
            MLP_KEY_GEQ = "relation_geq";

    public static final String ABORTION_SIGNAL = "ABORT";
}
