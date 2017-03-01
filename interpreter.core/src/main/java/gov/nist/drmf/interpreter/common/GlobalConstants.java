package gov.nist.drmf.interpreter.common;

import java.util.regex.Pattern;

/**
 * Created by AndreG-P on 28.02.2017.
 */
public class GlobalConstants {

    public static final String ALTERNATIVE_SPLIT = "||";

    public static final String CARET_CHAR = "^";

    public static final String WHITESPACE = " ";

    public static final String LATEX_MULTIPLY = "\\\\cdot|\\*";
    public static final Pattern LATEX_MULTIPLY_PATTERN = Pattern.compile(LATEX_MULTIPLY);

    /**
     * A flag to identify the CAS at runtime. It is used for the
     * translation from DLMF to a given CAS.
     */
    public static String CAS_KEY = "";

    /**
     * A flag to translate not the direct translation but the alternative translation.
     * It is used by the MacroParser (interpreter.cas).
     *
     * It is probably useless because the alternative translation is not fully implemented
     * yet.
     */
    public static boolean ALTERNATIVE_MODE = false;
}
