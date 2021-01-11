package gov.nist.drmf.interpreter.common.cas;

import gov.nist.drmf.interpreter.common.eval.ISymbolicTestCases;
import gov.nist.drmf.interpreter.common.eval.SymbolicalTest;
import gov.nist.drmf.interpreter.common.eval.TestResultType;
import gov.nist.drmf.interpreter.common.exceptions.ComputerAlgebraSystemEngineException;
import gov.nist.drmf.interpreter.common.eval.SymbolicCalculation;
import gov.nist.drmf.interpreter.common.eval.SymbolicResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
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

    @Override
    public SymbolicResult performSymbolicTest(SymbolicalTest test) {
        ArrayList<String> testExpressions = new ArrayList<>(test.getTestExpression());
        ArrayList<String> expectedOutcomes = new ArrayList<>(test.getExpectedOutcome());
        ISymbolicTestCases[] symbolicTestCases = test.getTestCases();

        List<SymbolicCalculation> calculations = new LinkedList<>();
        boolean overAllSuccessful = false;
        boolean allErrors = true;
        for ( ISymbolicTestCases testCase : symbolicTestCases ) {
            if ( !testCase.isActivated() ) continue;

            for ( int i = 0; i < testExpressions.size(); i++ ) {
                String testExpression = testCase.buildCommand(testExpressions.get(i));

                try {
                    T result = simplify(testExpression, test.getRequiredPackages());
                    boolean wasAborted = wasAborted(result);
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
                    }
                    overAllSuccessful |= (successful || isConditionallySuccessful);

                    SymbolicCalculation sc = new SymbolicCalculation();
                    sc.setResult(result.toString());
                    sc.wasSuccessful(successful);
                    sc.setTestProperty( testCase.getShortName() );
                    sc.wasAborted(wasAborted);
                    sc.setWasConditionallySuccessful(isConditionallySuccessful);
                    calculations.add( sc );
                    allErrors = false;
                } catch (Exception e) {
                    LOG.error("Error in symbolic test case: " + e.getMessage());
                    SymbolicCalculation sc = new SymbolicCalculation();
                    sc.setResult("Error - " + e.getMessage());
                    sc.setTestProperty( testCase.getShortName() );
                    calculations.add( sc );
                }
            }
        }

        SymbolicResult sr = new SymbolicResult();
        TestResultType resultType = overAllSuccessful ? TestResultType.SUCCESS :
                ( allErrors ? TestResultType.ERROR : TestResultType.FAILURE );
        sr.setTestResultType( resultType );
        sr.setTestCalculations( calculations );
        sr.setNumberOfTests( calculations.size() );
        return sr;
    }
}
