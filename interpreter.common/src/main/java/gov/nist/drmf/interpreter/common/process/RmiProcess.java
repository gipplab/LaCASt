package gov.nist.drmf.interpreter.common.process;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

/**
 * @author Andre Greiner-Petter
 */
public class RmiProcess implements RmiShutdowner {
    private static final Logger LOG = LogManager.getLogger(RmiProcess.class.getName());

    private final Registry registry;

    public RmiProcess(Registry registry) {
        this.registry = registry;
    }

    @Override
    public void stop() throws RemoteException {
        try {
            this.registry.unbind(getId());
            UnicastRemoteObject.unexportObject(this, true);
            LOG.info("Successful shutdown.");
            new Thread(() -> {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    // ignore it
                }
                System.exit(0);
            }).start();
        } catch (NotBoundException e) {
            LOG.error("Unable to shutdown RMI service", e);
        }
    }

    public static String getId() {
        return "RMI-SHUTDOWNER";
    }

    public static void main(String[] args) throws RemoteException, InterruptedException {
        LOG.info("Setup rmi server");
        Registry registry = LocateRegistry.createRegistry(1099); // default port

        RmiProcess rmiProcess = new RmiProcess(registry);
        RmiShutdowner shutdowner = (RmiShutdowner) UnicastRemoteObject.exportObject(rmiProcess, 0);
        registry.rebind(RmiProcess.getId(), shutdowner);
        LOG.info("Finished setup. Inform super process.");
        Thread.sleep(500);
        System.out.println(RmiProcessHandler.READY_SIGNAL);
    }

}
