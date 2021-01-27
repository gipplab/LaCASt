package gov.nist.drmf.interpreter.common.cas;

import gov.nist.drmf.interpreter.common.eval.EvaluatorType;

/**
 * @author Andre Greiner-Petter
 */
public interface IAbortEvaluator {
    /**
     * Sets the timeout for the upcoming computation
     * @param timeoutInSeconds sets the timeout in seconds
     */
    void setTimeout(EvaluatorType type, double timeoutInSeconds);

    /**
     * Disables the timeout. The computation may run forever
     */
    default void disableTimeout(EvaluatorType type) {
        setTimeout(type, -1);
    }
}
