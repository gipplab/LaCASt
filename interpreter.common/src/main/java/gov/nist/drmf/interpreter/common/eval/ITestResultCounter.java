package gov.nist.drmf.interpreter.common.eval;

import com.fasterxml.jackson.annotation.JsonGetter;

public interface ITestResultCounter {

    boolean wasAborted();

    void increaseNumberOfSuccessfulTests();

    void increaseNumberOfFailedTests();

    void increaseNumberOfErrorTests();

    void increaseNumberOfSkippedTests();

    int getNumberOfCalculationGroups();

    int getNumberOfTotalTests();

    int getNumberOfSuccessfulTests();

    int getNumberOfFailedTests();

    int getNumberOfErrorTests();

    int getNumberOfSkippedTests();

    @JsonGetter("overallResult")
    default TestResultType overallResult() {
        if ( getNumberOfCalculationGroups() == 0 || getNumberOfTotalTests() == 0 ) return TestResultType.SKIPPED;
        if (getNumberOfTotalTests() == getNumberOfSuccessfulTests()) return TestResultType.SUCCESS;
        else if ( getNumberOfErrorTests() == 0 && getNumberOfFailedTests() > 0) return TestResultType.FAILURE;
        else if ( getNumberOfSkippedTests() > 0 ) return TestResultType.SKIPPED;
        else return TestResultType.ERROR;
    }

    default void addTestResult(TestResultType result) {
        switch (result) {
            case SUCCESS:
                increaseNumberOfSuccessfulTests();
                break;
            case FAILURE:
                increaseNumberOfFailedTests();
                break;
            case ERROR:
                increaseNumberOfErrorTests();
                break;
            case SKIPPED:
                increaseNumberOfSkippedTests();
        }
    }

}
