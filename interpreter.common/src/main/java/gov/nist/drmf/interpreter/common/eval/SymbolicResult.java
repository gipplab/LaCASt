package gov.nist.drmf.interpreter.common.eval;

import com.fasterxml.jackson.annotation.*;

import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Andre Greiner-Petter
 */
@JsonPropertyOrder({
        "overallResult", "numberOfTests", "numberOfFailedTests", "numberOfSuccessfulTests",
        "numberOfSkippedTests", "numberOfErrorTests", "wasAborted", "crashed",
        "testCalculationsGroups"
})
public class SymbolicResult implements Serializable, ITestResultCounter {
    @JsonProperty("overallResult")
    private TestResultType overallResult;

    @JsonProperty("numberOfTests")
    private int numberOfTotalTests = 0;

    @JsonProperty("numberOfFailedTests")
    private int numberOfFailedTests = 0;

    @JsonProperty("numberOfSuccessfulTests")
    private int numberOfSuccessfulTests = 0;

    @JsonProperty("numberOfErrorTests")
    private int numberOfErrorTests = 0;

    @JsonProperty("numberOfSkippedTests")
    private int numberOfSkippedTests = 0;

    @JsonProperty("testCalculationsGroup")
    private List<SymbolicCalculationGroup> testCalculationsGroups = new LinkedList<>();

    @JsonProperty("crashed")
    private boolean crashed = false;

    public SymbolicResult() {
        overallResult = TestResultType.SKIPPED;
    }

    @JsonSetter("overallResult")
    public void setOverallResult(TestResultType overallResult) {
        this.overallResult = overallResult;
    }

    @JsonIgnore
    public SymbolicResult markAsCrashed() {
        this.crashed = true;
        return this;
    }

    @JsonGetter("crashed")
    public boolean crashed() {
        return this.crashed;
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

    public List<SymbolicCalculationGroup> getTestCalculationsGroups() {
        return testCalculationsGroups;
    }

    public void setTestCalculations(List<SymbolicCalculationGroup> testCalculationsGroup) {
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
    public void addTestCalculationsGroup(SymbolicCalculationGroup testCalc) {
        for ( SymbolicCalculation symCalc : testCalc.getTestCalculations() ) {
            addTestResult(symCalc.getResult());
        }
        this.numberOfTotalTests += testCalc.getSize();
        this.testCalculationsGroups.add(testCalc);
    }

    @JsonIgnore
    public void addTestCalculationsGroup(Collection<SymbolicCalculationGroup> testCals) {
        if ( testCals == null || testCals.isEmpty() ) return;

        for ( SymbolicCalculationGroup nc : testCals ) {
            addTestCalculationsGroup(nc);
        }
    }

    @JsonGetter("overallResult")
    public TestResultType overallResult() {
        if ( testCalculationsGroups.isEmpty() ) return TestResultType.SKIPPED;
        if (numberOfTotalTests == numberOfSuccessfulTests) return TestResultType.SUCCESS;
        else if ( numberOfErrorTests == 0 && numberOfFailedTests > 0) return TestResultType.FAILURE;
        else if ( numberOfSkippedTests > 0 ) return TestResultType.SKIPPED;
        else return TestResultType.ERROR;
    }

    @JsonIgnore
    public String printCalculations() {
        List<String> results = testCalculationsGroups.stream()
                .map( SymbolicCalculationGroup::getTestCalculations )
                .flatMap(Collection::stream)
                .map( SymbolicCalculation::getResultExpression )
                .collect(Collectors.toList());
        return "[" + String.join(", ", results) + "]";
    }

    @JsonIgnore
    public List<SymbolicCalculation> getAllCalculations() {
        return testCalculationsGroups.stream()
                .map( SymbolicCalculationGroup::getTestCalculations )
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
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
}
