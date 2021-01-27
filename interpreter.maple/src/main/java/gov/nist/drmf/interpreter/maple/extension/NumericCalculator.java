package gov.nist.drmf.interpreter.maple.extension;

import com.maplesoft.externalcall.MapleException;
import com.maplesoft.openmaple.Algebraic;
import com.maplesoft.openmaple.Numeric;
import gov.nist.drmf.interpreter.common.cas.PackageWrapper;
import gov.nist.drmf.interpreter.common.constants.Keys;
import gov.nist.drmf.interpreter.common.cas.AbstractCasEngineNumericalEvaluator;
import gov.nist.drmf.interpreter.common.eval.EvaluatorType;
import gov.nist.drmf.interpreter.common.eval.NumericCalculationGroup;
import gov.nist.drmf.interpreter.common.eval.TestResultType;
import gov.nist.drmf.interpreter.common.exceptions.ComputerAlgebraSystemEngineException;
import gov.nist.drmf.interpreter.common.eval.NumericCalculation;
import gov.nist.drmf.interpreter.common.symbols.BasicFunctionsTranslator;
import gov.nist.drmf.interpreter.common.symbols.SymbolTranslator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static gov.nist.drmf.interpreter.common.constants.GlobalConstants.NL;
import static gov.nist.drmf.interpreter.maple.extension.CommandBuilder.makeMapleList;
import static gov.nist.drmf.interpreter.maple.extension.CommandBuilder.makeMapleSet;

/**
 * @author Andre Greiner-Petter
 */
public class NumericCalculator extends AbstractCasEngineNumericalEvaluator<Algebraic> {
    private static final Logger LOG = LogManager.getLogger(NumericCalculator.class.getName());

    public static final int MAX_LOG_LENGTH = 300;

    private List<String> globalAssumptions = new LinkedList<>();
    private List<String> globalConstraints = new LinkedList<>();

    private final MapleInterface maple;
    private final PackageWrapper packageWrapper;

    private StringBuffer commandsList;

    private final String testExpression = "nTest";
    private final String varNames = "nVars";
    private final String conVarN = "nConstVars";
    private final String specVarN = "nSpecialVars";
    private final String consN = "constraints";

    private String latestTestExpression = "";
    private String lhs, rhs;
    private String lastPrecision;
    private String latestResultCheckMethod = "";
    private final List<String> latestAppliedConstraints;

    private boolean conVarSet = false;
    private boolean specVarSet = false;

    private double timeLimit = -1;
    private boolean timedOutBySetup = false;

    private int numberOfFailedCases = 0;
    private int numberOfTestCases = 0;

    private Set<String> requiredPackages = new HashSet<>();

    public NumericCalculator() {
        maple = MapleInterface.getUniqueMapleInterface();
        commandsList = new StringBuffer();
        latestAppliedConstraints = new LinkedList<>();

        SymbolTranslator symbolTranslator = new SymbolTranslator(Keys.KEY_LATEX, Keys.KEY_MAPLE);
        BasicFunctionsTranslator basicFunctionsTranslator = new BasicFunctionsTranslator(Keys.KEY_MAPLE);
        try {
            symbolTranslator.init();
            basicFunctionsTranslator.init();
        } catch (IOException e) {
            LOG.fatal("Unable to initiate the symbol and function translator.", e);
        }
        packageWrapper = new PackageWrapper(basicFunctionsTranslator, symbolTranslator);
    }

    @Override
    public void setTimeout(EvaluatorType type, double timeLimit) {
        if ( EvaluatorType.NUMERIC.equals(type) ) this.timeLimit = timeLimit;
    }

    @Override
    public void setTimeout(double timeLimit) {
        this.timeLimit = timeLimit;
    }

    private void setVariable(StringBuffer sb, String vars, String vals) {
        setVariable(sb, vars, vals, true);
    }

    private void clearAll() {
        // reset commandsList
        commandsList = new StringBuffer();
        timedOutBySetup = false;
        numberOfFailedCases = 0;
        numberOfTestCases = 0;
        latestTestExpression = "";
        lhs = "";
        rhs = "";
        lastPrecision = "0";
        requiredPackages.clear();
        latestAppliedConstraints.clear();
        latestResultCheckMethod = "";

        String varValues = AbstractCasEngineNumericalEvaluator.getValuesName(varNames);
        String conValsN = AbstractCasEngineNumericalEvaluator.getValuesName(conVarN);
        String specValsN = AbstractCasEngineNumericalEvaluator.getValuesName(specVarN);

        commandsList.append("unassign('" + testExpression + "'):").append(NL);
        commandsList.append("unassign('" + consN + "'):").append(NL);
        commandsList.append("unassign('" + varNames + "'):").append(NL);
        commandsList.append("unassign('" + conVarN + "'):").append(NL);
        commandsList.append("unassign('" + specVarN + "'):").append(NL);
        commandsList.append("unassign('").append(varValues).append("'):").append(NL);
        commandsList.append("unassign('").append(conValsN).append("'):").append(NL);
        commandsList.append("unassign('").append(specValsN).append("'):");

        try {
            maple.evaluate(commandsList.toString());
        } catch (MapleException e) {
            LOG.error("Unable to reset variables", e);
        } finally {
            commandsList = new StringBuffer();
        }
    }

    @Override
    public void setCurrentTestCase(String lhs, String rhs) {
        this.lhs = lhs;
        this.rhs = rhs;
    }

    @Override
    public boolean requiresRegisteredPackages() {
        return true;
    }

    @Override
    public void addRequiredPackages(Set<String> requiredPackages) {
        this.requiredPackages.addAll(requiredPackages);
    }

    private void setVariable(StringBuffer sb, String vars, String vals, boolean suppress) {
        sb
                .append(vars)
                .append(":=")
                .append(vals)
                .append(suppress ? ":" : ";")
                .append(NL);
    }

    private static final Pattern IN_PATTERN = Pattern.compile("^(.*) in (.*)$");

    @Override
    public void setGlobalNumericAssumptions(List<String> assumptions) {
        List<String> ass = new LinkedList<>();
        List<String> con = new LinkedList<>();
        for ( String a : assumptions ){
            Matcher m = IN_PATTERN.matcher(a);
            if ( m.matches() ) {
                String domainConstraint = m.group(1) + "::" + m.group(2);
                ass.add(domainConstraint);
            } else con.add(a);
        }
        this.globalAssumptions = ass;
        this.globalConstraints = con;
    }

    @Override
    public void storeVariables(Collection<String> variables, Collection<String> testValues) {
        // reset commandsList
        clearAll();

        String varValues = AbstractCasEngineNumericalEvaluator.getValuesName(varNames);
        setVariable(commandsList, varNames, makeMapleSet(variables));
        setVariable(commandsList, varValues, makeMapleList(testValues));

        LOG.debug("Set variables: " + makeMapleSet(variables));
        LOG.debug("Set values:    " + makeMapleList(testValues));
    }

    private void tryTimeOutExpression(StringBuffer sb, double timeout, String expression) {
        sb.append(testExpression)
                .append(" := try timelimit(")
                .append(timeout).append(", ").append(expression)
                .append(")");
        sb.append(NL).append("  catch \"time expired\":").append(NL);
        sb.append("    \"TIMED-OUT\";").append(NL);
        sb.append("  end try:").append(NL);
    }

    @Override
    public void storeConstraintVariables(List<String> constraintVariables, List<String> constraintValues) {
        if ( constraintVariables == null || constraintVariables.isEmpty() ){
            this.conVarSet = false;
            return;
        }

        String conValsN = AbstractCasEngineNumericalEvaluator.getValuesName(conVarN);
        setVariable(commandsList, conVarN, makeMapleList(constraintVariables));
        setVariable(commandsList, conValsN, makeMapleList(constraintValues));

        // update vars list
        setVariable(commandsList, varNames, varNames + " minus " + makeMapleSet(constraintVariables));

        LOG.debug("Set extra variables: " + constraintVariables);
        LOG.debug("Set extra values:    " + constraintValues);

        this.conVarSet = true;
    }

    @Override
    public void storeExtraVariables(List<String> extraVariables, List<String> extraValues) {
        if ( extraVariables == null || extraVariables.isEmpty() ){
            this.specVarSet = false;
            return;
        }
        String specValsN = AbstractCasEngineNumericalEvaluator.getValuesName(specVarN);

        commandsList.append(specVarN).append(":=")
                .append(varNames).append(" intersect ").append(makeMapleSet(extraVariables))
                .append(":").append(NL);
        commandsList.append(specValsN).append(":=").append(makeMapleList(extraValues)).append(":").append(NL);

        // update vars list
        commandsList
                .append(varNames).append(":=")
                .append(varNames).append(" minus (indets(").append(specVarN).append(",name) minus {constants}):")
                .append(NL);

        this.specVarSet = true;
    }

    @Override
    public String setConstraints(List<String> constraints) {
        commandsList.append(consN).append(":=");

        if ( constraints == null ) constraints = new LinkedList<>();

        constraints.addAll(globalConstraints);
        constraints.addAll(globalAssumptions);

        if ( constraints.isEmpty() )
            commandsList.append("[]:");
        else {
            commandsList.append("select(tmp -> verify(indets(tmp, name) minus {constants}, ")
                    .append(varNames).append(" union ").append(specVarN)
                    .append(", `subset`), ")
                    .append(makeMapleList(constraints))
                    .append("):");
        }

        commandsList.append(NL);

        return consN;
    }

    public void loadPackages() {
        if ( !requiredPackages.isEmpty() ) {
            try {
                String loadCommands = packageWrapper.loadPackages(requiredPackages);
                maple.evaluate(loadCommands);
                LOG.debug("Loaded packages: " + requiredPackages);
            } catch (MapleException e) {
                e.printStackTrace();
            }
        }
    }

    public void unloadPackages() {
        if ( !requiredPackages.isEmpty() ) {
            try {
                String loadCommands = packageWrapper.unloadPackages(requiredPackages);
                maple.evaluate(loadCommands);
                LOG.debug("Unloaded packages: " + requiredPackages);
            } catch (MapleException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public String buildTestCases(String constraintsName, int maxCombis)
            throws ComputerAlgebraSystemEngineException, IllegalArgumentException {
        String testValuesN = "nTestVals";

        String vals = AbstractCasEngineNumericalEvaluator.getValuesName(varNames);
        commandsList.append(testValuesN).append(":= [op(createListInList(")
                .append(varNames).append(",").append(vals).append("))");

        String combis = "inCombis := nops("+vals+")^nops("+varNames+")";

        combis = additionalCalculations(combis);
        commandsList.append("]:").append(NL);
        combis += ";";
        commandsList.append(combis).append(NL);

        maple.evaluateAndCheckRangeOfResult(commandsList.toString(), 0, Integer.MAX_VALUE);

        try {
            Algebraic a = maple.evaluate("constraints;");
            setActiveConstraints(a);
            LOG.info("Active constraints: " + a.toString());
        } catch (MapleException e) {
            LOG.error("Unable to check constraints", e);
        }

        commandsList = new StringBuffer();
        commandsList.append(testValuesN).append(":= buildTestValues(")
                .append(constraintsName).append(",").append(testValuesN).append(",").append(maxCombis).append("):").append(NL);
        return testValuesN;
    }

    private void setActiveConstraints(Algebraic constraintsList) throws MapleException {
        if ( constraintsList == null ) return;

        if ( !(constraintsList instanceof com.maplesoft.openmaple.List) ) {
            latestAppliedConstraints.add(constraintsList.toString());
        }

        com.maplesoft.openmaple.List conList = (com.maplesoft.openmaple.List) constraintsList;
        for ( int i = 0; i < conList.length(); i++ ) {
            latestAppliedConstraints.add( conList.get(i).toString() );
        }
    }

    private String additionalCalculations(String combis) {
        if ( specVarSet ) {
            String extVals = AbstractCasEngineNumericalEvaluator.getValuesName(specVarN);
            commandsList.append(", op(createListInList(")
                    .append(specVarN).append(",").append(extVals).append("))");
            combis += " * nops("+extVals+")^nops("+specVarN+")";
        }

        if ( conVarSet ) {
            String conVals = AbstractCasEngineNumericalEvaluator.getValuesName(conVarN);
            commandsList.append(", specialVariables(")
                    .append(conVarN).append(",").append(conVals).append(")");
        }

        return combis;
    }

    @Override
    public Algebraic performGeneratedTestOnExpression(
            String expression,
            String testCasesName,
            String postProcessingMethodName,
            int precision
    ) throws ComputerAlgebraSystemEngineException {
        if ( timedOutBySetup ) return null;

        latestTestExpression = expression;

        loadPackages();

        if ( timeLimit > 0 ) {
            tryTimeOutExpression(commandsList, timeLimit, expression);
        } else {
            commandsList.append(testExpression).append(" := ")
                    .append(expression).append(":").append(NL);
        }

        try {
            LOG.info("Start numerical test for: " + expression);
//            LOG.debug("Perform numerical test:" + NL + commandsList.toString());

            Algebraic testCases = maple.evaluate(
                    commandsList.toString() + NL + testCasesName + ";"
            );

            LOG.debug("Entered numerical preparations.");
            checkValues(testCases, testCasesName);

            String numericalTest =
                    "numResults := SpecialNumericalTesterTimeLimit(" +
                            timeLimit + ", nTest, "+testCasesName+","+precision+
                            ");";
            LOG.debug("Start numerical evaluation: " + numericalTest);

            latestResultCheckMethod = postProcessingMethodName;
            Algebraic results = maple.evaluate(numericalTest);
            return results;

//            if ( wasAborted(results) ) return results;
//            else if ( postProcessingMethodName != null && !postProcessingMethodName.isEmpty() ) {
//                Algebraic result = maple.evaluate(
//                        postProcessingMethodName + "(numResults);"
//                );
//
//                if ( result instanceof com.maplesoft.openmaple.List ) {
//                    com.maplesoft.openmaple.List list = (com.maplesoft.openmaple.List) result;
//                    LOG.info("First entries of result list [total: " + list.length() + "]: " + list.subList(0, Math.min(5, list.length())));
//                }
//
//                return result;
//            } else return results;
        } catch (MapleException e) {
            throw new ComputerAlgebraSystemEngineException(e);
        } finally {
            unloadPackages();
        }
    }

    @Override
    public TestResultType getStatusOfResult(Algebraic result) throws ComputerAlgebraSystemEngineException {
        if ( result == null ) return TestResultType.ERROR;
        try {
            if ( latestResultCheckMethod.isBlank() ) {
                LOG.error("Unable to find current result method. Something went wrong with perform numeric test order.");
                return TestResultType.ERROR;
            }

            if ( result instanceof com.maplesoft.openmaple.List ) {
                com.maplesoft.openmaple.List resList = (com.maplesoft.openmaple.List) result;
                if ( resList.length() != 2 ) {
                    LOG.error("The provided result is not a single result. Expected a list of value and test values but got " + resList.toString());
                    return TestResultType.ERROR;
                }

                Algebraic aResult = maple.evaluate(
                        latestResultCheckMethod + "(" + resList.get(0).toString() + ");"
                );
                String resString = aResult.toString();
                if ( "False".equals(resString) ) return TestResultType.FAILURE;
                else if ( "True".equals(resString) ) return TestResultType.SUCCESS;
                else {
                    LOG.warn("Analyzing test result: " + resString);
                    return TestResultType.ERROR;
                }
            } else {
                if ( wasAborted(result) ) {
                    return TestResultType.SKIPPED;
                } else {
                    LOG.warn("Result was not a list object... " + result.toString());
                    return TestResultType.ERROR;
                }
            }
        } catch (MapleException me) {
            throw new ComputerAlgebraSystemEngineException(me);
        } catch (Exception e) {
            LOG.warn("Error during check the result. " + result.toString());
            return TestResultType.ERROR;
        }
    }

    @Override
    public NumericCalculationGroup getNumericCalculationGroup(Algebraic results) {
        try {
            if ( results instanceof com.maplesoft.openmaple.List ) {
                com.maplesoft.openmaple.List resList = (com.maplesoft.openmaple.List) results;
                NumericCalculationGroup group = new NumericCalculationGroup();
                group.setTestExpression(latestTestExpression);
                group.setConstraints(new LinkedList<>(latestAppliedConstraints));
                group.setLhs(lhs);
                group.setRhs(rhs);

                for ( int i = 0; i < resList.length(); i++ ) {
                    Algebraic resI = resList.get(i);
                    NumericCalculation singleCalc = getNumericCalculation(resI);
                    if ( singleCalc != null ) group.addTestCalculation(singleCalc);
                }
                return group;
            }
        } catch (MapleException me) {
            LOG.warn("Unable to create list of numerical calculations from given result.");
        }
        LOG.debug("Given list was empty or not a list.");
        return new NumericCalculationGroup();
    }

    private NumericCalculation getNumericCalculation(Algebraic singleResult) {
        if ( !(singleResult instanceof com.maplesoft.openmaple.List) ) return null;
        com.maplesoft.openmaple.List resList = (com.maplesoft.openmaple.List) singleResult;

        try {
            if ( resList.length() != 2 ) {
                LOG.warn("The given single numeric test result is not a numeric test result. Expected 2 elements but got: " + singleResult.toString());
                return null;
            }

            NumericCalculation numericCalculation = new NumericCalculation();
            LOG.trace("Get result of " + resList);
            numericCalculation.setResult(getStatusOfResult(resList));
            numericCalculation.setResultExpression(resList.get(0).toString());

            Map<String, String> varValMap = new HashMap<>();
            numericCalculation.setTestValues(varValMap);

            Algebraic variableValues = resList.get(1);
            if ( variableValues instanceof com.maplesoft.openmaple.List ) {
                com.maplesoft.openmaple.List variableList = (com.maplesoft.openmaple.List) variableValues;
                for ( int i = 0; i < variableList.length(); i++ ) {
                    Algebraic varValPair = variableList.get(i);
                    String[] varValPairArr = varValPair.toString().split(" = ");
                    if ( varValPairArr.length != 2 ) continue;
                    varValMap.put(varValPairArr[0], varValPairArr[1]);
                }
            }

            return numericCalculation;
        } catch (ComputerAlgebraSystemEngineException | MapleException e) {
            LOG.warn("Unable to analyze the single result " + singleResult.toString(), e);
            return null;
        }
    }

    private TestResultType getStatusOfList(com.maplesoft.openmaple.List aList) throws MapleException {
        // if l == 0, the list is empty so the test was successful
        if ( aList.length() == 0 ){
            LOG.info("Test was successful.");
            return TestResultType.SUCCESS;
        }

        // otherwise the list contains errors or simple failures
        LOG.info("Test was NOT successful.");
        boolean allError = true;
        for ( int i = 0; i < aList.length(); i++ ) {
            if ( !aList.get(i).toString().contains("Error") ) allError = false;
        }

        if ( allError && aList.length() == numberOfTestCases ) {
            return TestResultType.ERROR;
        } else return TestResultType.FAILURE;
    }

    @Override
    public int getPerformedTestCases() {
        return numberOfTestCases;
    }

    @Override
    public int getNumberOfFailedTestCases() {
        return numberOfFailedCases;
    }

    @Override
    public String generateNumericTestExpression(String input) {
        return generateNumericCalculationExpression(input);
    }

    public static String generateNumericCalculationExpression(String input) {
        return "evalf(" + input + ")";
    }

    @Override
    public boolean wasAborted(Algebraic result) {
        return maple.isAbortedExpression(result);
    }

    private void checkValues( Algebraic nTestValsA, String valsName ) throws MapleException, IllegalArgumentException {
        if (nTestValsA.isNULL()) {
            if ( checkNumericalNTest() ){
                // in this case, numResults is Null but nTest is numerical
                // continue normal work by reset numResults to an empty array
                maple.evaluate( valsName+" := [];" );
                numberOfTestCases = 0;
                return;
            }
            throw new IllegalArgumentException("There are no valid test values.");
        }

        if ( nTestValsA instanceof com.maplesoft.openmaple.List){
            checkTestValuesList((com.maplesoft.openmaple.List) nTestValsA, valsName);
        }
    }

    private void checkTestValuesList(com.maplesoft.openmaple.List list, String valsName) throws MapleException {
        if (checkNumericalNTest()){
            maple.evaluate( valsName+" := [];" );
            numberOfTestCases = 0;
            LOG.info("No values test case.");
            return;
        }

        numberOfTestCases = list.length();
        LOG.info("First couple [of "+numberOfTestCases+"] generated test cases: " + list.subList(0, Math.min(5, list.length())));

        int length = list.length();
        if ( length <= 0 ){
            // else throw an exception
            throw new IllegalArgumentException("There are no valid test values.");
        }

        String values = list.toString();
        int min = Math.min(values.length(), MAX_LOG_LENGTH);
        if ( min < values.length() )
            values = values.substring(1, min) + "...";
//        LOG.info("Testing " + list.length() + " values: " + values);
    }

    private boolean checkNumericalNTest() throws MapleException {
        Algebraic numericalCheck = maple.evaluate("nTest;");
        return numericalCheck instanceof Numeric;
    }
}
