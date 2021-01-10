package gov.nist.drmf.interpreter.maple.secure;

import gov.nist.drmf.interpreter.common.constants.Keys;
import gov.nist.drmf.interpreter.common.eval.NumericalTest;
import gov.nist.drmf.interpreter.common.eval.SymbolicalTest;
import gov.nist.drmf.interpreter.common.exceptions.CASUnavailableException;
import gov.nist.drmf.interpreter.common.exceptions.ComputerAlgebraSystemEngineException;
import gov.nist.drmf.interpreter.common.pojo.NumericResult;
import gov.nist.drmf.interpreter.common.pojo.SymbolicResult;
import gov.nist.drmf.interpreter.common.process.RmiCasServer;
import gov.nist.drmf.interpreter.common.process.RmiProcessHandler;
import gov.nist.drmf.interpreter.common.process.UnrecoverableProcessException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * @author Andre Greiner-Petter
 */
public class MapleRmiClient extends RmiProcessHandler {
    private static final Logger LOG = LogManager.getLogger(MapleRmiClient.class.getName());

    private RmiCasServer server;

    private MapleRmiClient() throws CASUnavailableException {
        super( MapleRmiServer.class, List.of("-Xmx2g", "-Xss100M") );
    }

    @Override
    public void start() throws CASUnavailableException {
        try {
            setupRmiConnection();
        } catch (Exception e) {
            throw new CASUnavailableException(e);
        }
    }

    private void setupRmiConnection() throws Exception {
        super.start();
        LOG.info("Started remote Maple JVM.");
        Registry registry = LocateRegistry.getRegistry();
        server = (RmiCasServer) registry.lookup(RmiCasServer.KEY + Keys.KEY_MAPLE);
        LOG.info("Initialize maple");
        server.init();
    }

    private static void waitUntilRecovered(CompletableFuture<?> processFuture) throws UnrecoverableProcessException {
        try {
            LOG.debug("Block continuing until the subprocess recovered.");
            Object resultObject = processFuture.get();
            LOG.debug("Finished waiting for process to be done.");
            if ( resultObject == null ) {
                LOG.warn("Unable to recover from a fatal CAS JVM crash.");
                throw new UnrecoverableProcessException();
            }
        } catch (CancellationException | InterruptedException | ExecutionException e) {
            throw new UnrecoverableProcessException(e);
        }
    }

    public String enterCommand(String command) throws ComputerAlgebraSystemEngineException {
        CompletableFuture<?> processFuture = super.getProcessFuture();
        try {
            return server.enterCommand(command);
        } catch (RemoteException e) {
            LOG.fatal("Enter custom command caused the CAS JVM to crash. Wait for it to recover. Command was:\n"+command);
            waitUntilRecovered(processFuture);
            return null;
        }
    }

    public void setGlobalAssumptions(List<String> assumptions) {
        CompletableFuture<?> processFuture = super.getProcessFuture();
        try {
            server.setGlobalAssumptions(assumptions);
        } catch (RemoteException e) {
            LOG.fatal("Setting global assumptions crashed CAS JVM. Wait for it to recover.");
            waitUntilRecovered(processFuture);
        }
    }

    public void addRequiredPackages(Set<String> packages) {
        CompletableFuture<?> processFuture = super.getProcessFuture();
        try {
            server.addRequiredPackages(packages);
        } catch (RemoteException e) {
            LOG.fatal("Setting required packages crashed CAS JVM. Wait for it to recover.");
            waitUntilRecovered(processFuture);
        }
    }

    public NumericResult performNumericalTest(NumericalTest test) throws ComputerAlgebraSystemEngineException {
        CompletableFuture<?> processFuture = super.getProcessFuture();
        try {
            return server.performNumericalTest(test);
        } catch (RemoteException e) {
            LOG.fatal("Numerical test case crashed CAS JVM. Wait for it to recover.");
            waitUntilRecovered(processFuture);
            return new NumericResult().markAsCrashed();
        }
    }

    public SymbolicResult symbolicTest(SymbolicalTest test) throws ComputerAlgebraSystemEngineException {
        CompletableFuture<?> processFuture = super.getProcessFuture();
        try {
            return server.symbolicTest(test);
        } catch (RemoteException e) {
            LOG.fatal("Symbolic test case crashed CAS JVM. Wait for it to recover.");
            waitUntilRecovered(processFuture);
            return new SymbolicResult().markAsCrashed();
        }
    }

    public void forceGC() throws ComputerAlgebraSystemEngineException {
        CompletableFuture<?> processFuture = super.getProcessFuture();
        try {
            server.forceGC();
        } catch (RemoteException e) {
            LOG.fatal("Force garbage collection cause the CAS JVM to crash. Wait for it to recover.");
            waitUntilRecovered(processFuture);
        }
    }

    public void shutdown() {
        try {
            server.stop();
        } catch (Exception e) {
            LOG.error("Unable to stop CAS JVM.");
        }
    }

    // the instance
    private static MapleRmiClient clientInstance;

    /**
     * Returns the unique reference to Maple's RMI client
     * @return the unique instance of the Maple client
     * @throws CASUnavailableException if the CAS is unavailable
     */
    public static MapleRmiClient getInstance() throws CASUnavailableException {
        if ( clientInstance == null ) {
            clientInstance = new MapleRmiClient();
            clientInstance.start();
        }
        return clientInstance;
    }
}
