package gov.nist.drmf.interpreter.common.cas;

import gov.nist.drmf.interpreter.common.exceptions.ComputerAlgebraSystemEngineException;
import gov.nist.drmf.interpreter.common.cas.IAbortEvaluator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Observer;

/**
 * @author Andre Greiner-Petter
 */
public interface ICASEngineNumericalEvaluator<T> extends Observer, IAbortEvaluator<T> {
    static final Logger LOG = LogManager.getLogger(ICASEngineNumericalEvaluator.class.getName());

    /**
     * Stores the variables of the given expression and returns the
     * name of the variable that stores the information.
     * @param expression mathematical expression (already translated)
     * @param testValues list of values
     * @return name of the variable to access the variables of the expression
     */
    int storeVariables(String expression, List<String> testValues);

    default void storeVariablesSave(String expression, String fallbackExpression, List<String> testValues) throws IllegalArgumentException {
        int num = storeVariables(expression, testValues);

        boolean isValid = num >= 0;
        if ( num <= 0 ) {
            LOG.debug("Switch extracting variables to the fallback expression.");
            num = storeVariables(fallbackExpression, testValues);
        }

        // if both times, num < 0, we are unable to extract variables => throw an exception
        // if first time was valid (>= 0) but no vars extracted, we try to extract from the fallback expression
        // only if both results are under 0 (invalid) we throw an exception, otherwise it may simply contain 0 vars...
        // e.g. 1=1 no vars but valid :)
        if ( !isValid && num < 0 ) {
            LOG.trace("Still unable to extract variables => throwing exception.");
            throw new IllegalArgumentException("Unable to extract variables.");
        }
    }

    /**
     * Stores the given constraint variables and their values.
     * It also updates the previously stored variables.
     * @param constraintVariables list of variables of the constraints (translated)
     * @param constraintValues the values for the constraint variables (ordered and translated)
     * @return the name of the variable that accesses the constraint variables
     */
    void storeConstraintVariables(
            List<String> constraintVariables,
            List<String> constraintValues);

    /**
     * Essentially the same as {@link #storeConstraintVariables(List, List)}.
     * @param extraVariables list of special treatment variables (translated)
     * @param extraValues the values for the variables (ordered and translated)
     * @return the name of the variable that accesses the special treatment variables
     */
    void storeExtraVariables(
            List<String> extraVariables,
            List<String> extraValues);

    /**
     * Sets constraints and returns the name of variables that is defined as the constraints.
     * It returns null of no constraints exists.
     * @param constraints constraints
     * @return name of variable or null
     */
    String setConstraints(List<String> constraints);

    /**
     * Builds the test cases and returns the name of variable.
     * @param maxCombis maximum number of combinations
     * @return name of test cases variable
     * @throws ComputerAlgebraSystemEngineException
     * @throws IllegalArgumentException
     */
    String buildTestCases(String nameOfConstraints, int maxCombis) throws ComputerAlgebraSystemEngineException, IllegalArgumentException;

    T performNumericalTests(
            String expression,
            String testCasesName,
            String postProcessingMethodName,
            int precision
    ) throws ComputerAlgebraSystemEngineException;

    ResultType getStatusOfResult(T results) throws ComputerAlgebraSystemEngineException;

    default int getPerformedTestCases() {
        return 0;
    }

    default int getNumberOfFailedTestCases() {
        return 0;
    }

    default void setGlobalAssumptions(List<String> assumptions){
        LOG.warn("Ignoring global assumptions. Overwrite setGlobalAssumptions if you wanna use them!");
    };

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
    static String getValuesName( String variableName ) {
        return variableName + "Vals";
    }

    enum ResultType {
        SUCCESS, FAILURE, ERROR
    }
}
