package gov.nist.drmf.interpreter.common.text;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Andre Greiner-Petter
 */
public final class TextUtility {

    private TextUtility() {}

    public static String appendPattern(String in, Pattern pattern, int group) {
        StringBuilder sb = new StringBuilder();
        Matcher m = pattern.matcher(in);
        while ( m.find() ) {
            m.appendReplacement(sb, m.group(group));
        }
        m.appendTail(sb);
        return sb.toString();
    }
}
