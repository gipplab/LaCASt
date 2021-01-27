package gov.nist.drmf.interpreter.common.eval;

public interface ITestResultCounter {

    void increaseNumberOfSuccessfulTests();

    void increaseNumberOfFailedTests();

    void increaseNumberOfErrorTests();

    void increaseNumberOfSkippedTests();

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
