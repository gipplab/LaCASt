package gov.nist.drmf.interpreter.common.pojo;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Andre Greiner-Petter
 */
public class FormulaDefinition {
    @JsonProperty("definition")
    private final String definition;

    @JsonProperty("score")
    private final double score;

    private FormulaDefinition() {
        this(0, "");
    }

    public FormulaDefinition(double score, String definition) {
        this.score = score;
        this.definition = definition;
    }

    public double getScore() {
        return score;
    }

    public String getDefinition() {
        return definition;
    }
}
