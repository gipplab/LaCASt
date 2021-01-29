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
        "overallResult", "numberOfTests", "numberOfFailedTests", "numberOfSuccessfulTests",
        "numberOfSkippedTests", "numberOfErrorTests", "wasAborted", "crashed",
        "testCalculationsGroups"
})
public class NumericResult implements Serializable, ITestResultCounter {
    @JsonProperty("overallResult")
    private TestResultType overallResult;

    @JsonProperty("numberOfTests")
    private int numberOfTotalTests;

    @JsonProperty("numberOfFailedTests")
    private int numberOfFailedTests;

    @JsonProperty("numberOfSuccessfulTests")
    private int numberOfSuccessfulTests;

    @JsonProperty("numberOfErrorTests")
    private int numberOfErrorTests;

    @JsonProperty("numberOfSkippedTests")
    private int numberOfSkippedTests;

    @JsonProperty("testCalculationsGroups")
    private List<NumericCalculationGroup> testCalculationsGroups = new LinkedList<>();

    @JsonProperty("wasAborted")
    private boolean wasAborted = false;

    @JsonProperty("crashed")
    private boolean crashed = false;

    public NumericResult() {
        overallResult = TestResultType.SKIPPED;
    }

    @JsonSetter("overallResult")
    public void setOverallResult(TestResultType overallResult) {
        this.overallResult = overallResult;
    }

    @JsonGetter("crashed")
    public boolean crashed() {
        return crashed;
    }

    public NumericResult markAsCrashed() {
        this.crashed = true;
        return this;
    }

    @JsonGetter("wasAborted")
    public boolean wasAborted() {
        return wasAborted;
    }

    @JsonSetter("wasAborted")
    public void wasAborted(boolean wasAborted) {
        this.wasAborted = wasAborted;
    }

    /**
     * Merges the given results with this object.
     * @param nr another numeric result object
     */
    @JsonIgnore
    public void addFurtherResults(NumericResult nr) {
        this.addTestCalculationsGroup( nr.getTestCalculationsGroups() );
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

    public List<NumericCalculationGroup> getTestCalculationsGroups() {
        return testCalculationsGroups;
    }

    public void setTestCalculations(List<NumericCalculationGroup> testCalculationsGroup) {
        this.testCalculationsGroups = testCalculationsGroup;
    }

    public int getNumberOfErrorTests() {
        return numberOfErrorTests;
    }

    public void setNumberOfErrorTests(int numberOfErrorTests) {
        this.numberOfErrorTests = numberOfErrorTests;
    }

    public int getNumberOfSkippedTests() {
        return numberOfSkippedTests;
    }

    public void setNumberOfSkippedTests(int numberOfSkippedTests) {
        this.numberOfSkippedTests = numberOfSkippedTests;
    }

    @JsonIgnore
    public void addTestCalculationsGroup(NumericCalculationGroup testCalc) {
        for ( NumericCalculation numCalc : testCalc.getTestCalculations() ) {
            addTestResult(numCalc.getResult());
        }
        this.numberOfTotalTests += testCalc.getSize();
        this.testCalculationsGroups.add(testCalc);
    }

    @JsonIgnore
    public void addTestCalculationsGroup(Collection<NumericCalculationGroup> testCals) {
        if ( testCals == null || testCals.isEmpty() ) return;

        for ( NumericCalculationGroup nc : testCals ) {
            addTestCalculationsGroup(nc);
        }
    }

    @JsonIgnore
    @Override
    public void increaseNumberOfSuccessfulTests() {
        numberOfSuccessfulTests++;
    }

    @JsonIgnore
    @Override
    public void increaseNumberOfFailedTests() {
        numberOfFailedTests++;
    }

    @JsonIgnore
    @Override
    public void increaseNumberOfErrorTests() {
        numberOfErrorTests++;
    }

    @JsonIgnore
    @Override
    public void increaseNumberOfSkippedTests() {
        numberOfSkippedTests++;
    }

    @JsonIgnore
    @Override
    public int getNumberOfCalculationGroups() {
        return testCalculationsGroups.size();
    }
}
