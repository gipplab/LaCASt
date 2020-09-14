package gov.nist.drmf.interpreter.maple.extension;

import com.maplesoft.externalcall.MapleException;
import com.maplesoft.openmaple.Algebraic;
import com.maplesoft.openmaple.MString;
import com.maplesoft.openmaple.Numeric;
import gov.nist.drmf.interpreter.common.cas.ICASEngineNumericalEvaluator;
import gov.nist.drmf.interpreter.common.exceptions.ComputerAlgebraSystemEngineException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Observable;

import static gov.nist.drmf.interpreter.common.constants.GlobalConstants.NL;
import static gov.nist.drmf.interpreter.maple.extension.CommandBuilder.makeMapleList;
import static gov.nist.drmf.interpreter.maple.extension.CommandBuilder.makeMapleSet;

/**
 * @author Andre Greiner-Petter
 */
public class NumericCalculator implements ICASEngineNumericalEvaluator<Algebraic> {
    private static final Logger LOG = LogManager.getLogger(NumericCalculator.class.getName());

    public static final int MAX_LOG_LENGTH = 300;

    private final MapleInterface maple;

    private StringBuffer commandsList;

    private final String testExpression = "nTest";
    private final String varNames = "nVars";
    private final String conVarN = "nConstVars";
    private final String specVarN = "nSpecialVars";

    private boolean conVarSet = false;
    private boolean specVarSet = false;

    private double timeLimit = -1;
    private boolean timedOutBySetup = false;

    public NumericCalculator() {
        maple = MapleInterface.getUniqueMapleInterface();
        commandsList = new StringBuffer();
    }

    @Override
    public void setTimeout(double timeLimit) {
        this.timeLimit = timeLimit;
    }

    @Override
    public void storeVariables(String expression, List<String> testValues) {
        // reset commandsList
        commandsList = new StringBuffer();
        timedOutBySetup = false;

        if ( timeLimit > 0 ) {
            tryTimeOutExpression(commandsList, timeLimit, expression);
        } else {
            commandsList.append(testExpression).append(" := ")
                    .append(expression).append(":").append(NL);
        }

        commandsList
                .append(varNames)
                .append(":=myIndets(nTest):").append(NL);

        String varValues = ICASEngineNumericalEvaluator.getValuesName(varNames);
        commandsList
                .append(varValues)
                .append(":=")
                .append(makeMapleList(testValues))
                .append(":").append(NL);
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

        commandsList.append(conVarN).append(":=").append(makeMapleList(constraintVariables)).append(":").append(NL);
        commandsList.append(conValsN).append(":=").append(makeMapleList(constraintValues)).append(":").append(NL);

        // update vars list
        commandsList
                .append(varNames).append(":=")
                .append(varNames).append(" minus (indets(").append(conVarN).append(",name) minus {constants}):")
                .append(NL);

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
        String consN = "constraints";
        commandsList.append(consN).append(":=");

        if ( constraints == null || constraints.isEmpty() )
            commandsList.append("[]:");
        else commandsList.append(makeMapleList(constraints)).append(":");

        commandsList.append(NL);

        return consN;
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
        combis += "-1;";
        commandsList.append(combis).append(NL);

        maple.evaluateAndCheckRangeOfResult(commandsList.toString(), 0, maxCombis);
        commandsList = new StringBuffer();
        commandsList.append(testValuesN).append(":= buildTestValues(")
                .append(constraintsName).append(",").append(testValuesN).append("):");

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
        try {
            LOG.debug("Perform numerical test:" + NL + commandsList.toString());

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
                return maple.evaluate(
                        postProcessingMethodName + "(numResults);"
                );
            } else return results;
        } catch (MapleException e) {
            throw new ComputerAlgebraSystemEngineException(e);
        }
    }

    @Override
    public ResultType getStatusOfResult(Algebraic results) throws ComputerAlgebraSystemEngineException {
        try {
            if ( results instanceof com.maplesoft.openmaple.List ) {
                return getStatusOfList((com.maplesoft.openmaple.List) results);
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
        if ( aList.toString().contains("Error") ){
            return ResultType.ERROR;
        } else {
            return ResultType.FAILURE;
        }
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
            return;
        }

        int length = list.length();
        if ( length <= 0 ){
            // else throw an exception
            throw new IllegalArgumentException("There are no valid test values.");
        }

        String values = list.toString();
        int min = Math.min(values.length(), MAX_LOG_LENGTH);
        if ( min < values.length() )
            values = values.substring(1, min) + "...";
        LOG.info("Testing " + list.length() + " values: " + values);
    }

    private boolean checkNumericalNTest() throws MapleException {
        Algebraic numericalCheck = maple.evaluate("nTest;");
        return numericalCheck instanceof Numeric;
    }
}
