package gov.nist.drmf.interpreter;

import com.maplesoft.externalcall.MapleException;
import com.maplesoft.openmaple.Algebraic;
import com.maplesoft.openmaple.List;
import com.maplesoft.openmaple.MString;
import com.maplesoft.openmaple.Numeric;
import gov.nist.drmf.interpreter.common.exceptions.ComputerAlgebraSystemEngineException;
import gov.nist.drmf.interpreter.evaluation.numeric.ICASEngineNumericalEvaluator;
import gov.nist.drmf.interpreter.evaluation.symbolic.ICASEngineSymbolicEvaluator;
import gov.nist.drmf.interpreter.evaluation.numeric.NumericalEvaluator;
import gov.nist.drmf.interpreter.maple.listener.MapleListener;
import gov.nist.drmf.interpreter.maple.translation.MapleInterface;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static gov.nist.drmf.interpreter.examples.MLP.NL;

/**
 *
 * Created by AndreG-P on 27.04.2017.
 */
public class MapleSimplifier implements ICASEngineSymbolicEvaluator<Algebraic>, ICASEngineNumericalEvaluator<Algebraic> {
    private static final Logger LOG = LogManager.getLogger(MapleSimplifier.class.toString());

    /**
     * This zero pattern allows expressions such as
     *  0 or 0. or 0.0 or 0.000 and so on.
     */
    private static final String ZERO_PATTERN = "0\\.?0*";

    private static MapleInterface mapleInterface;
    private static MapleListener mapleListener;

    private static final double INTERRUPTER_THRESHOLD = 50;

    MapleSimplifier( MapleInterface mapleInterface ){
        MapleSimplifier.mapleInterface = mapleInterface;
        MapleSimplifier.mapleListener = mapleInterface.getUniqueMapleListener();
        //this.mapleListener.activateAutoInterrupt( INTERRUPTER_THRESHOLD );
    }

    /**
     * This method takes two maple expressions and returns true when both expression
     * are symbolically the same. To verify this, we use the "simplify" command from
     * Maple. Be aware that both expressions still can be mathematically equivalent
     * even when this method returns false!
     *
     * Be also aware that null inputs always returns false, even when both inputs are null.
     * However, two empty expression such as "" and "" returns true.
     *
     * @param exp1 Maple string of the first expression
     * @param exp2 Maple string of the second expression
     * @return true if both expressions are symbolically equivalent or false otherwise.
     *          If it returns false, both expressions still can be mathematically equivalent!
     * @throws MapleException If the test of equivalence produces an Maple error.
     */
    public boolean isEquivalent( @Nullable String exp1, @Nullable String exp2 )
            throws ComputerAlgebraSystemEngineException {
        if ( isNullOrEmpty(exp1, exp2) ) return false;

        // otherwise build simplify command to test equivalence
        String command = "(" + exp1 + ") - (" + exp2 + ")";
        Algebraic a = simplify( command );
        try {
            return isZero(a);
        } catch ( MapleException me ) {
            throw new ComputerAlgebraSystemEngineException(me);
        }
    }

    public Algebraic isMultipleEquivalent( @Nullable String exp1, @Nullable String exp2 )
            throws ComputerAlgebraSystemEngineException {
        if ( isNullOrEmpty(exp1, exp2) ) return null;

        // otherwise build simplify command to test equivalence
        String command = "(" + exp1 + ") / (" + exp2 + ")";
        Algebraic a = simplify( command );
        return a;
    }

    /**
     * This method takes two maple expressions and converts the difference
     * to the specified function before it tries to simplify the difference.
     *
     * It works exactly in the same way as {@link #isEquivalent(String, String)},
     * but converts the difference of {@param exp1} and {@param exp2} before it tries
     * to simplify the new expression.
     *
     * @param exp1 Maple string of the first expression
     * @param exp2 Maple string of the second expression
     * @param conversion Specified the destination of the conversion. For example, "expe" or "hypergeom".
     * @return true if both expressions are symbolically equivalent or false otherwise.
     *          If it returns false, both expressions still can be mathematically equivalent!
     * @throws MapleException If the test of equivalence produces an Maple error.
     */
    public boolean isEquivalentWithConversion(
            @Nullable String exp1,
            @Nullable String exp2,
            @Nonnull String conversion )
            throws ComputerAlgebraSystemEngineException, MapleException {
        if ( isNullOrEmpty(exp1, exp2) ) return false;

        // otherwise build simplify command to test equivalence
        String command = "convert((" + exp1 + ") - (" + exp2 + "),"+ conversion +")";
        Algebraic a = simplify( command );
        return isZero(a);
    }

    public Algebraic isMultipleEquivalentWithConversion(
            @Nullable String exp1,
            @Nullable String exp2,
            @Nonnull String conversion )
            throws ComputerAlgebraSystemEngineException{
        if ( isNullOrEmpty(exp1, exp2) ) return null;

        // otherwise build simplify command to test equivalence
        String command = "convert((" + exp1 + ") / (" + exp2 + "),"+ conversion +")";
        return simplify( command );
    }

    public boolean isEquivalentWithExpension(
            @Nullable String exp1,
            @Nullable String exp2,
            @Nullable String conversion
    ) throws ComputerAlgebraSystemEngineException, MapleException {
        if ( isNullOrEmpty(exp1, exp2) ) return false;

        // otherwise build simplify command to test equivalence
        String command = "expand((" + exp1 + ") - (" + exp2 + ")";
        command += conversion == null ? ")" : "," + conversion + ")";
        Algebraic a = simplify( command );
        return isZero(a);
    }

    public Algebraic isMultipleEquivalentWithExpension(
            @Nullable String exp1,
            @Nullable String exp2,
            @Nullable String conversion
    ) throws ComputerAlgebraSystemEngineException {
        if ( isNullOrEmpty(exp1, exp2) ) return null;

        // otherwise build simplify command to test equivalence
        String command = "expand((" + exp1 + ") / (" + exp2 + ")";
        command += conversion == null ? ")" : "," + conversion + ")";
        return simplify( command );
    }

    /**
     * Simplify given expression. Be aware, the given expression should not
     * end with ';'.
     * @param maple_expr given maple expression, without ';'
     * @return the algebraic object of the result of simplify(maple_expr);
     * @throws MapleException if the given expression cannot be evaluated.
     * @see Algebraic
     */
    public Algebraic mapleSimplify( String maple_expr ) throws MapleException {
        String command = "simplify(" + maple_expr + ");";
        LOG.debug("Simplification: " + command);
        mapleListener.timerReset();
        return mapleInterface.evaluateExpression( command );
    }

    @Override
    public Algebraic simplify( String input ) throws ComputerAlgebraSystemEngineException {
        try {
            return mapleSimplify(input);
        } catch ( MapleException me ) {
            throw new ComputerAlgebraSystemEngineException(me);
        }
    }

    @Override
    public Algebraic simplify( String input, String assumption ) throws ComputerAlgebraSystemEngineException {
        try {
            String cmd = "simplify(" + input + ") assuming " + assumption + ";";
            LOG.debug("Simplification: " + cmd);
            mapleListener.timerReset();
            return mapleInterface.evaluateExpression( cmd );
        } catch ( MapleException me ) {
            throw new ComputerAlgebraSystemEngineException(me);
        }
    }

    @Override
    public boolean isAsExpected( Algebraic in, String expect ) {
        String str = in.toString();
        if ( expect == null ){
            try {
                double d = Double.parseDouble(str);
                return true;
            } catch ( NumberFormatException nfe ) {};
//            if ( in instanceof Numeric ){
//                return true;
//                success[i] = true;
//                successStr[i] = type[i].getShortName() + ": " + aStr;
            return false;
            //else {
//                successStr[i] = type[i].getShortName() + ": NaN";
//            }
        } else if ( str.matches(expect) ) {
            return true;
        } else {
            return false;
        }

//            success[i] = true;
//            successStr[i] = type[i].getShortName() + ": Success";
//        } else {
//            successStr[i] = type[i].getShortName() + ": NaN";
//        }
    }

    @Override
    public void abort() throws ComputerAlgebraSystemEngineException {
        LOG.warn("Abortion is not supported by Maple.");
    }

    @Override
    public boolean wasAborted(Algebraic result) {
        return false; // there is no such a signal in Maple?
    }

    public Algebraic numericalMagic(String maple_expr ) throws MapleException {
        String command = "nTest := " + maple_expr + ":";
        command += "nVars := indets(nTest,name) minus {constants}:";
        command += "nVals := [-3/2, -1, -1/2, 0, 1/2, 1, 3/2]:";
        command += "nTestVals := createListInList(nVars,nVals):";
        LOG.debug("NumericalMagic: " + command);
        mapleInterface.evaluateExpression( command );

        command = "NumericalTester(nTest,nTestVals,0.0001,15);";
        LOG.debug("Start numerical test: " + command);
        return mapleInterface.evaluateExpression( command );
    }


    private StringBuffer commandsList;

    public static String makeMapleSet(java.util.List<String> els) {
        String s = makeListWithDelimiter(els);
        return "{"+s+"}";
    }

    public static String makeMapleList(java.util.List<String> els ) {
        String s = makeListWithDelimiter(els);
        return "["+s+"]";
    }

    public static String makeListWithDelimiter(java.util.List<String> els) {
        StringBuilder sb = new StringBuilder();
        sb.append(els.get(0));
        for ( int i = 1; i < els.size(); i++ ) {
            sb.append(", ").append(els.get(i));
        }
        return sb.toString();
    }

    @Override
    public String storeVariables(String expression, java.util.List<String> testValues) {
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
    public String storeConstraintVariables(String variableName, java.util.List<String> constraintVariables, java.util.List<String> constraintValues) {
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
    public String storeExtraVariables(String variableName, java.util.List<String> extraVariables, java.util.List<String> extraValues) {
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
    public String setConstraints(java.util.List<String> constraints) {
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
            Algebraic numCombis = mapleInterface.evaluateExpression(commandsList.toString());
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
    )
            throws ComputerAlgebraSystemEngineException {
        try {
            LOG.debug("Perform numerical test:" + NL + commandsList.toString());

            Algebraic testCases = mapleInterface
                    .evaluateExpression(
                            commandsList.toString() + NL + testCasesName + ";"
                    );

            LOG.debug("Entered numerical preparations.");
            checkValues(testCases, testCasesName);

            String numericalTest = "numResults := SpecialNumericalTester(nTest,"+testCasesName+","+precision+");";
            LOG.debug("Start numerical evaluation: " + numericalTest);

            Algebraic results = mapleInterface.evaluateExpression(numericalTest);
            if ( postProcessingMethodName != null &&
                    !postProcessingMethodName.isEmpty() ) {
                return mapleInterface.evaluateExpression(
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

    public String advancedNumericalTest(
            String maple_expr,
            String values,
            String constraintVariablesList,
            String constraintVariablesValues,
            String extraVariables,
            String extraVariablesValues,
            String constraints,
            int precision,
            int maxCombinations )
            throws MapleException, IllegalArgumentException {
        String command = buildCommandTestValues(
                maple_expr,
                values,
                constraintVariablesList,
                constraintVariablesValues,
                extraVariables,
                extraVariablesValues,
                constraints,
                maxCombinations );
        LOG.trace("Run: " + command);
        Algebraic nTestValsA = mapleInterface.evaluateExpression( command + NL + "nTestVals;" );
        checkValues(nTestValsA,"nTestVals");

        command = "numResults := SpecialNumericalTester(nTest,nTestVals," + precision + ");";
        LOG.debug("Start numerical test.");
        LOG.trace(command);
        Algebraic numResults = mapleInterface.evaluateExpression( command );
        logResults(numResults);

        return "numResults";
    }

    private void logResults(Algebraic a){
        String out = a.toString();
        String postfix = out.length() > NumericalEvaluator.MAX_LOG_LENGTH ? "..." : "";
        out = out.substring(0, Math.min(out.length(), NumericalEvaluator.MAX_LOG_LENGTH));
        out += postfix;
        LOG.info("Test results: " + out);
        LOG.trace("NumResults: " + a.toString());
    }

    private void checkValues( Algebraic nTestValsA, String valsName ) throws MapleException, IllegalArgumentException {
        if (nTestValsA.isNULL()) {
            if ( checkNumericalNTest() ){
                // in this case, numResults is Null but nTest is numerical
                // continue normal work by reset numResults to an empty array
                mapleInterface.evaluateExpression( valsName+" := [];" );
                return;
            }
            throw new IllegalArgumentException("There are no valid test values.");
        }

        if ( nTestValsA instanceof List ){
            if (checkNumericalNTest()){
                mapleInterface.evaluateExpression( valsName+" := [];" );
                return;
            }

            List l = (List) nTestValsA;
            int length = l.length();
            if ( length <= 0 ){
                // else throw an exception
                throw new IllegalArgumentException("There are no valid test values.");
            } else {
                String values = l.toString();
                int min = Math.min(values.length(), NumericalEvaluator.MAX_LOG_LENGTH);
                if ( min < values.length() )
                    values = values.substring(1, min) + "...";
                LOG.info("Testing " + l.length() + " values: " + values);
            }
        }
    }

    private boolean checkNumericalNTest() throws MapleException {
        Algebraic numericalCheck = mapleInterface.evaluateExpression("nTest;");
        return numericalCheck instanceof Numeric;
    }

    private String buildCommandTestValues (
            String maple_expr,
            String values,
            String constraintVariablesList,
            String constraintVariablesValues,
            String extraVariables,
            String extraVariablesValues,
            String constraints,
            int maxCombinations )
            throws MapleException, IllegalArgumentException {
        String command = "nTest := " + maple_expr + ":" + NL;

        LOG.debug("Numerical Test Expression: " + command);

        command += "nVars := myIndets(nTest):" + NL;
        command += "nVals := " + values + ":" + NL;

        if ( constraintVariablesList != null ){
            LOG.debug("Special Variable-Value pairs: " + constraintVariablesList + " with " + constraintVariablesValues);
            command += "nConstVarsL := " + constraintVariablesList + ":" + NL;
            command += "nConstVals  := " + constraintVariablesValues + ":" + NL;

            command += "nVars := nVars minus (indets(nConstVarsL,name) minus {constants}):" + NL;
        }

        String combis = "inCombis := nops(nVals)^nops(nVars)";

        if ( extraVariables != null ){
            LOG.debug("Treat special variables with special values if left in nVars.");
            command += "nSpecialVars := nVars intersect " + extraVariables + ":" + NL;
            command += "nSpecialVals := " + extraVariablesValues + ":" + NL;
            command += "nVars := nVars minus nSpecialVars:" + NL;
            combis += " + nops(nSpecialVals)^nops(nSpecialVars)";
        }

        // Test until here and look for number of variables
        command += combis + ":";
        LOG.debug("Calculate number of combinations.");
        LOG.trace(command);
        mapleInterface.evaluateExpression( command );

        Algebraic numOfCombis = mapleInterface.evaluateExpression("inCombis;");
        try {
            int i = Integer.parseInt(numOfCombis.toString());
            if ( i >= maxCombinations ) throw new IllegalArgumentException("Too many combinations: " + i);
        } catch ( NumberFormatException e ){
            throw new IllegalArgumentException("Cannot calculate number of combinations!");
        }

        // setup test values
        command = "nTestVals := [op(createListInList(nVars,nVals))";
        if ( extraVariables != null )
            command += ", op(createListInList(nSpecialVars,nSpecialVals))";
        if ( constraintVariablesList != null )
            command += ", specialVariables(nConstVarsL, nConstVals)";
        command += "]:" + NL;

        if ( constraints != null ){
            LOG.debug("Setup constraints for this test case.");
            command += "constraints := " + constraints + ":" + NL;
        } else {
            command += "constraints := []:" + NL;
        }

        // time to finally calculate set of test values by filter invalid combinations
        command += "nTestVals := buildTestValues(constraints,nTestVals):";
        return command;
    }

    /**
     *
     * @param expr
     * @return
     */
    public RelationResults holdsRelation( @Nullable String expr ) throws MapleException {
        try {
            String command = "op(1, ToInert(is(" + expr + ")));";
            mapleListener.timerReset();
            Algebraic a = mapleInterface.evaluateExpression( command );
            if ( !(a instanceof MString) ) return RelationResults.ERROR;

            MString ms = (MString) a;
            String s = ms.stringValue();
            if ( s.equals("true") ) return RelationResults.TRUE;
            if ( s.equals("false") ) return RelationResults.FALSE;
            if ( s.equals("FAIL") ) return RelationResults.FAIL;
            return RelationResults.ERROR;
        } catch ( MapleException me ){
            return RelationResults.ERROR;
        }
    }

    /**
     * Checks if the given algebraic object is 0.
     * @param a an algebraic object
     * @return true if the result is 0. False otherwise.
     * @throws MapleException if the given command produces an error in Maple.
     */
    private boolean isZero( Algebraic a ) throws MapleException {
        // null solutions returns false
        if ( a == null || a.isNULL() ) return false;
        // analyze the output string and returns true when it matches "0".
        String solution_str = a.toString();
        return solution_str.trim().matches(ZERO_PATTERN);
    }

    /**
     * If one of them is null, returns true.
     * If none is null but one of them is empty, it returns true
     * when both are empty, otherwise false.
     * Otherwise returns false.
     * @param exp1 string
     * @param exp2 string
     * @return true or false
     */
    private boolean isNullOrEmpty( String exp1, String exp2 ){
        // test if one of the inputs is null
        if ( exp1 == null || exp2 == null ) return true;
        // if one of the expressions is empty, it only returns true when both are empty
        if ( exp1.isEmpty() || exp2.isEmpty() ){
            return !(exp1.isEmpty() && exp2.isEmpty());
        }
        return false;
    }

    @Override
    public String generateNumericalTestExpression(String input) {
        return "evalf(" + input + ")";
    }
}
