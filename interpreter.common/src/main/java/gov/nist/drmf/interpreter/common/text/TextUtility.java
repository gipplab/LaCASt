package gov.nist.drmf.interpreter.common.text;

import java.util.LinkedList;
import java.util.List;
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

    public static List<String> splitAndNormalizeCommands(String in) {
        if ( in == null ) return new LinkedList<>();

        String[] elements = in.split(",");
        List<String> set = new LinkedList<>();
        for ( int i = 0; i < elements.length; i++ ) {
            elements[i] = elements[i].trim();
            elements[i] = elements[i].startsWith("\\") ? elements[i].substring(1) : elements[i];
            set.add(elements[i]);
        }
        return set;
    }
}
