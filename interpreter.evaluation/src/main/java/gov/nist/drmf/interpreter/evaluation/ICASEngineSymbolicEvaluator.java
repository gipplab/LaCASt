package gov.nist.drmf.interpreter.evaluation;

import gov.nist.drmf.interpreter.common.exceptions.ComputerAlgebraSystemEngineException;
import gov.nist.drmf.interpreter.common.grammar.IComputerAlgebraSystemEngine;

import java.util.List;
import java.util.Observable;
import java.util.Observer;

/**
 * @author Andre Greiner-Petter
 */
public interface ICASEngineSymbolicEvaluator<T> extends Observer, IAbortEvaluator<T> {

    T simplify( String expr ) throws ComputerAlgebraSystemEngineException;

    T simplify( String expr, String assumption ) throws ComputerAlgebraSystemEngineException;

    boolean isAsExpected(T in, String expect);

    @Override
    default void update(Observable o, Object arg) {
        try {
            abort();
        } catch ( ComputerAlgebraSystemEngineException casee ) {
            casee.printStackTrace();
        }
    }
}
