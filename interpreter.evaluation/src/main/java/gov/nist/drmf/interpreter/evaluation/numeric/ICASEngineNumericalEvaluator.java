package gov.nist.drmf.interpreter.evaluation.numeric;

import gov.nist.drmf.interpreter.common.exceptions.ComputerAlgebraSystemEngineException;
import gov.nist.drmf.interpreter.common.IAbortEvaluator;

import java.util.List;
import java.util.Observer;

/**
 * @author Andre Greiner-Petter
 */
public interface ICASEngineNumericalEvaluator<T> extends Observer, IAbortEvaluator<T> {
    /**
     * Stores the variables of the given expression and returns the
     * name of the variable that stores the information.
     * @param expression mathematical expression (already translated)
     * @param testValues list of values
     * @return name of the variable to access the variables of the expression
     */
    String storeVariables(String expression, List<String> testValues);

    /**
     * Stores the given constraint variables and their values.
     * It also updates the previously stored variables.
     * @param variableName the returned value of {@link #storeVariables(String, List)}
     * @param constraintVariables list of variables of the constraints (translated)
     * @param constraintValues the values for the constraint variables (ordered and translated)
     * @return the name of the variable that accesses the constraint variables
     */
    String storeConstraintVariables(
            String variableName,
            List<String> constraintVariables,
            List<String> constraintValues);

    /**
     * Essentially the same as {@link #storeConstraintVariables(String, List, List)}.
     * @param variableName the returned value of {@link #storeVariables(String, List)}
     * @param extraVariables list of special treatment variables (translated)
     * @param extraValues the values for the variables (ordered and translated)
     * @return the name of the variable that accesses the special treatment variables
     */
    String storeExtraVariables(
            String variableName,
            List<String> extraVariables,
            List<String> extraValues);

    String setConstraints(List<String> constraints);

    String buildTestCases(
            String constraintsName,
            String variableNames,
            String constraintVariableNames,
            String extraVariableNames,
            int maxCombis
    ) throws ComputerAlgebraSystemEngineException, IllegalArgumentException;

    T performNumericalTests(
            String expression,
            String testCasesName,
            String postProcessingMethodName,
            int precision
    ) throws ComputerAlgebraSystemEngineException;

    ResultType getStatusOfResult(T results) throws ComputerAlgebraSystemEngineException;

    /**
     * In Maple its evalf( input );
     * @param input
     * @return
     */
    String generateNumericalTestExpression(String input);

    /**
     * Returns the name of the values-variable for the variable names.
     * @param variableName when vars is the variable that holds the variables,
     *                     you can call this method with {@param variableName} vars
     *                     to get the according variable name of the values.
     * @return name of values
     */
    public static String getValuesName( String variableName ) {
        return variableName + "Vals";
    }

    public enum ResultType {
        SUCCESS, FAILURE, ERROR
    }
}
