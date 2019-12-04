package gov.nist.drmf.interpreter.common.replacements;

import com.fasterxml.jackson.annotation.JsonSetter;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Andre Greiner-Petter
 */
public class DLMFReplacementRule {
    private Pattern pattern = null;
    private String replacement = "";
    private int groups = 0;

    private List<DLMFReplacementCondition> conditions = new LinkedList<>();
    private List<DLMFReplacementCondition[]> conditionRanges = new LinkedList<>();

    public DLMFReplacementRule() {}

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

    @JsonSetter("condition")
    public void setCondition(List<String> cs) {
        for ( String c : cs ) {
            if ( c.contains("-") ) {
                String[] r = c.split("-");
                DLMFReplacementCondition start = new DLMFReplacementCondition(r[0]);
                DLMFReplacementCondition end = new DLMFReplacementCondition(r[1]);
                this.conditionRanges.add( new DLMFReplacementCondition[]{start, end} );
            } else {
                this.conditions.add( new DLMFReplacementCondition(c) );
            }
        }
    }

    public boolean applicable( String link ) {
        // if this is no conditional replacement, its always true
        if ( this.conditions.isEmpty() && this.conditionRanges.isEmpty() ) return true;

        // if it is a conditional replacement, the link must be not null
        if ( link == null ) return false;

        // now lets check if the condition matches
        DLMFReplacementCondition inputLink = new DLMFReplacementCondition(link);
        for ( DLMFReplacementCondition cond : conditions ) {
            if ( cond.match(inputLink) ) return true;
        }

        for ( DLMFReplacementCondition[] range : conditionRanges ) {
            if ( IReplacementCondition.withinRange(range[0], range[1], inputLink) ) return true;
        }

        return false;
    }

    public String saveReplace(String input, String link) {
        if ( applicable(link) ) return replace( input );
        else return input;
    }

    public String replace( String input ) {
        Matcher m = pattern.matcher(input);
        StringBuffer sb = new StringBuffer();
        while ( m.find() ) {
            String replaceStr = replaceByGroupMatch(m);
            m.appendReplacement(sb, replaceStr);
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private String replaceByGroupMatch( Matcher m ) {
        // replace \ by \\
        String repl = replacement;
        for ( int i = 1; i <= this.groups; i++ ) {
            repl = repl.replaceAll("\\$"+i, m.group(i));
        }
        return repl;
    }
}
