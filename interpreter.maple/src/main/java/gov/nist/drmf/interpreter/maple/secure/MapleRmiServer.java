package gov.nist.drmf.interpreter.maple.secure;

import gov.nist.drmf.interpreter.common.constants.Keys;
import gov.nist.drmf.interpreter.common.eval.*;
import gov.nist.drmf.interpreter.common.exceptions.CASUnavailableException;
import gov.nist.drmf.interpreter.common.exceptions.ComputerAlgebraSystemEngineException;
import gov.nist.drmf.interpreter.common.process.RmiCasServer;
import gov.nist.drmf.interpreter.common.process.RmiProcessHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;

/**
 * @author Andre Greiner-Petter
 */
public class MapleRmiServer implements RmiCasServer {
    private static final Logger LOG = LogManager.getLogger(MapleRmiServer.class.getName());

    private static Registry registry;

    static {
        try {
            registry = LocateRegistry.createRegistry(1099);
        } catch (RemoteException e) {
            LOG.fatal("Unable to setup RMI LocateRegistry", e);
            System.exit(1);
        }
    }

    private InternalRmiMapleConnector mapleConnector;

    /**
     * I have no clue why... but Maple must be lazy initialized.
     */
    public MapleRmiServer() {}

    @Override
    public void init() throws ComputerAlgebraSystemEngineException, CASUnavailableException {
        if ( this.mapleConnector != null ) return;
        this.mapleConnector = new InternalRmiMapleConnector();
        this.mapleConnector.loadNumericProcedures();
        if ( !this.mapleConnector.isCASAvailable() ) {
            throw new CASUnavailableException();
        }
        LOG.info("Successfully initiated RMI Maple CAS");
    }

    @Override
    public String getId() {
        return Keys.KEY_MAPLE;
    }

    @Override
    public void setTimeout(EvaluatorType type, double timeoutInSeconds) {
        if ( EvaluatorType.NUMERIC.equals(type) ) {
            this.mapleConnector.getNumericEvaluator().setTimeout(timeoutInSeconds);
        } else {
            this.mapleConnector.getSymbolicEvaluator().setTimeout(timeoutInSeconds);
        }
    }

    @Override
    public void disableTimeout(EvaluatorType type) {
        if ( EvaluatorType.NUMERIC.equals(type) ) {
            this.mapleConnector.getNumericEvaluator().disableTimeout();
        } else {
            this.mapleConnector.getSymbolicEvaluator().disableTimeout();
        }
    }

    @Override
    public String enterCommand(String command) throws ComputerAlgebraSystemEngineException {
        return this.mapleConnector.getCASEngine().enterCommand(command);
    }

    @Override
    public void setGlobalNumericAssumptions(List<String> assumptions) throws ComputerAlgebraSystemEngineException {
        this.mapleConnector.getNumericEvaluator().setGlobalNumericAssumptions(assumptions);
    }

    @Override
    public void setGlobalSymbolicAssumptions(List<String> assumptions) throws ComputerAlgebraSystemEngineException {
        this.mapleConnector.getSymbolicEvaluator().setGlobalSymbolicAssumptions(assumptions);
    }

    @Override
    public NumericResult performNumericalTest(NumericalTest test) throws ComputerAlgebraSystemEngineException {
        return this.mapleConnector.getNumericEvaluator().performNumericTest(test);
    }

    @Override
    public SymbolicResult performSymbolicTest(SymbolicalTest test) {
        return this.mapleConnector.getSymbolicEvaluator().performSymbolicTest(test);
    }

    @Override
    public void forceGC() throws ComputerAlgebraSystemEngineException {
        this.mapleConnector.getCASEngine().forceGC();
    }

    @Override
    public void stop() throws RemoteException {
        try {
            LOG.info("Received shutdown signal over RMI. Unbind RMI and gracefully shutdown.");
            registry.unbind(RmiCasServer.KEY + getId());
            UnicastRemoteObject.unexportObject(this, true);

            new Thread(() -> {
                try { Thread.sleep(500); }
                catch ( InterruptedException ie ) { /* we dont care */ }
                System.exit(0);
            }).start();
        } catch (NotBoundException e) {
            LOG.error("Unable to unbind keys.", e);
        }
    }

    public static void main(String[] args) throws CASUnavailableException, RemoteException {
        LOG.info("Start Maple JVM");
        MapleRmiServer mapleServer = new MapleRmiServer();
        LOG.info("Successfully started Maple JVM");

        RmiCasServer exportedServer = (RmiCasServer) UnicastRemoteObject.exportObject(mapleServer, 0);

        LOG.debug("Register server side RMI interfaces");
        registry.rebind(RmiCasServer.KEY + exportedServer.getId(), exportedServer);

        LOG.info("Successfully registered RMI bindings for Maple JVM");
        RmiProcessHandler.sendReadySignal();
    }
}
