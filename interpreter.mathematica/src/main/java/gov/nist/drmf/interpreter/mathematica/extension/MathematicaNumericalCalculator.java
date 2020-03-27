package gov.nist.drmf.interpreter.mathematica.extension;

import com.wolfram.jlink.Expr;
import com.wolfram.jlink.MathLinkException;
import gov.nist.drmf.interpreter.common.cas.GenericCommandBuilder;
import gov.nist.drmf.interpreter.common.cas.ICASEngineNumericalEvaluator;
import gov.nist.drmf.interpreter.common.exceptions.ComputerAlgebraSystemEngineException;
import gov.nist.drmf.interpreter.mathematica.common.Commands;
import gov.nist.drmf.interpreter.mathematica.evaluate.SymbolicEquivalenceChecker;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Observable;
import java.util.regex.Pattern;

import static gov.nist.drmf.interpreter.mathematica.evaluate.SymbolicEquivalenceChecker.MATH_ABORTION_SIGNAL;

/**
 * @author Andre Greiner-Petter
 */
public class MathematicaNumericalCalculator implements ICASEngineNumericalEvaluator<Expr> {
    private static final Logger LOG = LogManager.getLogger(MathematicaNumericalCalculator.class.getName());
    private final MathematicaInterface mathematicaInterface;
    private final SymbolicEquivalenceChecker miEquiChecker;

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

    public MathematicaNumericalCalculator() {
        this.mathematicaInterface = MathematicaInterface.getInstance();
        this.miEquiChecker = mathematicaInterface.getEvaluationChecker();
    }

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
            mathematicaInterface.evaluate(cmd);
        } catch (MathLinkException e) {
            LOG.error("Cannot clear variables.");
        }
    }

    private static void addVarDefinitionNL(StringBuilder sb, String varName, String def) {
        sb.append(varName).append(" := ").append(def).append(";").append(NL);
    }

    private static String generateValuesVarName(String var) {
        return var + "Vals";
    }

    private static String buildMathList(List<String> list) {
        if ( list == null || list.isEmpty() ) return "{}";
        String l = GenericCommandBuilder.makeListWithDelimiter(list);
        return "{"+l+"}";
    }

    @Override
    public void storeVariables(String expression, List<String> testValues) {
        clearVariables();
        String valsName = generateValuesVarName(varName);

        sb = new StringBuilder();
        String extract = Commands.EXTRACT_VARIABLES.build(expression);
        addVarDefinitionNL(sb, varName, extract);
        addVarDefinitionNL(sb, valsName, buildMathList(testValues));
    }

    @Override
    public void storeConstraintVariables(List<String> constraintVariables, List<String> constraintValues) {
        String eVals = generateValuesVarName(eVars);
        addVarDefinitionNL(sb, eVars, buildMathList(constraintVariables));
        addVarDefinitionNL(sb, eVals, buildMathList(constraintValues));
    }

    @Override
    public void storeExtraVariables(List<String> extraVariables, List<String> extraValues) {
        String eVals = generateValuesVarName(exVars);
        addVarDefinitionNL(sb, exVars, buildMathList(extraVariables));
        addVarDefinitionNL(sb, eVals, buildMathList(extraValues));
    }

    @Override
    public String setConstraints(List<String> constraints) {
        addVarDefinitionNL(sb, cons, buildMathList(constraints));
        return (constraints == null || constraints.isEmpty()) ? null : cons;
    }

    @Override
    public String buildTestCases(
            String constraintsName,
            int maxCombis
    ) throws ComputerAlgebraSystemEngineException, IllegalArgumentException {
        try {
            LOG.info("Setup variables for numerical test case.");
            mathematicaInterface.evaluate(sb.toString());
            sb = new StringBuilder();
        } catch (MathLinkException e) {
            e.printStackTrace();
        }

        // create test cases first
        String testCasesCmd = Commands.CREATE_TEST_CASES.build(
                varName,
                generateValuesVarName(varName),
                eVars,
                generateValuesVarName(eVars),
                exVars,
                generateValuesVarName(exVars)
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
            String res = mathematicaInterface.evaluate(commandString);
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
            String testCasesStr = mathematicaInterface.evaluate(testCasesName);
            LOG.trace("Test cases: " + testCasesStr);

            String cmd = Commands.NUMERICAL_TEST.build(expression, testCasesName);
            LOG.info("Compute numerical test for " + expression);
            return mathematicaInterface.evaluateToExpression(cmd);
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

    @Override
    public String generateNumericalTestExpression(String input) {
        return input;
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
    @Deprecated
    public void update(Observable observable, Object o) {
        // nothing to do here
    }
}
