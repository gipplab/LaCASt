package gov.nist.drmf.interpreter.maple;

import gov.nist.drmf.interpreter.common.cas.ICASEngineNumericalEvaluator;
import gov.nist.drmf.interpreter.common.cas.ICASEngineSymbolicEvaluator;
import gov.nist.drmf.interpreter.common.cas.IComputerAlgebraSystemEngine;
import gov.nist.drmf.interpreter.common.constants.Keys;
import gov.nist.drmf.interpreter.common.eval.INumericalEvaluationScripts;
import gov.nist.drmf.interpreter.common.eval.ISymbolicTestCases;
import gov.nist.drmf.interpreter.common.eval.NativeComputerAlgebraInterfaceBuilder;
import gov.nist.drmf.interpreter.common.exceptions.CASUnavailableException;
import gov.nist.drmf.interpreter.maple.common.MapleConstants;
import gov.nist.drmf.interpreter.maple.common.MapleScriptHandler;
import gov.nist.drmf.interpreter.maple.common.SymbolicMapleEvaluatorTypes;
import gov.nist.drmf.interpreter.maple.secure.MapleRmiClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

/**
 * @author Andre Greiner-Petter
 */
public class MapleConnector implements NativeComputerAlgebraInterfaceBuilder {
    private static final Logger LOG = LogManager.getLogger(MapleConnector.class.getName());

    private static final MapleRmiClient mapleClient = MapleRmiClient.getInstance();
    private MapleScriptHandler scriptHandler;
    private boolean loadedScripts = false;

    public MapleConnector() {
        try {
            this.scriptHandler = new MapleScriptHandler();
            loadedScripts = true;
        } catch (IOException e) {
            LOG.error("Unable to load scripts for Maple. Continue without procedures.", e);
        }
    }

    public static boolean isMapleAvailable() {
        return MapleRmiClient.isMaplePresent();
    }

    @Override
    public boolean isCASAvailable() {
        if ( !loadedScripts ) return false;
        return isMapleAvailable();
    }

    @Override
    public String getLanguageKey() {
        return Keys.KEY_MAPLE;
    }

    @Override
    public IComputerAlgebraSystemEngine getCASEngine() throws CASUnavailableException {
        if ( !isCASAvailable() ) throw new CASUnavailableException();
        return mapleClient;
    }

    @Override
    public ICASEngineNumericalEvaluator getNumericEvaluator() throws CASUnavailableException {
        if ( !isCASAvailable() ) throw new CASUnavailableException();
        return mapleClient.getNumericEvaluator();
    }

    @Override
    public ICASEngineSymbolicEvaluator getSymbolicEvaluator() throws CASUnavailableException {
        if ( !isCASAvailable() ) throw new CASUnavailableException();
        return mapleClient.getSymbolicEvaluator();
    }

    @Override
    public INumericalEvaluationScripts getEvaluationScriptHandler() throws CASUnavailableException {
        if ( !isCASAvailable() ) throw new CASUnavailableException();
        return scriptHandler;
    }

    @Override
    public ISymbolicTestCases[] getDefaultSymbolicTestCases() {
        return SymbolicMapleEvaluatorTypes.values();
    }

    @Override
    public String[] getDefaultPrePostComputationCommands() {
        return MapleConstants.getDefaultPrePostCommands();
    }

    @Override
    public String[] getNumericProcedures() {
        return scriptHandler.getNumericProcedures();
    }
}
