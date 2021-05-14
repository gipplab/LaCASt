package gov.nist.drmf.interpreter.common.cas;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Andre Greiner-Petter
 */
public class CASLinkBuilderMaple {
    private static final Pattern FUNC_PATTERN = Pattern.compile("^([a-zA-Z]+)\\(.*");

    private CASLinkBuilderMaple() {}

    public static String build(String functionName) {
        if ( functionName == null || functionName.isBlank() ) return "";
        return "https://en.maplesoft.com/support/help/Maple/view.aspx?path=" + functionName;
    }

    public static String extractFunctionNameFromPattern(String pattern) {
        Matcher m = FUNC_PATTERN.matcher(pattern);
        if ( m.matches() ) return m.group(1);
        else return null;
    }
}
