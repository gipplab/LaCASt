package gov.nist.drmf.interpreter.generic.macro;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.apache.logging.log4j.core.util.Integers;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Andre Greiner-Petter
 */
public class MacroCounter {
    @JsonProperty("macro")
    private final String macro;

    @JsonProperty("macroCounter")
    private int macroCounter;

    @JsonProperty("optionalArgumentCounter")
    private int optionalArgumentCounter;

    @JsonProperty("atCounter")
    private Map<Integer, Integer> atCounter;

    private MacroCounter() {
        this("");
    }

    public MacroCounter(String macro) {
        this.macro = macro;
        this.macroCounter = 0;
        this.optionalArgumentCounter = 0;
        this.atCounter = new HashMap<>();
    }

    @JsonIgnore
    public void incrementMacroCounter() {
        this.macroCounter++;
    }

    @JsonIgnore
    public void incrementOptionalArgumentCounter() {
        this.optionalArgumentCounter++;
    }

    @JsonIgnore
    public void incrementAtCounter(int numberOfAts) {
        this.atCounter.compute( numberOfAts, (numOfAts, counter) -> counter == null ? 1 : counter+1 );
    }

    @JsonSetter("macroCounter")
    public void setMacroCounter(int counter) {
        this.macroCounter = counter;
    }

    @JsonSetter("optionalArgumentCounter")
    public void setOptionalArgumentCounter(int counter) {
        this.optionalArgumentCounter = counter;
    }

    @JsonSetter("atCounter")
    public void setAtCounters(Map<String, Integer> atCounters) {
        atCounters.forEach( (k, v) -> this.atCounter.put(Integers.parseInt(k), v) );
    }

    @JsonGetter("macro")
    public String getMacro() {
        return this.macro;
    }

    @JsonGetter("macroCounter")
    public int getMacroCounter() {
        return this.macroCounter;
    }

    @JsonGetter("optionalArgumentCounter")
    public int getOptionalArgumentCounter() {
        return this.optionalArgumentCounter;
    }

    @JsonGetter("atCounter")
    public Map<Integer, Integer> getAtCounter() {
        return this.atCounter;
    }

    @JsonIgnore
    public int getNumberOfAtsCounter(int numberOfAts) {
        return this.atCounter.getOrDefault(numberOfAts, 0);
    }

    /**
     * Calculates the likelihood of the given combination of arguments for this macro
     * based on the internal counters.
     *
     * For example, a macro appears 5 times in the database
     * but only 2 times with an optional argument. The macro has a maximum of one at symbols
     * so every 5 occasions use exactly one at symbol. For a given {@param numberOfAts} 1 the
     * returned value is 3/5 if {@param optionalArgument} is false and 2/5 if it is true.
     *
     * The score is between 0 and 1. If the given number of ats never occur or the given optional
     * argument switch did not appears the score is obviously 0.
     *
     * @param optionalArgument the number of optional arguments
     * @param numberOfAts the number of ats for the score
     * @return the calculated score
     */
    @JsonIgnore
    public double getScore(boolean optionalArgument, int numberOfAts) {
        // if this number of ats simply never occur, the score 0 right away
        if ( !this.atCounter.containsKey(numberOfAts) ) return 0;

        // first the score based on the optional argument
        double score = optionalArgument ?
                optionalArgumentCounter/(double)macroCounter :
                (macroCounter-optionalArgumentCounter)/(double)macroCounter;

        return score * atCounter.getOrDefault(numberOfAts, 0)/(double)macroCounter;
    }

    @JsonIgnore
    @Override
    public String toString() {
        StringBuilder out = new StringBuilder(macro + ",");
        out.append(macroCounter).append(",");
        out.append(optionalArgumentCounter).append(",");
        for ( Map.Entry<Integer,Integer> entry : this.atCounter.entrySet() ) {
            out.append(entry.getKey()).append(":").append(entry.getValue()).append("/");
        }
        return out.toString();
    }
}
