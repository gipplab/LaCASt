package gov.nist.drmf.interpreter.evaluation;

import com.wolfram.jlink.Expr;
import com.wolfram.jlink.MathLinkException;
import gov.nist.drmf.interpreter.cas.translation.SemanticLatexTranslator;
import gov.nist.drmf.interpreter.common.constants.GlobalPaths;
import gov.nist.drmf.interpreter.common.constants.Keys;
import gov.nist.drmf.interpreter.common.exceptions.ComputerAlgebraSystemEngineException;
import gov.nist.drmf.interpreter.common.exceptions.TranslationException;
import gov.nist.drmf.interpreter.common.grammar.IComputerAlgebraSystemEngine;
import gov.nist.drmf.interpreter.evaluation.constraints.IConstraintTranslator;
import gov.nist.drmf.interpreter.evaluation.core.numeric.ICASEngineNumericalEvaluator;
import gov.nist.drmf.interpreter.evaluation.core.symbolic.ICASEngineSymbolicEvaluator;
import gov.nist.drmf.interpreter.mathematica.MathematicaInterface;
import gov.nist.drmf.interpreter.mathematica.common.Commands;
import gov.nist.drmf.interpreter.mathematica.evaluate.SymbolicEquivalenceChecker;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.List;
import java.util.Observable;
import java.util.regex.Pattern;

/**
 * @author Andre Greiner-Petter
 */
public class MathematicaTranslator implements
        IConstraintTranslator,
        IComputerAlgebraSystemEngine<Expr>,
        ICASEngineSymbolicEvaluator<Expr>,
        ICASEngineNumericalEvaluator<Expr>
{
    private static final Logger LOG = LogManager.getLogger(MathematicaTranslator.class.getName());

    public static final String MATH_ABORTION_SIGNAL = "$Aborted";

    /**
     * Interface to Mathematica
     */
    private MathematicaInterface mi;

    private SymbolicEquivalenceChecker miEquiChecker;

    /**
     * The interface to interact with the DLMF LaTeX translator
     */
    private SemanticLatexTranslator dlmfInterface;

    public MathematicaTranslator() {}

    public void init() throws IOException {
        // setup logging
        System.setProperty( Keys.KEY_SYSTEM_LOGGING, GlobalPaths.PATH_LOGGING_CONFIG.toString() );

        mi = MathematicaInterface.getInstance();
        miEquiChecker = mi.getEvaluationChecker();
        LOG.debug("Initialized Mathematica Interface");


        dlmfInterface = new SemanticLatexTranslator( Keys.KEY_MATHEMATICA );
        dlmfInterface.init( GlobalPaths.PATH_REFERENCE_DATA );
        LOG.debug("Initialized DLMF LaTeX Interface.");
    }

    @Override
    public String translate(String expression, String label) throws TranslationException {
        return dlmfInterface.translate(expression, label);
    }

    @Override
    public Expr enterCommand(String command) throws ComputerAlgebraSystemEngineException {
        try {
            return mi.evaluateToExpression(command);
//            return mi.evaluate(command);
        } catch (MathLinkException e) {
            throw new ComputerAlgebraSystemEngineException(e);
        }
    }

    @Override
    public void forceGC() throws ComputerAlgebraSystemEngineException {
        // nothing to do here, hopefully we don't need to invoke GC for mathematica
    }

    @Override
    public Expr simplify(String expr) throws ComputerAlgebraSystemEngineException {
        try {
            return miEquiChecker.fullSimplify(expr);
        } catch (MathLinkException e) {
            throw new ComputerAlgebraSystemEngineException(e);
        }
    }

    @Override
    public Expr simplify(String expr, String assumption) throws ComputerAlgebraSystemEngineException {
        try {
            return miEquiChecker.fullSimplify(expr, assumption);
        } catch (MathLinkException e) {
            throw new ComputerAlgebraSystemEngineException(e);
        }
    }

    @Override
    public boolean isAsExpected(Expr in, String expect) {
        try {
            return miEquiChecker.testZero(in);
        } catch (MathLinkException e) {
            LOG.error("Cannot check if expression is zero " + in.toString());
            return false;
        }
    }

    @Override
    public void abort() {
        miEquiChecker.abort();
    }

    @Override
    public boolean wasAborted(Expr result) {
        return result.toString().matches(Pattern.quote(MATH_ABORTION_SIGNAL));
    }

    @Override
    public String buildList(List<String> list) {
        String ls = list.toString();
        return ls.substring(1, ls.length()-1);
    }

    @Override
    public void update(Observable o, Object arg) {
        // nothing to do here
    }

    /**
     * Mathematica Numerical Tests Workflow:
     *
     * 1) Get Variables in Expression
     * 2) In case of Constraints
     *  2.1) ConstVars from Variable
     *  2.2) Get ConstVars - Value Pairs
     * 3) In Case of special values
     *  3.1) Special Variables from Variables
     *  3.2) Special Variables - Value Pairs
     * 4) Rest of Variables - Value pairs
     * 5) Variables - Values paris
     */
    private static final String NL = System.lineSeparator();
    private StringBuilder sb;

    private String varName = "vars";
    private String eVars = "constVars";
    private String exVars = "extraVars";
    private String cons = "assumptions";
    private String testCasesVar = "testCases";

    private void clearVariables() {
        String cmd = String.format(
                "ClearAll[%s, %s, %s, %s, %s]",
                varName,
                eVars,
                exVars,
                cons,
                testCasesVar
        );
        try {
            mi.evaluate(cmd);
        } catch (MathLinkException e) {
            LOG.error("Cannot clear variables.");
        }
    }

    /**
     * Extracts variables and stores them.
     * @param expression mathematical expression (already translated)
     * @param testValues list of values
     * @return variable name
     */
    @Override
    public String storeVariables(String expression, List<String> testValues) {
        clearVariables();
        String valsName = generateValuesVarName(varName);

        sb = new StringBuilder();
        String extract = Commands.EXTRACT_VARIABLES.build(expression);
        addVarDefinitionNL(sb, varName, extract);
        addVarDefinitionNL(sb, valsName, buildMathList(testValues));
        return varName;
    }

    @Override
    public String storeConstraintVariables(String variableName, List<String> constraintVariables, List<String> constraintValues) {
        String eVals = generateValuesVarName(eVars);
        addVarDefinitionNL(sb, eVars, buildMathList(constraintVariables));
        addVarDefinitionNL(sb, eVals, buildMathList(constraintValues));
        return eVars;
    }

    @Override
    public String storeExtraVariables(String variableName, List<String> extraVariables, List<String> extraValues) {
        String eVals = generateValuesVarName(exVars);
        addVarDefinitionNL(sb, exVars, buildMathList(extraVariables));
        addVarDefinitionNL(sb, eVals, buildMathList(extraValues));
        return exVars;
    }

    @Override
    public String setConstraints(List<String> constraints) {
        addVarDefinitionNL(sb, cons, buildMathList(constraints));
        return (constraints == null || constraints.isEmpty()) ? null : cons;
    }

    @Override
    public String buildTestCases(String constraintsName, String variableNames, String constraintVariableNames, String extraVariableNames, int maxCombis) throws ComputerAlgebraSystemEngineException, IllegalArgumentException {
        try {
            LOG.info("Setup variables for numerical test case.");
            mi.evaluate(sb.toString());
            sb = new StringBuilder();
        } catch (MathLinkException e) {
            e.printStackTrace();
        }

        // create test cases first
        String testCasesCmd = Commands.CREATE_TEST_CASES.build(
                variableNames,
                generateValuesVarName(variableNames),
                constraintVariableNames,
                generateValuesVarName(constraintVariableNames),
                extraVariableNames,
                generateValuesVarName(extraVariableNames)
        );

        if ( constraintsName != null ) {
            // filter cases based on constraints
            testCasesCmd = Commands.FILTER_TEST_CASES.build(constraintsName, testCasesCmd);
        }
        addVarDefinitionNL(sb, testCasesVar, testCasesCmd);

        // check if number of test cases is below definition
        String lengthCmd = Commands.LENGTH_OF_LIST.build(testCasesVar);
        sb.append(lengthCmd);

        String commandString = sb.toString();
        LOG.trace("Numerical Test Commands:"+NL+commandString);

        try {
            String res = mi.evaluate(commandString);
            LOG.debug("Generated test cases: " + res);
            int nT = Integer.parseInt(res);
            if ( nT > maxCombis ) throw new IllegalArgumentException("Too many test combinations.");
            // res should be an integer, testing how many test commands there are!
        } catch (MathLinkException | NumberFormatException e) {
            throw new ComputerAlgebraSystemEngineException(e);
        }

        return testCasesVar;
    }

    @Override
    public Expr performNumericalTests(String expression, String testCasesName, String postProcessingMethodName, int precision) throws ComputerAlgebraSystemEngineException {
        try {
            String testCasesStr = mi.evaluate(testCasesName);
            LOG.trace("Test cases: " + testCasesStr);

            String cmd = Commands.NUMERICAL_TEST.build(expression, testCasesName);
            LOG.info("Compute numerical test for " + expression);
            return mi.evaluateToExpression(cmd);
        } catch (MathLinkException e) {
            throw new ComputerAlgebraSystemEngineException(e);
        }
    }

    @Override
    public ResultType getStatusOfResult(Expr results) throws ComputerAlgebraSystemEngineException {
        String resStr = results.toString();
        LOG.info("Numerical test finished. Result: " + resStr);
        return resStr.matches("\\{}") ? ResultType.SUCCESS : ResultType.FAILURE;
    }

    private void addVarDefinitionNL(StringBuilder sb, String varName, String def) {
        sb.append(varName).append(" := ").append(def).append(";").append(NL);
    }

    private static String generateValuesVarName(String var) {
        return var + "Vals";
    }

    private static String buildMathList(List<String> list) {
        if ( list == null || list.isEmpty() ) return "{}";
        String l = MapleSimplifier.makeListWithDelimiter(list);
        return "{"+l+"}";
    }

    @Override
    public String generateNumericalTestExpression(String input) {
        return input;
    }
}
