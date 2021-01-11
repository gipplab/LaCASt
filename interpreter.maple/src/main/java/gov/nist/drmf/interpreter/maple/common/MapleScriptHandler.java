package gov.nist.drmf.interpreter.maple.common;

import gov.nist.drmf.interpreter.common.constants.GlobalPaths;
import gov.nist.drmf.interpreter.common.eval.INumericalEvaluationScripts;
import gov.nist.drmf.interpreter.common.eval.NumericalConfig;
import gov.nist.drmf.interpreter.common.eval.NumericalTestConstants;
import gov.nist.drmf.interpreter.maple.translation.MapleTranslator;

import java.io.IOException;

/**
 * @author Andre Greiner-Petter
 */
public class MapleScriptHandler implements INumericalEvaluationScripts {

    private String[] numericProcedures;

    private String numericalSievesMethod;
    private String numericalSievesMethodRelations;

    public MapleScriptHandler() throws IOException {
        loadScripts();
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
        numericalSievesMethod = MapleTranslator.extractNameOfProcedure(sieve_procedure);
        numericalSievesMethodRelations = "rel" + numericalSievesMethod;

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
    }

    @Override
    public String getPostProcessingScriptName(boolean isEquation) {
        return isEquation ? numericalSievesMethod : numericalSievesMethodRelations;
    }

    public String[] getNumericProcedures() {
        return numericProcedures;
    }
}
