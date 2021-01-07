package gov.nist.drmf.interpreter.generic.mlp.cas;

import gov.nist.drmf.interpreter.common.eval.NativeComputerAlgebraInterfaceBuilder;
import gov.nist.drmf.interpreter.common.eval.NumericalConfig;
import gov.nist.drmf.interpreter.common.interfaces.IConstraintTranslator;
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

    private static CASConnections instance;

    private final Map<String, NativeComputerAlgebraInterfaceBuilder<?>> connectionsMap;

    private final CASTranslators translators;

    private CASConnections(CASTranslators casTranslators) {
        this.translators = casTranslators;
        connectionsMap = new HashMap<>();

        // first maple
        NativeComputerAlgebraInterfaceBuilder<?> maple = new MapleConnector();
        tryAddCAS( maple, casTranslators.getTranslator(maple.getLanguageKey()) );

        // next mathematica
        NativeComputerAlgebraInterfaceBuilder<?> mathematica = new MathematicaConnector();
        tryAddCAS( mathematica, casTranslators.getTranslator(mathematica.getLanguageKey()) );
    }

    private void tryAddCAS(NativeComputerAlgebraInterfaceBuilder<?> cas, IConstraintTranslator translator) {
        try {
            if ( !cas.isCASAvailable() ) return;

            connectionsMap.put(cas.getLanguageKey(), cas);
            cas.loadNumericProcedures();

            if ( translator != null ) {
                NumericalConfig config = NumericalConfig.config();
                String[] globalAssumptions = config.getEntireTestSuiteAssumptionsList();
                String[] assumptionsTranslated = translator.translateEachConstraint(globalAssumptions);
                cas.getNumericEvaluator().setGlobalAssumptions(List.of(assumptionsTranslated));
            }
        } catch ( Exception | Error e ) {
            LOG.warn("Unable to establish connection with CAS " + cas.getLanguageKey() + ". " +
                    "Ignoring the CAS when computing MOI.", e);
        }
    }

    public CASTranslators getTranslators() {
        return translators;
    }

    public IConstraintTranslator getTranslator(String cas) {
        return translators.getTranslator(cas);
    }

    public List<NativeComputerAlgebraInterfaceBuilder<?>> getCASConnections() {
        return new LinkedList<>(connectionsMap.values());
    }

    public NativeComputerAlgebraInterfaceBuilder<?> getCASConnection(String cas) {
        return this.connectionsMap.get(cas);
    }

    public static CASConnections getInstance() {
        if ( instance == null ) {
            instance = new CASConnections(new CASTranslators());
        }
        return instance;
    }
}
