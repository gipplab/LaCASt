package gov.nist.drmf.interpreter.maple;

import com.maplesoft.openmaple.Algebraic;
import gov.nist.drmf.interpreter.common.cas.ICASEngineNumericalEvaluator;
import gov.nist.drmf.interpreter.common.cas.ICASEngineSymbolicEvaluator;
import gov.nist.drmf.interpreter.common.cas.IComputerAlgebraSystemEngine;
import gov.nist.drmf.interpreter.common.constants.GlobalPaths;
import gov.nist.drmf.interpreter.common.constants.Keys;
import gov.nist.drmf.interpreter.common.eval.*;
import gov.nist.drmf.interpreter.common.exceptions.CASUnavailableException;
import gov.nist.drmf.interpreter.common.pojo.NumericCalculation;
import gov.nist.drmf.interpreter.maple.common.MapleConstants;
import gov.nist.drmf.interpreter.maple.common.SymbolicMapleEvaluatorTypes;
import gov.nist.drmf.interpreter.maple.extension.MapleInterface;
import gov.nist.drmf.interpreter.maple.extension.NumericCalculator;
import gov.nist.drmf.interpreter.maple.extension.Simplifier;
import gov.nist.drmf.interpreter.maple.translation.MapleTranslator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Andre Greiner-Petter
 */
public class MapleConnector implements NativeComputerAlgebraInterfaceBuilder<Algebraic> {
    private static final Logger LOG = LogManager.getLogger(MapleConnector.class.getName());

    private Boolean mapleIsAvailable = null;

    private NumericCalculator numericCalculator = null;
    private Simplifier symbolicCalculator = null;

    private String[] numericProcedures;
    private INumericalEvaluationScripts scriptHandler = null;

    private boolean loadedScriptsSuccessfully = false;

    public MapleConnector() {
        try {
            loadScripts();
            this.loadedScriptsSuccessfully = true;
        } catch ( IOException ioe ) {
            LOG.error("Unable to load procedures for Maple. Continue without procedures.", ioe);
        }
    }

    private void loadScripts() throws IOException {
        numericProcedures = new String[3];
        String numericalProc = MapleTranslator.extractProcedure(GlobalPaths.PATH_MAPLE_NUMERICAL_PROCEDURES);
        numericProcedures[0] = numericalProc;

        // load expectation of results template
        NumericalConfig config =  NumericalConfig.config();
        String expectationTemplate = config.getExpectationTemplate();
        // load numerical sieve
        String sieve_procedure = MapleTranslator.extractProcedure( GlobalPaths.PATH_MAPLE_NUMERICAL_SIEVE_PROCEDURE );
        String sieve_procedure_relation = "rel" + sieve_procedure;

        // replace condition placeholder
        String numericalSievesMethod = MapleTranslator.extractNameOfProcedure(sieve_procedure);
        String numericalSievesMethodRelations = "rel" + numericalSievesMethod;

        sieve_procedure = sieve_procedure.replaceAll(
                NumericalTestConstants.KEY_NUMERICAL_SIEVES_CONDITION,
                expectationTemplate
        );

        sieve_procedure_relation = sieve_procedure_relation.replaceAll(
                NumericalTestConstants.KEY_NUMERICAL_SIEVES_CONDITION,
                "result"
        );

        numericProcedures[1] = sieve_procedure;
        numericProcedures[2] = sieve_procedure_relation;
        scriptHandler = (isEquation -> isEquation ? numericalSievesMethod : numericalSievesMethodRelations);
        LOG.debug("Finish Maple procedures setup.");
    }

    @Override
    public boolean isCASAvailable() {
        if ( !loadedScriptsSuccessfully ) return false;
        if ( mapleIsAvailable == null ) mapleIsAvailable = MapleInterface.isMaplePresent();
        return mapleIsAvailable;
    }

    @Override
    public String getLanguageKey() {
        return Keys.KEY_MAPLE;
    }

    @Override
    public IComputerAlgebraSystemEngine<Algebraic> getCASEngine() throws CASUnavailableException {
        if ( !isCASAvailable() ) throw new CASUnavailableException();
        return MapleInterface.getUniqueMapleInterface();
    }

    @Override
    public ICASEngineNumericalEvaluator<Algebraic> getNumericEvaluator() throws CASUnavailableException {
        if ( !isCASAvailable() ) throw new CASUnavailableException();
        if ( numericCalculator == null ) numericCalculator = new NumericCalculator();
        return numericCalculator;
    }

    @Override
    public ICASEngineSymbolicEvaluator<Algebraic> getSymbolicEvaluator() throws CASUnavailableException {
        if ( !isCASAvailable() ) throw new CASUnavailableException();
        if ( symbolicCalculator == null ) symbolicCalculator = new Simplifier();
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
        return numericProcedures;
    }
}
