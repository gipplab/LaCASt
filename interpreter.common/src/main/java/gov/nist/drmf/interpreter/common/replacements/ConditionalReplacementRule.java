package gov.nist.drmf.interpreter.common.replacements;

import com.fasterxml.jackson.annotation.JsonSetter;

import java.util.LinkedList;
import java.util.List;

/**
 * @author Andre Greiner-Petter
 */
public abstract class ConditionalReplacementRule extends ReplacementRule {
    private List<IReplacementCondition> conditions = new LinkedList<>();
    private List<IReplacementCondition[]> conditionRanges = new LinkedList<>();

    public ConditionalReplacementRule() {}

    abstract IReplacementCondition generateReplacementConditionalObject(String link);

    @JsonSetter("condition")
    public void setCondition(List<String> cs) {
        for ( String c : cs ) {
            if ( c.contains("-") ) {
                String[] r = c.split("-");
                IReplacementCondition start = generateReplacementConditionalObject(r[0]);
                IReplacementCondition end = generateReplacementConditionalObject(r[1]);
                this.conditionRanges.add( new IReplacementCondition[]{start, end} );
            } else {
                this.conditions.add( generateReplacementConditionalObject(c) );
            }
        }
    }

    public boolean applicable( String link ) {
        // if this is no conditional replacement, its always true
        if ( this.conditions.isEmpty() && this.conditionRanges.isEmpty() ) return true;

        // if it is a conditional replacement, the link must be not null
        if ( link == null ) return false;

        // now lets check if the condition matches
        IReplacementCondition inputLink = generateReplacementConditionalObject(link);
        for ( IReplacementCondition cond : conditions ) {
            if ( cond.match(inputLink) ) return true;
        }

        for ( IReplacementCondition[] range : conditionRanges ) {
            if ( IReplacementCondition.withinRange(range[0], range[1], inputLink) ) return true;
        }

        return false;
    }

    public String saveReplace(String input, String link) {
        if ( applicable(link) ) return replace( input );
        else return input;
    }
}
