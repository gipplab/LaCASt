package gov.nist.drmf.interpreter.common.grammar;

import gov.nist.drmf.interpreter.common.exceptions.ComputerAlgebraSystemEngineException;

import java.util.List;

/**
 * @author Andre Greiner-Petter
 */
public interface IComputerAlgebraSystemEngine<T> {
    T enterCommand(String command) throws ComputerAlgebraSystemEngineException;

    void forceGC() throws ComputerAlgebraSystemEngineException;

    String buildList(List<String> list);
}
