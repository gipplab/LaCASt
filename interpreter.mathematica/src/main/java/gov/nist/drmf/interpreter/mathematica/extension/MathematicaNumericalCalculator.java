package gov.nist.drmf.interpreter.mathematica.extension;

import com.wolfram.jlink.Expr;
import com.wolfram.jlink.ExprFormatException;
import com.wolfram.jlink.MathLinkException;
import gov.nist.drmf.interpreter.common.cas.GenericCommandBuilder;
import gov.nist.drmf.interpreter.common.cas.ICASEngineNumericalEvaluator;
import gov.nist.drmf.interpreter.common.exceptions.ComputerAlgebraSystemEngineException;
import gov.nist.drmf.interpreter.common.replacements.LogManipulator;
import gov.nist.drmf.interpreter.mathematica.common.Commands;
import gov.nist.drmf.interpreter.mathematica.evaluate.SymbolicEquivalenceChecker;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.beans.Expression;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Observable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static gov.nist.drmf.interpreter.mathematica.evaluate.SymbolicEquivalenceChecker.MATH_ABORTION_SIGNAL;

/**
 * @author Andre Greiner-Petter
 */
public class MathematicaNumericalCalculator implements ICASEngineNumericalEvaluator<Expr> {
    private static final Logger LOG = LogManager.getLogger(MathematicaNumericalCalculator.class.getName());

    private static final Pattern ILLEGAL_VAR_PATTERNS =
            Pattern.compile("Infinity|Integrate|Sum|Part|FreeVariables|Less|Equal|Piecewise");

    private final MathematicaInterface mathematicaInterface;
    private final SymbolicEquivalenceChecker miEquiChecker;

    private int timeout = -1;

    private String globalAssumptions = "{}";
    private String globalConstraints = "{}";

    private double threshold = 0.001;

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
    private String expr = "expr";
    private String varName = "vars";
    private String eVars = "constVars";
    private String exVars = "extraVars";
    private String cons = "assumptions";
    private String testCasesVar = "testCases";

    private int testCases = 0;
    private int failedCases = 0;
    private Expr wasAborted = null;

    public MathematicaNumericalCalculator() {
        this.mathematicaInterface = MathematicaInterface.getInstance();
        this.miEquiChecker = mathematicaInterface.getEvaluationChecker();
    }

    public void setThreshold(double threshold) {
        this.threshold = threshold;
    }

    private void clearVariables() {
        String cmd = String.format(
                "ClearAll[%s, %s, %s, %s, %s, %s]",
                expr,
                varName,
                eVars,
                exVars,
                cons,
                testCasesVar
        );

        testCases = 0;
        failedCases = 0;
        wasAborted = null;

        try {
            mathematicaInterface.evaluate(cmd);
        } catch (MathLinkException e) {
            LOG.error("Cannot clear variables.");
        }
    }

    private static void addVarDefinitionNL(StringBuilder sb, String varName, String def) {
        sb.append(varName).append(" = ").append(def).append(";").append(NL);
    }

    private static String generateValuesVarName(String var) {
        return var + "Vals";
    }

    private static String buildMathList(Collection<String> list) {
        if ( list == null || list.isEmpty() ) return "{}";
        String l = GenericCommandBuilder.makeListWithDelimiter(list);
        return "{"+l+"}";
    }

    @Override
    public void setGlobalAssumptions(List<String> assumptions) {
        List<String> ass = new LinkedList<>();
        List<String> con = new LinkedList<>();
        for ( String a : assumptions ){
            if ( a.contains("\\[Element]") ) ass.add(a);
            else con.add(a);
        }
        this.globalAssumptions = buildMathList(ass);
        this.globalConstraints = buildMathList(con);
    }

    @Override
    public void storeVariables(Collection<String> variables, Collection<String> testValues) {
        clearVariables();
        String valsName = generateValuesVarName(varName);

        sb = new StringBuilder();
        addVarDefinitionNL(sb, varName, buildMathList(variables));
        addVarDefinitionNL(sb, valsName, buildMathList(testValues));

        LOG.debug("Set variables: " + buildMathList(variables));
        LOG.debug("Set values:    " + buildMathList(testValues));
    }

    @Override
    public void storeConstraintVariables(List<String> constraintVariables, List<String> constraintValues) {
        String eVals = generateValuesVarName(eVars);
        addVarDefinitionNL(sb, eVars, buildMathList(constraintVariables));
        addVarDefinitionNL(sb, eVals, buildMathList(constraintValues));
        if ( constraintVariables != null ) {
            LOG.debug("Set extra variables: " + constraintVariables);
            LOG.debug("Set extra values:    " + constraintValues);
        }
    }

    @Override
    public void storeExtraVariables(List<String> extraVariables, List<String> extraValues) {
        String eVals = generateValuesVarName(exVars);
        addVarDefinitionNL(sb, exVars, buildMathList(extraVariables));
        addVarDefinitionNL(sb, eVals, buildMathList(extraValues));
    }

    @Override
    public String setConstraints(List<String> constraints) {
        String variables = Commands.COMPLEMENT.build(varName, eVars);
        String command = "Join[" +
                    Commands.FILTER_ASSUMPTIONS.build(buildMathList(constraints), variables) + ", " +
                    Commands.FILTER_ASSUMPTIONS.build(globalAssumptions, variables) + ", " +
                    Commands.FILTER_ASSUMPTIONS.build(globalConstraints, variables) +
//                    Commands.FILTER_GLOBAL_ASSUMPTIONS.build(globalConstraints, varName, buildMathList(constraints)) +
                "]";

        addVarDefinitionNL(sb, cons, command);
        return ((constraints == null || constraints.isEmpty()) && globalAssumptions.matches("\\{}") ) ?
                null : cons;
    }

    @Override
    public String buildTestCases(
            String constraintsName,
            int maxCombis
    ) throws ComputerAlgebraSystemEngineException, IllegalArgumentException {
        if ( wasAborted != null ) return null;

        try {
            LOG.info("Setup variables for numerical test case.");
            LOG.trace(sb.toString());
            mathematicaInterface.evaluate(sb.toString());
            String appliedConst = mathematicaInterface.evaluate(cons);
            LOG.debug("Applying constraints: " + appliedConst);
            sb = new StringBuilder();
        } catch (MathLinkException e) {
            LOG.warn("Unable to setup variables for numerical test cases", e);
        }

        // create test cases first
        String testCasesCmd = Commands.CREATE_TEST_CASES.build(
                varName, generateValuesVarName(varName),
                eVars, generateValuesVarName(eVars),
                exVars, generateValuesVarName(exVars)
        );

        if ( constraintsName != null ) {
            // filter cases based on constraints
            testCasesCmd = Commands.FILTER_TEST_CASES.build(
                    constraintsName,
                    testCasesCmd,
                    Integer.toString(maxCombis)
            );
        }
        addVarDefinitionNL(sb, testCasesVar, testCasesCmd);

        // check if number of test cases is below definition
        String lengthCmd = testCasesVar; //Commands.LENGTH_OF_LIST.build(testCasesVar);
        sb.append(lengthCmd);

        String commandString = sb.toString();
        LOG.trace("Numerical Test Commands:"+NL+commandString);

        LOG.debug("Compute number of test cases and check if it is in range.");
        Expr res = runWithTimeout(commandString, timeout);
        if ( wasAborted(res) ) {
            LOG.debug("Unable to generate test cases in time. Timed out.");
            wasAborted = res;
            return null;
        }
        testCases = mathematicaInterface.checkIfEvaluationIsInRange(res, -1, maxCombis+1);
        return testCasesVar;
    }

    @Override
    public void setTimeout(double timeoutInSeconds) {
        this.timeout = (int)(1_000*timeoutInSeconds);
    }

    @Override
    public Expr performNumericalTests(String expression, String testCasesName, String postProcessingMethodName, int precision) throws ComputerAlgebraSystemEngineException {
        if ( wasAborted != null && testCasesName == null ) return wasAborted;

//            String testCasesStr = mathematicaInterface.evaluate(testCasesName);
//            LOG.trace("Test cases: " + testCasesStr);
//            LOG.debug("Sneak of test cases: " + LogManipulator.shortenOutput(testCasesStr, 10));

        sb = new StringBuilder();
        addVarDefinitionNL(sb, expr, expression);
        sb.append(Commands.NUMERICAL_TEST.build(expr, testCasesName, Double.toString(threshold)));
        LOG.info("Compute numerical test for: " + expression);

        return runWithTimeout(sb.toString(), timeout);
    }

    private Expr runWithTimeout(String cmd, int timeout) throws ComputerAlgebraSystemEngineException {
        try {
            Thread abortionThread = null;
            if ( timeout > 0 ) {
                abortionThread = MathematicaInterface.getAbortionThread(this, timeout);
                abortionThread.start();
            }
            Expr result = mathematicaInterface.evaluateToExpression(cmd);
            if ( abortionThread != null ) abortionThread.interrupt();

            return result;
        } catch (MathLinkException e) {
            throw new ComputerAlgebraSystemEngineException(e);
        }
    }

    @Override
    public ResultType getStatusOfResult(Expr results) throws ComputerAlgebraSystemEngineException {
        String resStr = results.toString();

        try {
            failedCases = results.length();
        } catch (Error | Exception e) {
            // nothing to do
        }

//        LOG.info("Numerical test finished. Result: " + resStr);
        LOG.info("Numerical test finished. Result: " + LogManipulator.shortenOutput(resStr, 5));
        if ( !resStr.matches("^\\{.*") ) return ResultType.ERROR;
        if ( resStr.matches("\\{}|\\{0(?:.0)?[^\\d.]+}") ) return ResultType.SUCCESS;
        return ResultType.FAILURE;
    }

    @Override
    public int getPerformedTestCases() {
        return testCases;
    }

    @Override
    public int getNumberOfFailedTestCases() {
        return failedCases;
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
