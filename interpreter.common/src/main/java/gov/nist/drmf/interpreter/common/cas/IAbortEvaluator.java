package gov.nist.drmf.interpreter.common.cas;

import gov.nist.drmf.interpreter.common.exceptions.ComputerAlgebraSystemEngineException;

/**
 * @author Andre Greiner-Petter
 */
public interface IAbortEvaluator<T> {
    void abort() throws ComputerAlgebraSystemEngineException;

    boolean wasAborted(T result);
}