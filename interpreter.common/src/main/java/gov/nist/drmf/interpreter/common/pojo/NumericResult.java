package gov.nist.drmf.interpreter.common.pojo;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Andre Greiner-Petter
 */
public class NumericResult {
    @JsonProperty("successful")
    private boolean successful;

    @JsonProperty("numberOfTests")
    private int numberOfTotalTests;

    @JsonProperty("numberOfFailedTests")
    private int numberOfFailedTests;

    @JsonProperty("numberOfSuccessfulTests")
    private int numberOfSuccessfulTests;

    @JsonProperty("testCalculations")
    private List<NumericCalculation> testCalculations;

    public NumericResult() {
        testCalculations = new LinkedList<>();
    }

    public NumericResult(boolean successful, int totalTests, int failedTests, int successfulTests) {
        this();
        this.successful = successful;
        this.numberOfTotalTests = totalTests;
        this.numberOfFailedTests = failedTests;
        this.numberOfSuccessfulTests = successfulTests;
    }

    /**
     * Merges the given results with this object.
     * @param nr another numeric result object
     */
    @JsonIgnore
    public void addFurtherResults(NumericResult nr) {
        if ( this.numberOfTotalTests == 0 ) this.successful = nr.successful;
        else this.successful &= nr.successful; // this new test case is only successful if all parts where successful

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
