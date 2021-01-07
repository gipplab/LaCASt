package gov.nist.drmf.interpreter.common.eval;

import gov.nist.drmf.interpreter.common.cas.ICASEngineNumericalEvaluator;
import gov.nist.drmf.interpreter.common.exceptions.ComputerAlgebraSystemEngineException;

import java.util.Set;

/**
 * @author Andre Greiner-Petter
 */
public interface INumericTestCalculator<T> {

    ICASEngineNumericalEvaluator<T> getNumericEvaluator();

    Set<String> getRequiredPackages();

    /**
     * Performs the numerical test. Calls {@link #getNumericEvaluator()}
     * to get the evaluator implementation. It also calls {@link #getRequiredPackages()}
     * in case this test must load additional packages before performing the calculations.
     * @param test the test case
     * @return the result
     * @throws ComputerAlgebraSystemEngineException if an error was thrown
     */
    default T performNumericalTest(NumericalTest test)
            throws ComputerAlgebraSystemEngineException {
        ICASEngineNumericalEvaluator<T> numericalEvaluator = getNumericEvaluator();
        if ( numericalEvaluator.requiresRegisteredPackages() ) {
            numericalEvaluator.addRequiredPackages(getRequiredPackages());
        }
        return numericalEvaluator.performNumericalTest(test);
    }

    /**
     * Returns true if the given result was aborted, otherwise false.
     * @param result the result of {@link #performNumericalTest(NumericalTest)}
     * @return true if the result was aborted
     */
    default boolean isAbortedResult(T result) {
        return getNumericEvaluator().wasAborted(result);
    }

    /**
     * Returns the result type of the given test. Either it was successful, failed or it threw an error.
     * @param results the results from {@link #performNumericalTest(NumericalTest)}
     * @return the result type, successful, failed or error
     * @throws ComputerAlgebraSystemEngineException if the given result cannot be analyzed properly
     */
    default TestResultType testResult(T results) throws ComputerAlgebraSystemEngineException {
        return getNumericEvaluator().getStatusOfResult(results);
    }
}
