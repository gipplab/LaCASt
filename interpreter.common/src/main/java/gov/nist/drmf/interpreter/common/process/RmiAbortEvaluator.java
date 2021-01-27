package gov.nist.drmf.interpreter.common.process;

import gov.nist.drmf.interpreter.common.eval.EvaluatorType;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RmiAbortEvaluator extends Remote {
    void setTimeout(EvaluatorType type, double timeoutInSeconds) throws RemoteException;

    void disableTimeout(EvaluatorType type) throws RemoteException;
}
