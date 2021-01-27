package gov.nist.drmf.interpreter.common.process;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RmiShutdowner extends Remote {
    default double plus(double a, double b) throws RemoteException {
        System.exit(23);
        return a+b;
    }

    void stop() throws RemoteException;
}
