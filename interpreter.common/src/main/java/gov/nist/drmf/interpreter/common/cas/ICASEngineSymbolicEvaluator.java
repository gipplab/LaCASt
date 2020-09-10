package gov.nist.drmf.interpreter.common.cas;

import gov.nist.drmf.interpreter.common.exceptions.ComputerAlgebraSystemEngineException;

import java.util.Observable;
import java.util.Observer;
import java.util.Set;

/**
 * @author Andre Greiner-Petter
 */
public interface ICASEngineSymbolicEvaluator<T> extends Observer, IAbortEvaluator<T> {

    T simplify( String expr, Set<String> requiredPackages ) throws ComputerAlgebraSystemEngineException;

    T simplify( String expr, String assumption, Set<String> requiredPackages ) throws ComputerAlgebraSystemEngineException;

    void setTimeout(double timeoutInSeconds);

    boolean isAsExpected(T in, double expect);

    boolean isConditionallyExpected(T in, double expect);

    String getCondition(T in);

    @Override
    default void update(Observable o, Object arg) {
        try {
            abort();
        } catch ( ComputerAlgebraSystemEngineException casee ) {
            casee.printStackTrace();
        }
    }
}
