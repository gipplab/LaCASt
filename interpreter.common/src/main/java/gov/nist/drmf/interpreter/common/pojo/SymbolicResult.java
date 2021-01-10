package gov.nist.drmf.interpreter.common.pojo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.LinkedList;
import java.util.List;

/**
 * @author Andre Greiner-Petter
 */
public class SymbolicResult {

    @JsonProperty("successful")
    private boolean successful;

    @JsonProperty("numberOfTests")
    private int numberOfTests;

    @JsonProperty("testCalculations")
    private List<SymbolicCalculation> testCalculations;

    @JsonIgnore
    private boolean crashed = false;

    public SymbolicResult() {
        testCalculations = new LinkedList<>();
    }

    public SymbolicResult(boolean successful) {
        this();
        this.successful = successful;
    }

    @JsonIgnore
    public SymbolicResult markAsCrashed() {
        this.crashed = true;
        return this;
    }

    @JsonIgnore
    public boolean crashed() {
        return this.crashed;
    }

    public boolean isSuccessful() {
        return successful;
    }

    public void setSuccessful(boolean successful) {
        this.successful = successful;
    }

    public int getNumberOfTests() {
        return numberOfTests;
    }

    public void setNumberOfTests(int numberOfTests) {
        this.numberOfTests = numberOfTests;
    }

    public List<SymbolicCalculation> getTestCalculations() {
        return testCalculations;
    }

    public void setTestCalculations(List<SymbolicCalculation> testCalculations) {
        this.testCalculations = testCalculations;
    }
}
