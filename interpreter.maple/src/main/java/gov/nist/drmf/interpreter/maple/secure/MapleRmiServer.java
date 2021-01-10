package gov.nist.drmf.interpreter.maple.secure;

import com.maplesoft.externalcall.MapleException;
import com.maplesoft.openmaple.Algebraic;
import com.maplesoft.openmaple.Engine;
import gov.nist.drmf.interpreter.common.cas.ICASEngineNumericalEvaluator;
import gov.nist.drmf.interpreter.common.cas.ICASEngineSymbolicEvaluator;
import gov.nist.drmf.interpreter.common.constants.Keys;
import gov.nist.drmf.interpreter.common.eval.NumericalTest;
import gov.nist.drmf.interpreter.common.eval.SymbolicalTest;
import gov.nist.drmf.interpreter.common.exceptions.CASUnavailableException;
import gov.nist.drmf.interpreter.common.exceptions.ComputerAlgebraSystemEngineException;
import gov.nist.drmf.interpreter.common.pojo.NumericResult;
import gov.nist.drmf.interpreter.common.pojo.SymbolicResult;
import gov.nist.drmf.interpreter.common.process.RmiCasServer;
import gov.nist.drmf.interpreter.common.process.RmiProcessHandler;
import gov.nist.drmf.interpreter.maple.extension.OldMapleInterface;
import gov.nist.drmf.interpreter.maple.listener.MapleListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.Set;

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
            e.printStackTrace();
        }
    }

    private RmiMapleConnector maple;

    /**
     * TODO: I have no clue why... but Maple must be lazy initialized.
     */
    private MapleRmiServer() {}

    public void init() throws ComputerAlgebraSystemEngineException, CASUnavailableException {
        if ( this.maple != null ) return;
        this.maple = new RmiMapleConnector();
        this.maple.loadNumericProcedures();
        if ( !this.maple.isCASAvailable() ) {
            throw new CASUnavailableException();
        }
    }

    @Override
    public String getId() {
        return Keys.KEY_MAPLE;
    }

    @Override
    public String enterCommand(String command) throws ComputerAlgebraSystemEngineException {
        return this.maple.getCASEngine().enterCommand(command).toString();
    }

    @Override
    public void setGlobalAssumptions(List<String> assumptions) {
        this.maple.getNumericEvaluator().setGlobalAssumptions(assumptions);
    }

    @Override
    public void addRequiredPackages(Set<String> packages) {
        this.maple.getNumericEvaluator().addRequiredPackages(packages);
    }

    @Override
    public NumericResult performNumericalTest(NumericalTest test) throws ComputerAlgebraSystemEngineException {
        ICASEngineNumericalEvaluator<Algebraic> numericalEvaluator = this.maple.getNumericEvaluator();
        Algebraic result = numericalEvaluator.performNumericalTest(test);
        return numericalEvaluator.getNumericResult(result);
    }

    @Override
    public SymbolicResult symbolicTest(SymbolicalTest test) throws ComputerAlgebraSystemEngineException {
        ICASEngineSymbolicEvaluator<Algebraic> symbolicEvaluator = this.maple.getSymbolicEvaluator();
        return symbolicEvaluator.getResult(test);
    }

    @Override
    public void forceGC() throws ComputerAlgebraSystemEngineException {
        this.maple.getCASEngine().forceGC();
    }

    @Override
    public void stop() throws RemoteException {
        try {
            registry.unbind(RmiCasServer.KEY);
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

    public static void main(String[] args) throws CASUnavailableException, RemoteException, ComputerAlgebraSystemEngineException, MapleException {
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
