package gov.nist.drmf.interpreter.common.grammar;

import gov.nist.drmf.interpreter.common.exceptions.ComputerAlgebraSystemEngineException;

/**
 * @author Andre Greiner-Petter
 */
public interface IComputerAlgebraSystemEngine<T> {
    T enterCommand(String command) throws ComputerAlgebraSystemEngineException;
}
