package gov.nist.drmf.interpreter.common.process;

import gov.nist.drmf.interpreter.common.eval.NumericalTest;
import gov.nist.drmf.interpreter.common.exceptions.ComputerAlgebraSystemEngineException;
import gov.nist.drmf.interpreter.common.eval.NumericResult;

import java.rmi.RemoteException;
import java.util.List;

/**
 * @author Andre Greiner-Petter
 */
public interface RmiCasNumericEvaluator extends RmiCasInterface, RmiAbortEvaluator {
    String KEY = "RMI_CAS_NUMERIC_EVALUATOR";

    /**
     * Sets global assumptions that will be applied to all following tests.
     * @param assumptions list of assumptions
     * @throws RemoteException if something went wrong with the remote VM
     */
    void setGlobalNumericAssumptions(List<String> assumptions) throws RemoteException, ComputerAlgebraSystemEngineException;

    /**
     * Performs a single numerical test and returns the result
     * @param test test case
     * @return the test result
     * @throws ComputerAlgebraSystemEngineException if something went wrong inside the CAS
     * @throws RemoteException if something went wrong with the remote VM
     */
    NumericResult performNumericalTest(NumericalTest test)
            throws ComputerAlgebraSystemEngineException, RemoteException;
}
