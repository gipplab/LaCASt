package gov.nist.drmf.interpreter.common.cas;

import gov.nist.drmf.interpreter.common.eval.ISymbolicTestCases;
import gov.nist.drmf.interpreter.common.eval.SymbolicalTest;
import gov.nist.drmf.interpreter.common.exceptions.ComputerAlgebraSystemEngineException;
import gov.nist.drmf.interpreter.common.pojo.SymbolicCalculation;
import gov.nist.drmf.interpreter.common.pojo.SymbolicResult;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * @author Andre Greiner-Petter
 */
public interface ICASEngineSymbolicEvaluator<T> extends IAbortEvaluator<T> {

    T simplify( String expr, Set<String> requiredPackages ) throws ComputerAlgebraSystemEngineException;

    T simplify( String expr, String assumption, Set<String> requiredPackages ) throws ComputerAlgebraSystemEngineException;

    boolean isTrue(T in) throws ComputerAlgebraSystemEngineException;

    boolean isAsExpected(T in, double expect);

    boolean isConditionallyExpected(T in, double expect);

    String getCondition(T in);

    default SymbolicResult getResult(SymbolicalTest test) throws ComputerAlgebraSystemEngineException {
        ArrayList<String> testExpressions = new ArrayList<>(test.getTestExpression());
        ArrayList<String> expectedOutcomes = new ArrayList<>(test.getExpectedOutcome());
        ISymbolicTestCases[] symbolicTestCases = test.getTestCases();

        List<SymbolicCalculation> calculations = new LinkedList<>();
        boolean overAllSuccessful = false;
        for ( ISymbolicTestCases testCase : symbolicTestCases ) {
            if ( !testCase.isActivated() ) continue;

            for ( int i = 0; i < testExpressions.size(); i++ ) {
                String testExpression = testCase.buildCommand(testExpressions.get(i));
                T result = simplify(testExpression, test.getRequiredPackages());
                if ( !overAllSuccessful ) {
                    String expect = expectedOutcomes.get(i);
                    boolean successful;
                    if ( "true".equals(expect) ) successful = isTrue(result);
                    else {
                        double expectedD = Double.parseDouble(expect);
                        successful = isAsExpected(result, expectedD);
                    }
                    overAllSuccessful = successful;
                }

                SymbolicCalculation sc = new SymbolicCalculation();
                sc.setResult(result.toString());
                sc.setTestProperty( testCase.getShortName() );
                calculations.add( sc );
            }
        }
        SymbolicResult sr = new SymbolicResult();
        sr.setSuccessful(overAllSuccessful);
        sr.setTestCalculations( calculations );
        sr.setNumberOfTests( calculations.size() );
        return sr;
    }
}
