package gov.nist.drmf.interpreter.common.process;

import gov.nist.drmf.interpreter.common.eval.SymbolicalTest;
import gov.nist.drmf.interpreter.common.exceptions.ComputerAlgebraSystemEngineException;
import gov.nist.drmf.interpreter.common.eval.SymbolicResult;

import java.rmi.RemoteException;
import java.util.List;

public interface RmiCasSymbolicEvaluator extends RmiCasInterface, RmiAbortEvaluator {
    String KEY = "RMI_CAS_SYMBOLIC_EVALUATOR";

    /**
     * Sets global assumptions that will be applied to all following tests.
     * @param assumptions list of assumptions
     * @throws RemoteException if something went wrong with the remote VM
     */
    void setGlobalSymbolicAssumptions(List<String> assumptions) throws RemoteException, ComputerAlgebraSystemEngineException;

    /**
     * Performs a single symbolic test and returns the result
     * @param test the test to perform
     * @return the test result
     * @throws RemoteException if something critical went wrong with the CAS VM
     */
    SymbolicResult performSymbolicTest(SymbolicalTest test)
            throws RemoteException;
}
