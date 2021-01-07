package gov.nist.drmf.interpreter.common.cas;

import gov.nist.drmf.interpreter.common.eval.NumericalTest;
import gov.nist.drmf.interpreter.common.eval.TestResultType;
import gov.nist.drmf.interpreter.common.exceptions.ComputerAlgebraSystemEngineException;
import gov.nist.drmf.interpreter.common.cas.IAbortEvaluator;
import gov.nist.drmf.interpreter.common.pojo.NumericCalculation;
import gov.nist.drmf.interpreter.common.pojo.NumericResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

/**
 * @author Andre Greiner-Petter
 */
public interface ICASEngineNumericalEvaluator<T> extends IAbortEvaluator<T> {
    Logger LOG = LogManager.getLogger(ICASEngineNumericalEvaluator.class.getName());

    /**
     * Sets global assumptions that will be applied to all following tests.
     * @param assumptions list of assumptions
     */
    default void setGlobalAssumptions(List<String> assumptions) {
        LOG.warn("Ignoring global assumptions. Overwrite setGlobalAssumptions if you wanna use them!");
    }

    /**
     * Stores the variables of the given expression and returns the
     * name of the variable that stores the information.
     *
     * @param variables  mathematical expression (already translated)
     * @param testValues list of values
     */
    void storeVariables(Collection<String> variables, Collection<String> testValues);

    /**
     * Stores the given constraint variables and their values.
     * It also updates the previously stored variables.
     *
     * @param constraintVariables list of variables of the constraints (translated)
     * @param constraintValues    the values for the constraint variables (ordered and translated)
     */
    void storeConstraintVariables(
            List<String> constraintVariables,
            List<String> constraintValues);

    /**
     * Essentially the same as {@link #storeConstraintVariables(List, List)}.
     *
     * @param extraVariables list of special treatment variables (translated)
     * @param extraValues    the values for the variables (ordered and translated)
     * @return the name of the variable that accesses the special treatment variables
     */
    void storeExtraVariables(
            List<String> extraVariables,
            List<String> extraValues);

    /**
     * Sets constraints and returns the name of variables that is defined as the constraints.
     * It returns null of no constraints exists.
     *
     * @param constraints constraints
     * @return name of variable or null
     */
    String setConstraints(List<String> constraints);

    /**
     * Builds the test cases and returns the name of variable.
     *
     * @param maxCombis maximum number of combinations
     * @return name of test cases variable
     * @throws ComputerAlgebraSystemEngineException
     * @throws IllegalArgumentException
     */
    String buildTestCases(String nameOfConstraints, int maxCombis) throws ComputerAlgebraSystemEngineException, IllegalArgumentException;

    /**
     * Returns true if this engine requires to register packages. If this is true
     * you should implement and use {@link #addRequiredPackages(Set)}.
     *
     * @return true if this engine requires to register packages
     */
    default boolean requiresRegisteredPackages() {
        return false;
    }

    /**
     * Is ignored by default. You should implement this method alongside with
     * {@link #requiresRegisteredPackages()}.
     *
     * @param packages the packages to register
     */
    default void addRequiredPackages(Set<String> packages) {
        // nothing to do here...
    }

    /**
     * The last part in the chain of a setup for numerical test. See {@link #performNumericalTest(NumericalTest)} to
     * see the entire process of setting up a numeric test.
     * @param expression the expression that should be tested (e.g. lhs-rhs)
     * @param testCasesName the variable name of the stored test cases returned by {@link #buildTestCases(String, int)}
     * @param postProcessingMethodName the process to call after the test was performed (for post processing)
     * @param precision the precision the numerical test should use
     * @return the original CAS result. Use {@link #wasAborted(Object)} and {@link #getStatusOfResult(Object)} to analyze
     * the result or call {@link #getNumericCalculationList(Object)} to get an object list of the result
     * @throws ComputerAlgebraSystemEngineException if something in the CAS went wrong during the test
     */
    T performGeneratedTestOnExpression(
            String expression,
            String testCasesName,
            String postProcessingMethodName,
            int precision
    ) throws ComputerAlgebraSystemEngineException;

    /**
     * Performs the numerical test at once by the given test object.
     * This means it:
     * 1) sets up the variables {@link #storeVariables(Collection, Collection)}
     * 2) sets up constraint variables {@link #storeConstraintVariables(List, List)}
     * 3) sets up extra variables {@link #storeExtraVariables(List, List)}
     * 4) sets the constraints {@link #setConstraints(List)}
     * 5) builds the test cases {@link #buildTestCases(String, int)}
     * 6) and performs the test finally {@link #performGeneratedTestOnExpression(String, String, String, int)}.
     *
     * @param test the test case
     * @return the result
     * @throws ComputerAlgebraSystemEngineException if an error was thrown
     */
    default T performNumericalTest(NumericalTest test)
            throws ComputerAlgebraSystemEngineException {
        // store variables first
        storeVariables(
                test.getVariables(),
                test.getTestValues()
        );

        // next, store constraint variables extracted from blueprints
        storeConstraintVariables(
                test.getConstraintVariables(),
                test.getConstraintVariablesValues()
        );

        // next, store special variables (such as k should be integer)
        storeExtraVariables(
                test.getExtraVariables(),
                test.getExtraVariablesValues()
        );

        // next, store the actual constraints
        String constraintN = setConstraints(test.getConstraints());


        // finally, generate all test cases that fit the constraints
        String testValuesN = buildTestCases(
                constraintN,
                test.getMaxCombis()
        );

        // perform the test
        return performGeneratedTestOnExpression(
                test.getTestExpression(),
                testValuesN,
                test.getPostProcessingMethodName(),
                test.getPrecision()
        );
    }

    /**
     * Returns the status of the test result
     * @param results the test result returned by {@link #performNumericalTest(NumericalTest)} or
     *                {@link #performGeneratedTestOnExpression(String, String, String, int)}.
     * @return the type of the result
     * @throws ComputerAlgebraSystemEngineException if the expressions cannot be analyzed
     */
    TestResultType getStatusOfResult(T results) throws ComputerAlgebraSystemEngineException;

    /**
     * @return the number of total test cases as performed by the latest test
     */
    default int getPerformedTestCases() {
        return 0;
    }

    /**
     * @return the number of failed test cases as performed by the latest test
     */
    default int getNumberOfFailedTestCases() {
        return 0;
    }

    /**
     * Returns a rather convenient pojo that summarizes the test result
     * @param results the test result as it was returned by {@link #performNumericalTest(NumericalTest)} or
     *                by {@link #performGeneratedTestOnExpression(String, String, String, int)}
     * @return a summary of the numeric test as a pojo
     * @throws ComputerAlgebraSystemEngineException if the result cannot be analyzed by the CAS
     */
    default NumericResult getNumericResult(T results) throws ComputerAlgebraSystemEngineException {
        TestResultType type = getStatusOfResult(results);
        int tests = getPerformedTestCases();
        int failed = getNumberOfFailedTestCases();

        NumericResult nr = new NumericResult(
                TestResultType.SUCCESS.equals(type),
                tests,
                failed,
                tests-failed
        );

        nr.addTestCalculations( getNumericCalculationList(results) );
        return nr;
    }

    /**
     * Analysis the result and returns a list of the performed tests that failed!
     * Notice that successful tests does not appear in the list. Hence, an empty list means the test
     * was successful.
     * @param result as it was generated by {@link #performNumericalTest(NumericalTest)} or
     *               by {@link #performGeneratedTestOnExpression(String, String, String, int)}
     * @return the list of failed test calculations and the values
     */
    List<NumericCalculation> getNumericCalculationList(T result);

    /**
     * In Maple its evalf( input );
     *
     * @param input
     * @return
     */
    String generateNumericalTestExpression(String input);

    /**
     * Returns the name of the values-variable for the variable names.
     *
     * @param variableName when vars is the variable that holds the variables,
     *                     you can call this method with {@param variableName} vars
     *                     to get the according variable name of the values.
     * @return name of values
     */
    static String getValuesName(String variableName) {
        return variableName + "Vals";
    }
}
