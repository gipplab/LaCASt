package gov.nist.drmf.interpreter.common.process;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RmiServer extends Remote {
    String KEY = "RMI_SERVER";

    /**
     * Stops the JVM properly
     */
    void stop() throws RemoteException;
}
