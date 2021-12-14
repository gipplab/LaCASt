package gov.nist.drmf.interpreter.maple.secure;

import gov.nist.drmf.interpreter.common.cas.ICASEngineNumericalEvaluator;
import gov.nist.drmf.interpreter.common.cas.ICASEngineSymbolicEvaluator;
import gov.nist.drmf.interpreter.common.cas.ICASEngine;
import gov.nist.drmf.interpreter.common.constants.Keys;
import gov.nist.drmf.interpreter.common.eval.*;
import gov.nist.drmf.interpreter.common.exceptions.CASUnavailableException;
import gov.nist.drmf.interpreter.maple.common.MapleConstants;
import gov.nist.drmf.interpreter.maple.common.MapleScriptHandler;
import gov.nist.drmf.interpreter.maple.common.SymbolicMapleEvaluatorTypes;
import gov.nist.drmf.interpreter.maple.extension.MapleInterface;
import gov.nist.drmf.interpreter.maple.extension.MapleNumericCalculator;
import gov.nist.drmf.interpreter.maple.extension.MapleSimplifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

/**
 * @author Andre Greiner-Petter
 */
public class InternalRmiMapleConnector implements NativeComputerAlgebraInterfaceBuilder {
    private static final Logger LOG = LogManager.getLogger(InternalRmiMapleConnector.class.getName());

    private Boolean mapleIsAvailable = null;

    private MapleNumericCalculator numericCalculator = null;
    private MapleSimplifier symbolicCalculator = null;

    private MapleScriptHandler scriptHandler;

    private boolean loadedScriptsSuccessfully = false;

    InternalRmiMapleConnector() {
        try {
            scriptHandler = new MapleScriptHandler();
            this.loadedScriptsSuccessfully = true;
            LOG.info("Setup maple script handler.");
        } catch ( IOException ioe ) {
            LOG.error("Unable to load procedures for Maple. Continue without procedures.", ioe);
        }
    }

    @Override
    public boolean isCASAvailable() {
        if ( !loadedScriptsSuccessfully ) return false;
        try {
            return MapleInterface.getUniqueMapleInterface() != null;
        } catch ( Exception | Error e ) {
            LOG.warn("Cannot init maple interface", e);
            return false;
        }
    }

    @Override
    public String getLanguageKey() {
        return Keys.KEY_MAPLE;
    }

    @Override
    public ICASEngine getCASEngine() throws CASUnavailableException {
        if ( !isCASAvailable() ) throw new CASUnavailableException();
        return MapleInterface.getUniqueMapleInterface();
    }

    @Override
    public ICASEngineNumericalEvaluator getNumericEvaluator() throws CASUnavailableException {
        if ( !isCASAvailable() ) throw new CASUnavailableException();
        if ( numericCalculator == null ) numericCalculator = new MapleNumericCalculator();
        return numericCalculator;
    }

    @Override
    public ICASEngineSymbolicEvaluator getSymbolicEvaluator() throws CASUnavailableException {
        if ( !isCASAvailable() ) throw new CASUnavailableException();
        if ( symbolicCalculator == null ) symbolicCalculator = new MapleSimplifier();
        return symbolicCalculator;
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
