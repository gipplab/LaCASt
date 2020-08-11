package gov.nist.drmf.interpreter.cas.common;

import org.intellij.lang.annotations.Language;

import java.util.regex.Pattern;

/**
 * @author Andre Greiner-Petter
 */
public class DLMFPatterns {
    public static final String SPACE = " ";

    @Language("RegExp")
    public static final String SPECIAL_SYMBOL_PATTERN_FOR_SPACES =
            "[\\^/_!|]";

    @Language("RegExp")
    public static final String PATTERN_BASIC_OPERATIONS =
            ".*[,;.+\\-*/^_!{}\\[\\]<>\\s=]|\\\\[ci]dot.*";

    @Language("RegExp")
    public static final String STRING_END_TREAT_AS_CLOSED_PARANTHESIS =
            ".*\\s*[)\\]}!]\\s*";

    @Language("RegExp")
    public static final String DERIV_NOTATION = "\\\\(?:[tip]|tp|ip)?deriv";

    public static final Pattern ENDS_ON_STAR_PATTERN = Pattern.compile("(.*)\\*\\s*$");

    public static final String CHAR_BACKSLASH = "\\";

    public static final Pattern DLMF_ID_PATTERN = Pattern.compile("-(\\d+)-\\d+-E\\d+.s.tex");

    public static final String TEMPORARY_VARIABLE_NAME = "temp";
}
