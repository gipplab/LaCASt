package gov.nist.drmf.interpreter.common.pojo;

import com.fasterxml.jackson.annotation.*;

import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Andre Greiner-Petter
 */
@JsonPropertyOrder({
        "successful", "wasAborted", "numberOfTests", "numberOfFailedTests", "numberOfSuccessfulTests",
        "testCalculations"
})
public class NumericResult implements Serializable {
    @JsonProperty("successful")
    private boolean successful;

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

    public NumericResult() {
        testCalculations = new LinkedList<>();
        this.successful = false;
        this.wasAborted = false;
    }

    public NumericResult(boolean successful, int totalTests, int failedTests, int successfulTests, boolean wasAborted) {
        this();
        this.successful = totalTests > 0 && successful;
        this.numberOfTotalTests = totalTests;
        this.numberOfFailedTests = failedTests;
        this.numberOfSuccessfulTests = successfulTests;
        this.wasAborted = wasAborted;
    }

    /**
     * Merges the given results with this object.
     * @param nr another numeric result object
     */
    @JsonIgnore
    public void addFurtherResults(NumericResult nr) {
        if ( this.numberOfTotalTests == 0 ) {
            this.successful = nr.successful;
            this.wasAborted = nr.wasAborted;
        }
        else {
            this.successful &= nr.successful; // this new test case is only successful if all parts where successful
            this.wasAborted |= nr.wasAborted;
        }

        this.numberOfTotalTests += nr.numberOfTotalTests;
        this.numberOfFailedTests += nr.numberOfFailedTests;
        this.numberOfSuccessfulTests += nr.numberOfSuccessfulTests;
        testCalculations.addAll( nr.testCalculations );
    }

    public boolean isSuccessful() {
        return successful;
    }

    public void setSuccessful(boolean successful) {
        this.successful = successful;
    }

    @JsonGetter("wasAborted")
    public Boolean getWasAborted() {
        return wasAborted;
    }

    @JsonSetter("wasAborted")
    public void setWasAborted(Boolean wasAborted) {
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
