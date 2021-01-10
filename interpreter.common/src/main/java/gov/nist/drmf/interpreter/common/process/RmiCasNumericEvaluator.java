package gov.nist.drmf.interpreter.common.process;

import gov.nist.drmf.interpreter.common.eval.NumericalTest;
import gov.nist.drmf.interpreter.common.exceptions.ComputerAlgebraSystemEngineException;
import gov.nist.drmf.interpreter.common.pojo.NumericResult;

import java.rmi.RemoteException;
import java.util.List;
import java.util.Set;

/**
 * @author Andre Greiner-Petter
 */
public interface RmiCasNumericEvaluator extends RmiCasInterface {
    String KEY = "RMI_CAS_NUMERIC_EVALUATOR";

    /**
     * Sets global assumptions that will be applied to all following tests.
     * @param assumptions list of assumptions
     * @throws RemoteException if something went wrong with the remote VM
     */
    default void setGlobalAssumptions(List<String> assumptions) throws RemoteException {
        // ignore by default
    }

    /**
     * Is ignored by default.
     *
     * @param packages the packages to register
     * @throws RemoteException if something went wrong with the remote VM
     */
    default void addRequiredPackages(Set<String> packages) throws RemoteException {
        // ignoring it
    }

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
