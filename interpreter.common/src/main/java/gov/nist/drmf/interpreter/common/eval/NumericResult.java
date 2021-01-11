package gov.nist.drmf.interpreter.common.eval;

import com.fasterxml.jackson.annotation.*;

import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Andre Greiner-Petter
 */
@JsonPropertyOrder({
        "result", "wasAborted", "numberOfTests", "numberOfFailedTests", "numberOfSuccessfulTests",
        "testCalculations"
})
public class NumericResult implements Serializable {
    @JsonProperty("result")
    private TestResultType testResultType;

    @JsonProperty("numberOfTests")
    private int numberOfTotalTests;

    @JsonProperty("numberOfFailedTests")
    private int numberOfFailedTests;

    @JsonProperty("numberOfSuccessfulTests")
    private int numberOfSuccessfulTests;

    @JsonProperty("wasAborted")
    private boolean wasAborted;

    @JsonProperty("testCalculations")
    private List<NumericCalculation> testCalculations;

    @JsonIgnore
    private boolean crashed = false;

    public NumericResult() {
        testCalculations = new LinkedList<>();
        this.testResultType = TestResultType.FAILURE;
        this.wasAborted = false;
    }

    public NumericResult(TestResultType resultType, int totalTests, int failedTests, int successfulTests, boolean wasAborted) {
        this();
        this.testResultType = resultType;
        this.numberOfTotalTests = totalTests;
        this.numberOfFailedTests = failedTests;
        this.numberOfSuccessfulTests = successfulTests;
        this.wasAborted = wasAborted;
    }

    @JsonIgnore
    public NumericResult markAsCrashed() {
        this.crashed = true;
        return this;
    }

    @JsonIgnore
    public boolean crashed() {
        return crashed;
    }

    /**
     * Merges the given results with this object.
     * @param nr another numeric result object
     */
    @JsonIgnore
    public void addFurtherResults(NumericResult nr) {
        if ( this.numberOfTotalTests == 0 ) {
            this.testResultType = nr.testResultType;
            this.wasAborted = nr.wasAborted;
        }
        else {
            this.testResultType.and(nr.testResultType);
            this.wasAborted |= nr.wasAborted;
        }

        this.numberOfTotalTests += nr.numberOfTotalTests;
        this.numberOfFailedTests += nr.numberOfFailedTests;
        this.numberOfSuccessfulTests += nr.numberOfSuccessfulTests;
        testCalculations.addAll( nr.testCalculations );
    }

    @JsonGetter("result")
    public TestResultType getTestResultType() {
        return testResultType;
    }

    @JsonSetter("result")
    public void setTestResultType(TestResultType testResultType) {
        this.testResultType = testResultType;
    }

    @JsonGetter("wasAborted")
    public Boolean wasAborted() {
        return wasAborted;
    }

    @JsonSetter("wasAborted")
    public void wasAborted(Boolean wasAborted) {
        this.wasAborted = wasAborted;
    }

    @JsonGetter("numberOfTests")
    public int getNumberOfTotalTests() {
        return numberOfTotalTests;
    }

    @JsonSetter("numberOfTests")
    public void setNumberOfTotalTests(int numberOfTotalTests) {
        this.numberOfTotalTests = numberOfTotalTests;
    }

    public int getNumberOfFailedTests() {
        return numberOfFailedTests;
    }

    public void setNumberOfFailedTests(int numberOfFailedTests) {
        this.numberOfFailedTests = numberOfFailedTests;
    }

    public int getNumberOfSuccessfulTests() {
        return numberOfSuccessfulTests;
    }

    public void setNumberOfSuccessfulTests(int numberOfSuccessfulTests) {
        this.numberOfSuccessfulTests = numberOfSuccessfulTests;
    }

    public List<NumericCalculation> getTestCalculations() {
        return testCalculations;
    }

    public void setTestCalculations(List<NumericCalculation> testCalculations) {
        this.testCalculations = testCalculations;
    }

    @JsonIgnore
    public void addTestCalculations(NumericCalculation testCalc) {
        this.testCalculations.add(testCalc);
    }

    @JsonIgnore
    public void addTestCalculations(Collection<NumericCalculation> testCals) {
        this.testCalculations.addAll(testCals);
    }
}
