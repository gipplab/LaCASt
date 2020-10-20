package gov.nist.drmf.interpreter.maple.extension;

import com.maplesoft.externalcall.MapleException;
import com.maplesoft.openmaple.Algebraic;
import com.maplesoft.openmaple.MString;
import com.maplesoft.openmaple.Numeric;
import gov.nist.drmf.interpreter.common.cas.ICASEngineNumericalEvaluator;
import gov.nist.drmf.interpreter.common.cas.PackageWrapper;
import gov.nist.drmf.interpreter.common.constants.Keys;
import gov.nist.drmf.interpreter.common.exceptions.ComputerAlgebraSystemEngineException;
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
public class NumericCalculator implements ICASEngineNumericalEvaluator<Algebraic> {
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
        requiredPackages.clear();

        String varValues = ICASEngineNumericalEvaluator.getValuesName(varNames);
        String conValsN = ICASEngineNumericalEvaluator.getValuesName(conVarN);
        String specValsN = ICASEngineNumericalEvaluator.getValuesName(specVarN);

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
    public void setGlobalAssumptions(List<String> assumptions) {
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

        String varValues = ICASEngineNumericalEvaluator.getValuesName(varNames);
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

        String conValsN = ICASEngineNumericalEvaluator.getValuesName(conVarN);
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
        String specValsN = ICASEngineNumericalEvaluator.getValuesName(specVarN);

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

        String vals = ICASEngineNumericalEvaluator.getValuesName(varNames);
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
            LOG.info("Active constraints: " + a.toString());
        } catch (MapleException e) {
            LOG.error("Unable to check constraints", e);
        }

        commandsList = new StringBuffer();
        commandsList.append(testValuesN).append(":= buildTestValues(")
                .append(constraintsName).append(",").append(testValuesN).append("):").append(NL);
        return testValuesN;
    }

    private String additionalCalculations(String combis) {
        if ( specVarSet ) {
            String extVals = ICASEngineNumericalEvaluator.getValuesName(specVarN);
            commandsList.append(", op(createListInList(")
                    .append(specVarN).append(",").append(extVals).append("))");
            combis += " * nops("+extVals+")^nops("+specVarN+")";
        }

        if ( conVarSet ) {
            String conVals = ICASEngineNumericalEvaluator.getValuesName(conVarN);
            commandsList.append(", specialVariables(")
                    .append(conVarN).append(",").append(conVals).append(")");
        }

        return combis;
    }

    @Override
    public Algebraic performNumericalTests(
            String expression,
            String testCasesName,
            String postProcessingMethodName,
            int precision
    ) throws ComputerAlgebraSystemEngineException {
        if ( timedOutBySetup ) return null;

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

            Algebraic results = maple.evaluate(numericalTest);
            if ( wasAborted(results) ) return results;
            else if ( postProcessingMethodName != null && !postProcessingMethodName.isEmpty() ) {
                Algebraic result = maple.evaluate(
                        postProcessingMethodName + "(numResults);"
                );

                if ( result instanceof com.maplesoft.openmaple.List ) {
                    com.maplesoft.openmaple.List list = (com.maplesoft.openmaple.List) result;
                    LOG.info("First entries of result list [total: " + list.length() + "]: " + list.subList(0, Math.min(5, list.length())));
                }

                return result;
            } else return results;
        } catch (MapleException e) {
            throw new ComputerAlgebraSystemEngineException(e);
        } finally {
            unloadPackages();
        }
    }

    @Override
    public ResultType getStatusOfResult(Algebraic results) throws ComputerAlgebraSystemEngineException {
        try {
            if ( results instanceof com.maplesoft.openmaple.List ) {
                com.maplesoft.openmaple.List resList = (com.maplesoft.openmaple.List) results;
                numberOfFailedCases = resList.length();
                return getStatusOfList(resList);
            } else {
                LOG.warn("Sieved list was not a list object... " + results.toString());
                return ResultType.ERROR;
            }
        } catch (MapleException me) {
            throw new ComputerAlgebraSystemEngineException(me);
        }
    }

    private ResultType getStatusOfList(com.maplesoft.openmaple.List aList) throws MapleException {
        // if l == 0, the list is empty so the test was successful
        if ( aList.length() == 0 ){
            LOG.info("Test was successful.");
            return ResultType.SUCCESS;
        }

        // otherwise the list contains errors or simple failures
        LOG.info("Test was NOT successful.");
        boolean allError = true;
        for ( int i = 0; i < aList.length(); i++ ) {
            if ( !aList.get(i).toString().contains("Error") ) allError = false;
        }

        if ( allError && aList.length() == numberOfTestCases ) {
            return ResultType.ERROR;
        } else return ResultType.FAILURE;
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
    public String generateNumericalTestExpression(String input) {
        return "evalf(" + input + ")";
    }

    @Override
    public void abort() {
        LOG.warn("Maple supports abortion via timelimit when starting computation.");
    }

    @Override
    public boolean wasAborted(Algebraic result) {
        return maple.isAbortedExpression(result);
    }

    @Override
    public void update(Observable observable, Object o) {
        abort();
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
