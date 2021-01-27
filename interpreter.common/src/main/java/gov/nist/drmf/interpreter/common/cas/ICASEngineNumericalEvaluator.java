package gov.nist.drmf.interpreter.common.cas;

import gov.nist.drmf.interpreter.common.eval.EvaluatorType;
import gov.nist.drmf.interpreter.common.eval.NumericalTest;
import gov.nist.drmf.interpreter.common.exceptions.ComputerAlgebraSystemEngineException;
import gov.nist.drmf.interpreter.common.eval.NumericResult;

import java.util.List;

/**
 * @author Andre Greiner-Petter
 */
public interface ICASEngineNumericalEvaluator extends IAbortEvaluator {
    /**
     * Main method of the CAS engine numeric evaluator
     * @param test the test case
     * @return the result of the test case
     */
    NumericResult performNumericTest(NumericalTest test) throws ComputerAlgebraSystemEngineException;

    /**
     * In Maple its evalf( input );
     * @param expression input
     * @return the numeric test expression (e.g., evalf(input))
     */
    String generateNumericTestExpression(String expression);

    /**
     * Sets global assumptions that will be applied to all following tests.
     * @param assumptions list of assumptions
     */
    default void setGlobalNumericAssumptions(List<String> assumptions) throws ComputerAlgebraSystemEngineException {
        // ignore by default
    }

    default void setTimeout(double timeoutInSeconds) {
        setTimeout(EvaluatorType.NUMERIC, timeoutInSeconds);
    }

    default void disableTimeout() {
        disableTimeout(EvaluatorType.NUMERIC);
    }
}
