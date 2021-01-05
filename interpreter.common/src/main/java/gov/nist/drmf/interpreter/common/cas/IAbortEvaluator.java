package gov.nist.drmf.interpreter.common.cas;

import gov.nist.drmf.interpreter.common.exceptions.ComputerAlgebraSystemEngineException;

/**
 * @author Andre Greiner-Petter
 */
public interface IAbortEvaluator<T> {
    void setTimeout(double timeoutInSeconds);

    default void disableTimeout() {
        setTimeout(-1);
    }

    boolean wasAborted(T result);
}
