package gov.nist.drmf.interpreter.common.cas;

import gov.nist.drmf.interpreter.common.eval.*;
import gov.nist.drmf.interpreter.common.exceptions.ComputerAlgebraSystemEngineException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Set;

/**
 * @author Andre Greiner-Petter
 */
public abstract class AbstractCasEngineSymbolicEvaluator<T> implements ICASEngineSymbolicEvaluator {
    private static final Logger LOG = LogManager.getLogger(AbstractCasEngineSymbolicEvaluator.class.getName());

    public abstract T simplify(String expr, Set<String> requiredPackages) throws ComputerAlgebraSystemEngineException;

    public abstract T simplify(String expr, String assumption, Set<String> requiredPackages) throws ComputerAlgebraSystemEngineException;

    public abstract boolean isTrue(T in) throws ComputerAlgebraSystemEngineException;

    public abstract boolean isAsExpected(T in, double expect);

    public abstract boolean isConditionallyExpected(T in, double expect);

    public abstract String getCondition(T in);

    public abstract boolean wasAborted(T result);

    public abstract String getLatestTestExpression();

    @Override
    synchronized public SymbolicResult performSymbolicTest(SymbolicalTest test) {
        ArrayList<SymbolicalTestBaseCase> testExpressions = new ArrayList<>(test.getTestExpression());
        ArrayList<String> expectedOutcomes = new ArrayList<>(test.getExpectedOutcome());
        ISymbolicTestCases[] symbolicTestCases = test.getTestCases();

        SymbolicResult symbolicResult = new SymbolicResult();
        for ( int i = 0; i < testExpressions.size(); i++ ) {
            SymbolicalTestBaseCase testBase = testExpressions.get(i);
            SymbolicCalculationGroup group = new SymbolicCalculationGroup();
            group.setLhs(testBase.getLhs());
            group.setRhs(testBase.getRhs());
            group.setTestExpression(testBase.getTestExpression());

            for ( ISymbolicTestCases testCase : symbolicTestCases ) {
                if (!testCase.isActivated()) continue;

                SymbolicCalculation symbolicCalculation = new SymbolicCalculation();
                String testExpression = testCase.buildCommand(testBase.getTestExpression());
                symbolicCalculation.setTestTitle(testCase.getShortName());

                try {
                    T result = simplify(testExpression, test.getRequiredPackages());
                    symbolicCalculation.setTestExpression(getLatestTestExpression());
                    boolean wasAborted = wasAborted(result);
                    symbolicCalculation.wasAborted(wasAborted);
                    if ( wasAborted ) {
                        symbolicCalculation.setResult(TestResultType.SKIPPED);
                        continue;
                    }

                    symbolicCalculation.setResultExpression(result.toString());

                    boolean isConditionallySuccessful = false;

                    String expect = expectedOutcomes.get(i);
                    boolean successful;
                    if ( "true".equals(expect) ) {
                        successful = isTrue(result);
                    }
                    else {
                        double expectedD = Double.parseDouble(expect);
                        successful = isAsExpected(result, expectedD);
                        isConditionallySuccessful = isConditionallyExpected(result, expectedD);
                        symbolicCalculation.setWasConditionallySuccessful(isConditionallySuccessful);
                    }

                    if ( successful || isConditionallySuccessful )
                        symbolicCalculation.setResult(TestResultType.SUCCESS);
                    else symbolicCalculation.setResult(TestResultType.FAILURE);

                } catch (Exception e) {
                    LOG.error("Error in symbolic test case: " + e.getMessage(), e);
                    symbolicCalculation.setResult(TestResultType.ERROR);
                } finally {
                    group.addTestCalculation(symbolicCalculation);
                }
            }

            symbolicResult.addTestCalculationsGroup(group);
        }

        return symbolicResult;
    }
}
