package gov.nist.drmf.interpreter.maple.extension;

import com.maplesoft.externalcall.MapleException;
import com.maplesoft.openmaple.Algebraic;
import com.maplesoft.openmaple.Numeric;
import gov.nist.drmf.interpreter.common.cas.ICASEngineNumericalEvaluator;
import gov.nist.drmf.interpreter.common.exceptions.ComputerAlgebraSystemEngineException;
import gov.nist.drmf.interpreter.maple.listener.MapleListener;
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
    private final MapleListener listener;

    private StringBuffer commandsList;

    public NumericCalculator() {
        maple = MapleInterface.getUniqueMapleInterface();
        listener = MapleInterface.getUniqueMapleListener();
        commandsList = new StringBuffer();
    }

    @Override
    public String storeVariables(String expression, List<String> testValues) {
        // reset commandsList
        commandsList = new StringBuffer();

        commandsList.append("nTest := ").append(expression).append(":").append(NL);

        String varNames = "nVars";
        commandsList
                .append(varNames)
                .append(":=myIndets(")
                .append(expression)
                .append("):").append(NL);

        String varValues = ICASEngineNumericalEvaluator.getValuesName(varNames);
        commandsList
                .append(varValues)
                .append(":=")
                .append(makeMapleList(testValues))
                .append(":").append(NL);

        return varNames;
    }

    @Override
    public String storeConstraintVariables(String variableName, List<String> constraintVariables, List<String> constraintValues) {
        if ( constraintVariables == null || constraintVariables.isEmpty() )
            return null;

        String conVarN = "nConstVars";
        String conValsN = ICASEngineNumericalEvaluator.getValuesName(conVarN);

        commandsList.append(conVarN).append(":=").append(makeMapleList(constraintVariables)).append(":").append(NL);
        commandsList.append(conValsN).append(":=").append(makeMapleList(constraintValues)).append(":").append(NL);

        // update vars list
        commandsList
                .append(variableName).append(":=")
                .append(variableName).append(" minus (indets(").append(conVarN).append(",name) minus {constants}):")
                .append(NL);

        return conVarN;
    }

    @Override
    public String storeExtraVariables(String variableName, List<String> extraVariables, List<String> extraValues) {
        if ( extraVariables == null || extraVariables.isEmpty() )
            return null;

        String specVarN = "nSpecialVars";
        String specValsN = ICASEngineNumericalEvaluator.getValuesName(specVarN);

        commandsList.append(specVarN).append(":=")
                .append(variableName).append(" intersect ").append(makeMapleSet(extraVariables))
                .append(":").append(NL);
        commandsList.append(specValsN).append(":=").append(makeMapleList(extraValues)).append(":").append(NL);

        // update vars list
        commandsList
                .append(variableName).append(":=")
                .append(variableName).append(" minus (indets(").append(specVarN).append(",name) minus {constants}):")
                .append(NL);

        return specVarN;
    }

    @Override
    public String setConstraints(List<String> constraints) {
        String consN = "constraints";

        commandsList.append(consN).append(":=");

        if ( constraints == null || constraints.isEmpty() )
            commandsList.append("[]:");
        else commandsList.append(makeMapleList(constraints));

        commandsList.append(NL);

        return consN;
    }

    @Override
    public String buildTestCases(
            String constraintsName,
            String variableNames,
            String constraintVariableNames,
            String extraVariableNames,
            int maxCombis
    ) throws ComputerAlgebraSystemEngineException, IllegalArgumentException {
        String testValuesN = "nTestVals";

        String vals = ICASEngineNumericalEvaluator.getValuesName(variableNames);
        commandsList.append(testValuesN).append(":= [op(createListInList(")
                .append(variableNames).append(",").append(vals).append("))");

        String combis = "inCombis := nops("+vals+")^nops("+variableNames+")";

        if ( extraVariableNames != null ) {
            String extVals = ICASEngineNumericalEvaluator.getValuesName(extraVariableNames);
            commandsList.append(", op(createListInList(")
                    .append(extraVariableNames).append(",").append(extVals).append("))");
            combis += " * nops("+extVals+")^nops("+extraVariableNames+")";
        }

        if ( constraintVariableNames != null ) {
            String conVals = ICASEngineNumericalEvaluator.getValuesName(constraintVariableNames);
            commandsList.append(", specialVariables(")
                    .append(constraintVariableNames).append(",").append(conVals).append(")");
        }

        commandsList.append("]:").append(NL);
        combis += ";";
        commandsList.append(combis).append(NL);

        try {
            LOG.debug("Prepare numerical test:" + NL + commandsList.toString());
            Algebraic numCombis = MapleInterface.getUniqueMapleInterface().evaluate(commandsList.toString());
            try {
                int i = Integer.parseInt(numCombis.toString());
                if ( i >= maxCombis ) throw new IllegalArgumentException("Too many combinations: " + i);
            } catch ( NumberFormatException e ){
                throw new IllegalArgumentException("Cannot calculate number of combinations!");
            }
        } catch (MapleException me) {
            throw new ComputerAlgebraSystemEngineException(me);
        }

        commandsList = new StringBuffer();
        commandsList.append(testValuesN).append(":= buildTestValues(")
                .append(constraintsName).append(",").append(testValuesN).append("):");

        return testValuesN;
    }

    @Override
    public Algebraic performNumericalTests(
            String expression,
            String testCasesName,
            String postProcessingMethodName,
            int precision
    ) throws ComputerAlgebraSystemEngineException {
        try {
            LOG.debug("Perform numerical test:" + NL + commandsList.toString());

            Algebraic testCases = MapleInterface.getUniqueMapleInterface().evaluate(
                    commandsList.toString() + NL + testCasesName + ";"
            );

            LOG.debug("Entered numerical preparations.");
            checkValues(testCases, testCasesName);

            String numericalTest = "numResults := SpecialNumericalTester(nTest,"+testCasesName+","+precision+");";
            LOG.debug("Start numerical evaluation: " + numericalTest);

            Algebraic results = MapleInterface.getUniqueMapleInterface().evaluate(numericalTest);
            if ( postProcessingMethodName != null &&
                    !postProcessingMethodName.isEmpty() ) {
                return MapleInterface.getUniqueMapleInterface().evaluate(
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
                com.maplesoft.openmaple.List aList = (com.maplesoft.openmaple.List) results;
                int l = aList.length();

                // if l == 0, the list is empty so the test was successful
                if ( l == 0 ){
                    LOG.info("Test was successful.");
                    return ResultType.SUCCESS;
                } else { // otherwise the list contains errors or simple failures
                    LOG.info("Test was NOT successful.");
                    String evaluation = aList.toString();

                    if ( evaluation.contains("Error") ){
                        return ResultType.ERROR;
                    } else {
                        return ResultType.FAILURE;
                    }
                }
            } else {
                LOG.warn("Sieved list was not a list object... " + results.toString());
                return ResultType.ERROR;
            }
        } catch (MapleException me) {
            throw new ComputerAlgebraSystemEngineException(me);
        }
    }

    @Override
    public String generateNumericalTestExpression(String input) {
        return "evalf(" + input + ")";
    }

    @Override
    public void abort() {
        LOG.warn("Abortion is not supported by Maple.");
    }

    @Override
    public boolean wasAborted(Algebraic result) {
        return false;
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
                MapleInterface.getUniqueMapleInterface().evaluate( valsName+" := [];" );
                return;
            }
            throw new IllegalArgumentException("There are no valid test values.");
        }

        if ( nTestValsA instanceof com.maplesoft.openmaple.List){
            if (checkNumericalNTest()){
                MapleInterface.getUniqueMapleInterface().evaluate( valsName+" := [];" );
                return;
            }

            com.maplesoft.openmaple.List l = (com.maplesoft.openmaple.List) nTestValsA;
            int length = l.length();
            if ( length <= 0 ){
                // else throw an exception
                throw new IllegalArgumentException("There are no valid test values.");
            } else {
                String values = l.toString();
                int min = Math.min(values.length(), MAX_LOG_LENGTH);
                if ( min < values.length() )
                    values = values.substring(1, min) + "...";
                LOG.info("Testing " + l.length() + " values: " + values);
            }
        }
    }

    private boolean checkNumericalNTest() throws MapleException {
        Algebraic numericalCheck = MapleInterface.getUniqueMapleInterface().evaluate("nTest;");
        return numericalCheck instanceof Numeric;
    }
}
