package gov.nist.drmf.interpreter.generic.mlp.cas;

import gov.nist.drmf.interpreter.common.config.GenericLacastConfig;
import gov.nist.drmf.interpreter.common.eval.NativeComputerAlgebraInterfaceBuilder;
import gov.nist.drmf.interpreter.common.eval.NumericalConfig;
import gov.nist.drmf.interpreter.common.eval.SymbolicalConfig;
import gov.nist.drmf.interpreter.common.exceptions.CASUnavailableException;
import gov.nist.drmf.interpreter.common.exceptions.InitTranslatorException;
import gov.nist.drmf.interpreter.common.interfaces.IConstraintTranslator;
import gov.nist.drmf.interpreter.core.api.DLMFTranslator;
import gov.nist.drmf.interpreter.maple.MapleConnector;
import gov.nist.drmf.interpreter.mathematica.MathematicaConnector;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author Andre Greiner-Petter
 */
public final class CASConnections {
    private static final Logger LOG = LogManager.getLogger(CASConnections.class.getName());

    private final Map<String, NativeComputerAlgebraInterfaceBuilder> connectionsMap;

    private final Map<String, NumericalConfig> numericalConfigMap;
    private final Map<String, SymbolicalConfig> symbolicalConfigMap;

    public CASConnections(GenericLacastConfig config) {
        connectionsMap = new HashMap<>();
        numericalConfigMap = new HashMap<>();
        symbolicalConfigMap = new HashMap<>();

        // first maple
        try {
            NativeComputerAlgebraInterfaceBuilder maple = config.getMapleSubprocessInfo() == null ?
                    new MapleConnector() : new MapleConnector(config.getMapleSubprocessInfo());
            tryAddCAS( maple );
        } catch ( ExceptionInInitializerError | CASUnavailableException e ) {
            LOG.warn("Maple is unavailable! So we will not be able to use Maple in the following computations.", e);
        }

        // next mathematica
        try {
            NativeComputerAlgebraInterfaceBuilder mathematica = new MathematicaConnector();
            tryAddCAS( mathematica );
        } catch ( ExceptionInInitializerError | CASUnavailableException e ) {
            LOG.warn("Mathematica is unavailable! So we will not be able to use Mathematica in the following computations.", e);
        }
    }

    private synchronized void tryAddCAS(NativeComputerAlgebraInterfaceBuilder cas) {
        try {
            if ( !cas.isCASAvailable() ) return;
            DLMFTranslator translator = new DLMFTranslator(cas.getLanguageKey());
            translator.getConfig().setLettersAsConstantsMode(true);

            connectionsMap.put(cas.getLanguageKey(), cas);

            NumericalConfig numConfig = NumericalConfig.config();
            cas.getNumericEvaluator().setTimeout(numConfig.getTimeout());
            cas.loadNumericProcedures();
            numericalConfigMap.put(cas.getLanguageKey(), numConfig);

            SymbolicalConfig symConfig = new SymbolicalConfig(cas.getDefaultSymbolicTestCases());
            cas.getSymbolicEvaluator().setTimeout(symConfig.getTimeout());
            symbolicalConfigMap.put(cas.getLanguageKey(), symConfig);

            String[] globalAssumptions = numConfig.getEntireTestSuiteAssumptionsList();
            String[] assumptionsTranslated = translator.translateEachConstraint(globalAssumptions);
            cas.getNumericEvaluator().setGlobalNumericAssumptions(List.of(assumptionsTranslated));
        } catch ( InitTranslatorException ite ) {
            LOG.warn("Forward translator for CAS " + cas.getLanguageKey() + " is unavailable. " +
                    "CAS Connection is not established");
        } catch ( Exception | Error e ) {
            LOG.warn("Unable to establish connection with CAS " + cas.getLanguageKey() + ". " +
                    "Ignoring the CAS when computing MOI.", e);
        }
    }

    public NativeComputerAlgebraInterfaceBuilder getCASConnection(String cas) {
        return this.connectionsMap.get(cas);
    }

    public NumericalConfig getNumericalConfig(String cas) {
        return this.numericalConfigMap.get(cas);
    }

    public SymbolicalConfig getSymbolicalConfig(String cas) {
        return this.symbolicalConfigMap.get(cas);
    }
}
