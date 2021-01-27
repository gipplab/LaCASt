package gov.nist.drmf.interpreter.common.process;

import gov.nist.drmf.interpreter.common.exceptions.ComputerAlgebraSystemEngineException;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RmiCasInterface extends Remote {
    String KEY = "RMI_CAS_INTERFACE";

    /**
     * Enters the given command in CAS. Unfortunately, since the CAS is remotely in another JVM,
     * we cannot allow to get the result from the given input. The result can only be given by
     * a string representation.
     * @param command the CAS command
     * @throws ComputerAlgebraSystemEngineException if the CAS cannot perform the command
     * @throws RemoteException if the connection is lost with the CAS JVM
     */
    String enterCommand(String command) throws ComputerAlgebraSystemEngineException, RemoteException;

    /**
     * Forces to clean GC of CAS
     * @throws ComputerAlgebraSystemEngineException if the command went wrong
     * @throws RemoteException if the connection is lost with the CAS JVM
     */
    void forceGC() throws ComputerAlgebraSystemEngineException, RemoteException;
}
