package gov.nist.drmf.interpreter.maple.secure;

import gov.nist.drmf.interpreter.common.cas.IAbortEvaluator;
import gov.nist.drmf.interpreter.common.cas.IComputerAlgebraSystemEngine;
import gov.nist.drmf.interpreter.common.constants.Keys;
import gov.nist.drmf.interpreter.common.eval.*;
import gov.nist.drmf.interpreter.common.exceptions.CASUnavailableException;
import gov.nist.drmf.interpreter.common.exceptions.ComputerAlgebraSystemEngineException;
import gov.nist.drmf.interpreter.common.process.RmiCasServer;
import gov.nist.drmf.interpreter.common.process.RmiProcessHandler;
import gov.nist.drmf.interpreter.common.process.RmiSubprocessInfo;
import gov.nist.drmf.interpreter.common.process.UnrecoverableProcessException;
import gov.nist.drmf.interpreter.maple.common.MapleConfig;
import gov.nist.drmf.interpreter.maple.extension.MapleInterface;
import gov.nist.drmf.interpreter.maple.extension.NumericCalculator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * @author Andre Greiner-Petter
 */
public class MapleRmiClient extends RmiProcessHandler
        implements IComputerAlgebraSystemEngine, IAbortEvaluator {
    private static final Logger LOG = LogManager.getLogger(MapleRmiClient.class.getName());

    private RmiCasServer server;

    private final MapleRmiClientNumericEvaluator numericEvaluator;
    private final MapleRmiClientSymbolicEvaluator symbolicEvaluator;

    public MapleRmiClient(RmiSubprocessInfo subprocessInfo) throws CASUnavailableException {
        super(subprocessInfo);
        numericEvaluator = new MapleRmiClientNumericEvaluator(this);
        symbolicEvaluator = new MapleRmiClientSymbolicEvaluator(this);
    }

    public MapleRmiClientNumericEvaluator getNumericEvaluator() {
        return numericEvaluator;
    }

    public MapleRmiClientSymbolicEvaluator getSymbolicEvaluator() {
        return symbolicEvaluator;
    }

    @Override
    public void start() throws CASUnavailableException {
        try {
            setupRmiConnection();
        } catch (Exception e) {
            throw new CASUnavailableException(e);
        }
    }

    @Override
    public void stop() {
        if ( isAlive() ) {
            try {
                LOG.info("Send Maple RMI server shutdown signal and wait.");
                server.stop();
                // the server waits 500ms to actually shutdown in order to wait for
                // java to unbind RMI endpoints. Hence, we should wait at least also 500ms
                // to continue. Just in case. We do not really need to wait because there is nothing
                // to wait for actually.
                try { Thread.sleep(1_000); }
                catch (InterruptedException e) {
                    // we dont really care...
                }
            } catch (RemoteException e) {
                LOG.debug("Unable to stop remote JVM.");
            }
        }
        super.stop();
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

    @Override
    public void setTimeout(EvaluatorType type, double timeoutInSeconds) {
        CompletableFuture<?> processFuture = super.getProcessFuture();
        try {
            server.setTimeout(type, timeoutInSeconds);
        } catch (RemoteException e) {
            LOG.fatal("Setup timeout caused the CAS JVM to crash. Wait for it to recover.");
            waitUntilRecovered(processFuture);
        }
    }

    @Override
    public void disableTimeout(EvaluatorType type) {
        CompletableFuture<?> processFuture = super.getProcessFuture();
        try {
            server.disableTimeout(type);
        } catch (RemoteException e) {
            LOG.fatal("Disabling timeout caused the CAS JVM to crash. Wait for it to recover.");
            waitUntilRecovered(processFuture);
        }
    }

    @Override
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

    @Override
    public String buildList(List<String> list) {
        return MapleInterface.buildMapleList(list);
    }

    public void setGlobalNumericAssumptions(List<String> assumptions) throws ComputerAlgebraSystemEngineException {
        CompletableFuture<?> processFuture = super.getProcessFuture();
        try {
            server.setGlobalNumericAssumptions(assumptions);
        } catch (RemoteException e) {
            LOG.fatal("Setting global assumptions crashed CAS JVM. Wait for it to recover.");
            waitUntilRecovered(processFuture);
        }
    }

    public void setGlobalSymbolicAssumptions(List<String> assumptions) throws ComputerAlgebraSystemEngineException {
        CompletableFuture<?> processFuture = super.getProcessFuture();
        try {
            server.setGlobalSymbolicAssumptions(assumptions);
        } catch (RemoteException e) {
            LOG.fatal("Setting global assumptions crashed CAS JVM. Wait for it to recover.");
            waitUntilRecovered(processFuture);
        }
    }

    @Override
    public void forceGC() throws ComputerAlgebraSystemEngineException {
        CompletableFuture<?> processFuture = super.getProcessFuture();
        try {
            server.forceGC();
        } catch (RemoteException e) {
            LOG.fatal("Force garbage collection cause the CAS JVM to crash. Wait for it to recover.", e);
            waitUntilRecovered(processFuture);
        }
    }

    public SymbolicResult performSymbolicTest(SymbolicalTest test) {
        CompletableFuture<?> processFuture = super.getProcessFuture();
        try {
            return server.performSymbolicTest(test);
        } catch (RemoteException e) {
            LOG.fatal("Perform symbolic test cause the CAS JVM to crash. Wait for it to recover. Test was:\n" + test.getTestExpression(), e);
            waitUntilRecovered(processFuture);
            return new SymbolicResult().markAsCrashed();
        }
    }

    public NumericResult performNumericTest(NumericalTest test) throws ComputerAlgebraSystemEngineException {
        CompletableFuture<?> processFuture = super.getProcessFuture();
        try {
            return server.performNumericalTest(test);
        } catch (RemoteException e) {
            LOG.fatal("Numerical test case crashed CAS JVM. Wait for it to recover. Test was: " + test.getTestExpression(), e);
            waitUntilRecovered(processFuture);
            return new NumericResult().markAsCrashed();
        }
    }

    public String generateNumericTestExpression(String expression) {
        return NumericCalculator.generateNumericCalculationExpression(expression);
    }

    /**
     * Simply checks whether the system environment variables are set properly and the paths exists.
     * This method does not include actually starting Maple and see if it works.
     * @return checks if maple is theoretically present. It does not perform any license tests!
     */
    public static boolean isMaplePresent() {
        return MapleConfig.areSystemVariablesSetProperly();
    }
}
