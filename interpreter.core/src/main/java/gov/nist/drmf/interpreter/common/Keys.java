package gov.nist.drmf.interpreter.common;

/**
 * @author Andre Greiner-Petter
 */
public class Keys {
    // this will be setup on runtime
    public static String CAS_KEY = "";

    public static boolean ALTERNATIVE_MODE = false;

    public static final String ALTERNATIVE_SPLIT = "||";

    // Key value for LaTeX
    public static final String KEY_LATEX = "LaTeX";

    // Key value for Maple
    public static final String KEY_MAPLE = "Maple";

    // Key value for Mathematica
    public static final String KEY_MATHEMATICA = "Mathematica";

    // Key value for DLMF
    public static final String KEY_DLMF = "DLMF";

    public static final String KEY_DLMF_MACRO = "dlmf-macro";

    public static final String
            NUM_OF_VARS     = "Number of Variables",
            NUM_OF_ATS      = "Number of Ats",
            NUM_OF_PARAMS   = "Number of Parameters";

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
            FEATURE_VALUE_CONSTANT  = "mathematical constant";
}
