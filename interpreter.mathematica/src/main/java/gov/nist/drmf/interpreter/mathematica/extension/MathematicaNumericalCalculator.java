package gov.nist.drmf.interpreter.mathematica.extension;

import gov.nist.drmf.interpreter.common.cas.AbstractCasEngineNumericalEvaluator;
import gov.nist.drmf.interpreter.common.cas.GenericCommandBuilder;
import gov.nist.drmf.interpreter.common.eval.EvaluatorType;
import gov.nist.drmf.interpreter.common.eval.NumericCalculation;
import gov.nist.drmf.interpreter.common.eval.NumericCalculationGroup;
import gov.nist.drmf.interpreter.common.eval.TestResultType;
import gov.nist.drmf.interpreter.common.exceptions.ComputerAlgebraSystemEngineException;
import gov.nist.drmf.interpreter.mathematica.common.Commands;
import gov.nist.drmf.interpreter.mathematica.core.MathematicaInterface;
import gov.nist.drmf.interpreter.mathematica.wrapper.Expr;
import gov.nist.drmf.interpreter.mathematica.wrapper.MathLinkException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Duration;
import java.util.*;
import java.util.regex.Pattern;

/**
 * @author Andre Greiner-Petter
 */
public class MathematicaNumericalCalculator extends AbstractCasEngineNumericalEvaluator<Expr> {
    private static final Logger LOG = LogManager.getLogger(MathematicaNumericalCalculator.class.getName());

    private final MathematicaInterface mathematicaInterface;

    private Duration timeout = Duration.ofSeconds(-1);

    private String globalAssumptions = "{}";
    private String globalConstraints = "{}";

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
    private final String expr = "expr";
    private final String varName = "vars";
    private final String eVars = "constVars";
    private final String exVars = "extraVars";
    private final String cons = "assumptions";
    private final String testCasesVar = "testCases";

    private String latestTestExpression = "";
    private String lhs, rhs;

    private String lastPrecision;

    private final List<String> latestAppliedConstraints;

    private int testCases = 0;
    private int failedCases = 0;
    private Expr wasAborted = null;

    public MathematicaNumericalCalculator() {
        this.mathematicaInterface = MathematicaInterface.getInstance();
        this.latestAppliedConstraints = new LinkedList<>();
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
        latestAppliedConstraints.clear();
        latestTestExpression = "";
        lhs = "";
        rhs = "";
        lastPrecision = "0";

        try {
            mathematicaInterface.evaluate(cmd);
        } catch (MathLinkException e) {
            LOG.error("Cannot clear variables.");
        }
    }

    @Override
    public void setCurrentTestCase(String lhs, String rhs) {
        this.lhs = lhs;
        this.rhs = rhs;
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
    public void setGlobalNumericAssumptions(List<String> assumptions) {
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
        String command = "Join[";
//                +
//                    Commands.FILTER_ASSUMPTIONS.build(buildMathList(constraints), variables) + ", " +
//                    Commands.FILTER_ASSUMPTIONS.build(globalAssumptions, variables) + ", " +
//                    Commands.FILTER_ASSUMPTIONS.build(globalConstraints, variables) +
////                    Commands.FILTER_GLOBAL_ASSUMPTIONS.build(globalConstraints, varName, buildMathList(constraints)) +
//                "]";

        if ( constraints != null && !constraints.isEmpty() ) {
            command += Commands.FILTER_ASSUMPTIONS.build(buildMathList(constraints), variables);
        }

        if ( command.endsWith("]") ) command += ", ";

        if ( !globalAssumptions.equals("{}") ){
            command += Commands.FILTER_ASSUMPTIONS.build(globalAssumptions, variables);
        }

        if ( command.endsWith("]") ) command += ", ";

        if ( !globalConstraints.equals("{}") ) {
            command += Commands.FILTER_ASSUMPTIONS.build(globalConstraints, variables);
        }

        command += "]";

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

        enterSetup();
        sb = new StringBuilder();

        // create test cases first
        String testCasesCmd = buildTestCasesString(constraintsName, maxCombis);
        addVarDefinitionNL(sb, testCasesVar, testCasesCmd);

        // check if number of test cases is below definition
        sb.append(testCasesVar);

        String commandString = sb.toString();
        LOG.debug("Numerical Test Commands:"+NL+commandString);

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

    private void enterSetup() {
        try {
            LOG.info("Setup variables for numerical test case.");
            LOG.trace(sb.toString());
            mathematicaInterface.evaluate(sb.toString());
            Expr appliedConstraints = mathematicaInterface.evaluateToExpression(cons);
            for ( Expr ac : appliedConstraints.args() ) {
                if ( ac != null ) {
                    String s = mathematicaInterface.evaluate("ToString["+ac.toString()+", InputForm]");
                    if ( !s.isBlank() ) s = s.substring(1, s.length()-1);
                    latestAppliedConstraints.add(s);
                }
            }
            LOG.debug("Applying constraints: " + appliedConstraints.toString());
        } catch (MathLinkException e) {
            LOG.warn("Unable to setup variables for numerical test cases", e);
        }
    }

    private String buildTestCasesString(String constraintsName, int maxCombis) {
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

        return testCasesCmd;
    }

    @Override
    public void setTimeout(EvaluatorType type, double timeLimit) {
        if ( EvaluatorType.NUMERIC.equals(type) ) this.setTimeout(timeLimit);
    }

    @Override
    public void setTimeout(double timeoutInSeconds) {
        this.timeout = Duration.ofMillis( (int)(timeoutInSeconds * 1_000) );
    }

    @Override
    public void disableTimeout(EvaluatorType type) {
        if ( EvaluatorType.NUMERIC.equals(type) )
            this.timeout = Duration.ofSeconds(-1);
    }

    @Override
    public Expr performGeneratedTestOnExpression(String expression, String testCasesName, String postProcessingMethodName, int precision) throws ComputerAlgebraSystemEngineException {
        if ( wasAborted != null && testCasesName == null ) return wasAborted;

        sb = new StringBuilder();
        addVarDefinitionNL(sb, expr, expression);
        latestTestExpression = expression;
        lastPrecision = Double.toString(1/(double)precision);
        sb.append(Commands.NUMERICAL_TEST.build(expr, testCasesName, lastPrecision));
        LOG.info("Compute numerical test for: " + expression);

        return runWithTimeout(sb.toString(), timeout);
    }

    private Expr runWithTimeout(String cmd, Duration timeout) throws ComputerAlgebraSystemEngineException {
        try {
            return mathematicaInterface.evaluateToExpression(cmd, timeout);
        } catch (MathLinkException e) {
            throw new ComputerAlgebraSystemEngineException(e);
        }
    }

    @Override
    public TestResultType getStatusOfSingleResult(Expr result) {
        String checkCommand = Commands.WAS_NUMERICALLY_SUCCESSFUL.build(result.toString(), lastPrecision);
        try {
            String res = mathematicaInterface.evaluate(checkCommand);
            if ( "True".equals(res) ) return TestResultType.SUCCESS;
            else return TestResultType.FAILURE;
        } catch (MathLinkException e) {
            LOG.warn("Unable to analyze result: " + e.getMessage());
            return TestResultType.ERROR;
        }
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
    public String generateNumericTestExpression(String input) {
        return input;
    }

    @Override
    public NumericCalculationGroup getNumericCalculationGroup(Expr result) {
        if ( !result.listQ() ) return new NumericCalculationGroup();

        NumericCalculationGroup group = new NumericCalculationGroup();
        group.setLhs(lhs);
        group.setRhs(rhs);
        group.setTestExpression(latestTestExpression);
        group.setConstraints(new LinkedList<>(latestAppliedConstraints));

        Expr[] resultsArr = result.args();
        for ( Expr res : resultsArr ) {
            try {
                NumericCalculation nc = getNumericCalculation(res);
                group.addTestCalculation(nc);
            } catch (MathLinkException mle) {
                LOG.warn("Unable to generate string of internal expression: " + res.toString());
            }
        }

        return group;
    }

    private NumericCalculation getNumericCalculation(Expr result) throws MathLinkException {
        if ( !result.listQ() ) return null;

        Expr[] singleResultArgs = result.args();
        if ( singleResultArgs.length != 2 ) {
            LOG.warn("Given result list is not a list of numeric results. Expected to 2 elements for a single result but got: " + result.toString());
            return null;
        }

        NumericCalculation nc = new NumericCalculation( getStatusOfSingleResult(singleResultArgs[0]) );
        String s = mathematicaInterface.evaluate("ToString["+singleResultArgs[0].toString()+", InputForm]");
        if ( !s.isBlank() ) s = s.substring(1, s.length()-1);
        nc.setResultExpression( s );

        Map<String, String> varValMap = new HashMap<>();
        nc.setTestValues(varValMap);

        Expr varValPairs = singleResultArgs[1];
        if ( !varValPairs.listQ() ) return nc;

        for ( Expr varValPair : varValPairs.args() ) {
            if ( !varValPair.head().toString().equals("Rule") ) {
                LOG.warn("Unable to parse non-rule numeric variable-value pair.");
                continue;
            }

            Expr[] varValPairArr = varValPair.args();
            String key = mathematicaInterface.evaluate("ToString["+varValPairArr[0].toString()+", InputForm]");
            String val = mathematicaInterface.evaluate("ToString["+varValPairArr[1].toString()+", InputForm]");
            if ( !key.isBlank() ) key = key.substring(1, key.length()-1);
            if ( !val.isBlank() ) val = val.substring(1, val.length()-1);
            varValMap.put( key, val );
        }

        return nc;
    }

    @Override
    public boolean wasAborted(Expr result) {
        return result.toString().matches(Pattern.quote(MathematicaInterface.MATH_ABORTION_SIGNAL));
    }
}
