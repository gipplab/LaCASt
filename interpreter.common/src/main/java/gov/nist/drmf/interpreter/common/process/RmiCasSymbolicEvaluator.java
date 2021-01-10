package gov.nist.drmf.interpreter.common.process;

import gov.nist.drmf.interpreter.common.eval.SymbolicalTest;
import gov.nist.drmf.interpreter.common.exceptions.ComputerAlgebraSystemEngineException;
import gov.nist.drmf.interpreter.common.pojo.SymbolicResult;

import java.rmi.RemoteException;

public interface RmiCasSymbolicEvaluator extends RmiCasInterface {
    String KEY = "RMI_CAS_SYMBOLIC_EVALUATOR";

    /**
     * Performs a single symbolic test and returns the result
     * @param test the test to perform
     * @return the test result
     * @throws ComputerAlgebraSystemEngineException if something went wrong during computation
     * @throws RemoteException if something critical went wrong with the CAS VM
     */
    SymbolicResult symbolicTest(SymbolicalTest test)
            throws ComputerAlgebraSystemEngineException, RemoteException;
}
