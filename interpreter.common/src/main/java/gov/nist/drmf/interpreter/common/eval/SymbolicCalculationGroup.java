package gov.nist.drmf.interpreter.common.eval;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Andre Greiner-Petter
 */
@JsonPropertyOrder({
        "lhs","rhs","testExpression","testCalculations"
})
public class SymbolicCalculationGroup implements Serializable {
    @JsonProperty("lhs")
    private String lhs;

    @JsonProperty("rhs")
    private String rhs;

    @JsonProperty("testExpression")
    private String testExpression;

    @JsonProperty("testCalculations")
    private List<SymbolicCalculation> testCalculations;

    public SymbolicCalculationGroup() {
        this.testCalculations = new LinkedList<>();
    }

    public String getLhs() {
        return lhs;
    }

    public void setLhs(String lhs) {
        this.lhs = lhs;
    }

    public String getRhs() {
        return rhs;
    }

    public void setRhs(String rhs) {
        this.rhs = rhs;
    }

    public String getTestExpression() {
        return testExpression;
    }

    public void setTestExpression(String testExpression) {
        this.testExpression = testExpression;
    }

    public List<SymbolicCalculation> getTestCalculations() {
        return testCalculations;
    }

    public void setTestCalculations(List<SymbolicCalculation> testCalculations) {
        this.testCalculations = testCalculations;
    }

    @JsonIgnore
    public void addTestCalculation(SymbolicCalculation testCalculation) {
        if ( testCalculation != null )
            this.testCalculations.add(testCalculation);
    }

    @JsonIgnore
    public int getSize() {
        return testCalculations.size();
    }
}
