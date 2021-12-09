package gov.nist.drmf.interpreter.common.cas;

import gov.nist.drmf.interpreter.common.eval.*;
import gov.nist.drmf.interpreter.common.exceptions.ComputerAlgebraSystemEngineException;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * @author Andre Greiner-Petter
 */
public abstract class AbstractCasEngineNumericalEvaluator<T> implements ICASEngineNumericalEvaluator {
    /**
     * Stores the variables of the given expression and returns the
     * name of the variable that stores the information.
     *
     * @param variables  mathematical expression (already translated)
     * @param testValues list of values
     */
    public abstract void storeVariables(Collection<String> variables, Collection<String> testValues);

    /**
     * Stores the given constraint variables and their values.
     * It also updates the previously stored variables.
     *
     * @param constraintVariables list of variables of the constraints (translated)
     * @param constraintValues    the values for the constraint variables (ordered and translated)
     */
    public abstract void storeConstraintVariables(
            List<String> constraintVariables,
            List<String> constraintValues);

    /**
     * Essentially the same as {@link #storeConstraintVariables(List, List)}.
     *
     * @param extraVariables list of special treatment variables (translated)
     * @param extraValues    the values for the variables (ordered and translated)
     * @return the name of the variable that accesses the special treatment variables
     */
    public abstract void storeExtraVariables(
            List<String> extraVariables,
            List<String> extraValues);

    /**
     * Sets constraints and returns the name of variables that is defined as the constraints.
     * It returns null of no constraints exists.
     *
     * @param constraints constraints
     * @return name of variable or null
     */
    public abstract String setConstraints(List<String> constraints);

    /**
     * Builds the test cases and returns the name of variable.
     *
     * @param maxCombis maximum number of combinations
     * @return name of test cases variable
     * @throws ComputerAlgebraSystemEngineException
     * @throws IllegalArgumentException
     */
    public abstract String buildTestCases(String nameOfConstraints, int maxCombis) throws ComputerAlgebraSystemEngineException, IllegalArgumentException;

    /**
     * Returns true if this engine requires to register packages. If this is true
     * you should implement and use {@link #addRequiredPackages(Set)}.
     *
     * @return true if this engine requires to register packages
     */
    public boolean requiresRegisteredPackages() {
        return false;
    }

    /**
     * Adds required packages to this test
     * @param packages
     */
    public void addRequiredPackages(Set<String> packages) {
        // nothing;
    }

    /**
     * @return the number of total test cases as performed by the latest test
     */
    public int getPerformedTestCases() {
        return 0;
    }

    /**
     * @return the number of failed test cases as performed by the latest test
     */
    public int getNumberOfFailedTestCases() {
        return 0;
    }

    /**
     * Returns the name of the values-variable for the variable names.
     *
     * @param variableName when vars is the variable that holds the variables,
     *                     you can call this method with {@param variableName} vars
     *                     to get the according variable name of the values.
     * @return name of values
     */
    public static String getValuesName(String variableName) {
        return variableName + "Vals";
    }

    /**
     * The last part in the chain of a setup for numerical test. See {@link #performNumericalTest(NumericalTest)} to
     * see the entire process of setting up a numeric test.
     * @param expression the expression that should be tested (e.g. lhs-rhs)
     * @param testCasesName the variable name of the stored test cases returned by {@link #buildTestCases(String, int)}
     * @param postProcessingMethodName the process to call after the test was performed (for post processing)
     * @param precision the precision the numerical test should use
     * @return the original CAS result. Use {@link #wasAborted(Object)} to analyze
     * the result and {@link #getNumericCalculationGroup(Object)} (Object)} to get an object list of the results.
     * @throws ComputerAlgebraSystemEngineException if something in the CAS went wrong during the test
     */
    public abstract T performGeneratedTestOnExpression(
            String expression,
            String testCasesName,
            String postProcessingMethodName,
            int precision
    ) throws ComputerAlgebraSystemEngineException;

    public abstract void setCurrentTestCase(String lhs, String rhs);

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
    private T performNumericalTest(NumericalTest test)
            throws ComputerAlgebraSystemEngineException {
        addRequiredPackages(test.getRequiredPackages());

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

        setCurrentTestCase(test.getLhs(), test.getRhs());

        // perform the test
        return performGeneratedTestOnExpression(
                test.getTestExpression(),
                testValuesN,
                test.getPostProcessingMethodName(),
                test.getPrecision()
        );
    }

    /**
     * Returns the status of a single (!) test result, i.e., the given result is a list of two elements:
     * 1) The numeric result (expected to be a numerical value, such as 0. But it can be anything if a test was unsuccessful)
     * 2) A map/list of the values for each variable that produced this test result (e.g., {x -> 1, y -> I}).
     *
     * Note that the test methods {@link #performNumericalTest(NumericalTest)} and
     * {@link #performGeneratedTestOnExpression(String, String, String, int)} return lists of results. This method,
     * expects a single test result, i.e., just a single element of the list returned by these methods. If you want
     * to know the overall status (i.e., the status over all test results combined in the entire list of tests), use
     * {@link #getNumericResult(Object)} with {@link NumericResult#overallResult()} instead.
     *
     * @param results one element of the list of results that was returned either by
     *                  {@link #performNumericalTest(NumericalTest)} or
     *                  {@link #performGeneratedTestOnExpression(String, String, String, int)}.
     * @return the type of the result
     * @throws ComputerAlgebraSystemEngineException if the expressions cannot be analyzed
     */
    public abstract TestResultType getStatusOfSingleResult(T results) throws ComputerAlgebraSystemEngineException;

    /**
     * Returns a rather convenient pojo that summarizes the test result
     * @param results the test result as it was returned by {@link #performNumericalTest(NumericalTest)} or
     *                by {@link #performGeneratedTestOnExpression(String, String, String, int)}
     * @return a summary of the numeric test as a pojo
     * @throws ComputerAlgebraSystemEngineException if the result cannot be analyzed by the CAS
     */
    public NumericResult getNumericResult(T results) throws ComputerAlgebraSystemEngineException {
        NumericResult nr = new NumericResult();
        if ( wasAborted(results) ) {
            nr.wasAborted(true);
        } else {
            NumericCalculationGroup group = getNumericCalculationGroup(results);
            nr.addTestCalculationsGroup( group );
        }
        return nr;
    }

    public abstract boolean wasAborted(T result);

    /**
     * Analysis the result and returns a list of the performed tests that failed!
     * Notice that successful tests does not appear in the list. Hence, an empty list means the test
     * was successful.
     * @param result as it was generated by {@link #performNumericalTest(NumericalTest)} or
     *               by {@link #performGeneratedTestOnExpression(String, String, String, int)}
     * @return the list of failed test calculations and the values
     */
    public abstract NumericCalculationGroup getNumericCalculationGroup(T result);

    @Override
    synchronized public NumericResult performNumericTest(NumericalTest test) throws ComputerAlgebraSystemEngineException {
        T result = performNumericalTest( test );
        return getNumericResult(result);
    }
}
