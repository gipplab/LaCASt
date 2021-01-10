package gov.nist.drmf.interpreter.common.process;

import java.rmi.RemoteException;

public interface RmiCasServer extends RmiCasNumericEvaluator, RmiCasSymbolicEvaluator, RmiServer{
    String KEY = "RMI_CAS_SERVER_";

    String getId() throws RemoteException;

    /**
     * Any CAS must be implemented in lazy initialization.
     * I have no clue why, but Maple automatically runs into SIGSEGV if
     * the engine is setup prior to its initial RMI call.
     *
     * Hence, we must init Maple via RMI. This is the only option it works.
     *
     * @throws Exception
     * @throws RemoteException
     */
    void init() throws Exception, RemoteException;
}
