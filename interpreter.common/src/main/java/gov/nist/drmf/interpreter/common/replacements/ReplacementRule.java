package gov.nist.drmf.interpreter.common.replacements;

import com.fasterxml.jackson.annotation.JsonSetter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Andre Greiner-Petter
 */
public class ReplacementRule {
    private Pattern pattern = null;
    private String replacement = "";
    private int groups = 0;

    public ReplacementRule(){};

    @JsonSetter("pattern")
    public void setPattern(String pattern) {
        this.pattern = Pattern.compile(pattern);
    }

    @JsonSetter("replacement")
    public void setReplacement(String replacement) {
        this.replacement = replacement == null ? "" : replacement;
    }

    @JsonSetter("groups")
    public void setGroups(int groups) {
        this.groups = groups;
    }

    @Override
    public String toString() {
        return "[Repl-Rule: " + pattern + " --> " + replacement + "]";
    }

    public String replace( String input ) {
        Matcher m = pattern.matcher(input);
        StringBuffer sb = new StringBuffer();
        while ( m.find() ) {
            String replaceStr = replaceByGroupMatch(m);

            // yeah... I know... that's freaking crazy but it's necessary
            replaceStr = replaceStr.replaceAll("\\\\$", "\\\\\\\\");

            m.appendReplacement(sb, replaceStr);
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private String replaceByGroupMatch( Matcher m ) {
        // replace \ by \\
        String repl = replacement;
        for ( int i = 1; i <= this.groups; i++ ) {
            String insert = m.group(i);
            if ( insert.equals("\\") ) {
                repl = repl.replace("$"+i, "\\");
            } else {
                insert = insert.replace("\\", "\\\\");
                repl = repl.replace("$"+i, insert);
            }
        }
        return repl;
    }
}
