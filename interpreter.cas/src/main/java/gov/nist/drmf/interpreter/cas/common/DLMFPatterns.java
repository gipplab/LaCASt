package gov.nist.drmf.interpreter.cas.common;

import java.util.regex.Pattern;

/**
 * @author Andre Greiner-Petter
 */
public class DLMFPatterns {
    public static final String SPACE = " ";

    public static final String OPEN_PARENTHESIS_PATTERN =
            "(left)[-\\s](parenthesis|bracket|brace|delimiter)";

    public static final String CLOSE_PARENTHESIS_PATTERN =
            "(right)[-\\s](parenthesis|bracket|brace|delimiter)";

    public static final String PARENTHESIS_PATTERN =
            "(right|left)[-\\s](parenthesis|bracket|brace|delimiter)";

    public static final String SPECIAL_SYMBOL_PATTERN_FOR_SPACES =
            "[\\^\\/\\_\\!|]";

    public static final String PATTERN_BASIC_OPERATIONS =
            ".*[,;.+\\-*/\\^_!{}\\[\\]<>\\s=|]|\\\\[ci]dot.*";

    public static final String ABSOLUTE_VAL_TERM_TEXT_PATTERN = "\\\\?\\|";

    public static final String CHAR_BACKSLASH = "\\";

    public static final Pattern DLMF_ID_PATTERN = Pattern.compile("-(\\d+)-\\d+-E\\d+.s.tex");
}
