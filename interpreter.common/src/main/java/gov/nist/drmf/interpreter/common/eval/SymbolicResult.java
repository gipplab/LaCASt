package gov.nist.drmf.interpreter.common.eval;

import com.fasterxml.jackson.annotation.*;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Andre Greiner-Petter
 */
@JsonPropertyOrder({
        "result", "numberOfTests", "testCalculations"
})
public class SymbolicResult implements Serializable {

    @JsonProperty("result")
    private TestResultType testResultType;

    @JsonProperty("numberOfTests")
    private int numberOfTests;

    @JsonProperty("testCalculations")
    private List<SymbolicCalculation> testCalculations;

    @JsonIgnore
    private boolean crashed = false;

    public SymbolicResult() {
        testCalculations = new LinkedList<>();
    }

    public SymbolicResult(TestResultType testResultType) {
        this();
        this.testResultType = testResultType;
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

    @JsonGetter("result")
    public TestResultType getTestResultType() {
        return testResultType;
    }

    @JsonSetter("result")
    public void setTestResultType(TestResultType testResultType) {
        this.testResultType = testResultType;
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

    @JsonIgnore
    public String printCalculations() {
        List<String> results = testCalculations.stream().map( SymbolicCalculation::getResult ).collect(Collectors.toList());
        return "[" + String.join(", ", results) + "]";
    }
}
