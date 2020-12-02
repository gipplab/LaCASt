package gov.nist.drmf.interpreter.generic.pojo;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Andre Greiner-Petter
 */
public class FormulaDefiniens {
    @JsonProperty("definition")
    private final String definition;

    @JsonProperty("score")
    private final double score;

    private FormulaDefiniens() {
        this(0, "");
    }

    public FormulaDefiniens(double score, String definition) {
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
