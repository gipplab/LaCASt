package gov.nist.drmf.interpreter.evaluation;

import gov.nist.drmf.interpreter.common.exceptions.ComputerAlgebraSystemEngineException;
import gov.nist.drmf.interpreter.common.grammar.IComputerAlgebraSystemEngine;

/**
 * @author Andre Greiner-Petter
 */
public interface ICASEngineSymbolicEvaluator<T> {

    T simplify( String expr ) throws ComputerAlgebraSystemEngineException;

    T simplify( String expr, String assumption ) throws ComputerAlgebraSystemEngineException;

    boolean isAsExpected(T in, String expect);

}
