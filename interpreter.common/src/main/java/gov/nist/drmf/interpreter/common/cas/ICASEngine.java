package gov.nist.drmf.interpreter.common.cas;

import gov.nist.drmf.interpreter.common.exceptions.ComputerAlgebraSystemEngineException;

import java.util.List;

/**
 * Communicate via CAS engine directly
 * @author Andre Greiner-Petter
 */
public interface ICASEngine {
    /**
     * Enter a command into the CAS. If the input was not valid or something else went wrong, a
     * CAS exception will be thrown. Unfortunately, the CAS engine may run in a separated JVM, hence
     * communicating internal objects does not work. If you want to get access to (e.g.) Maple's Algebraic
     * object, you must start the engine within your own VM and communicate with the proper interface directly.
     *
     * @param command the command that should be processed within the CAS
     * @return the string representation of the answer from the CAS
     * @throws ComputerAlgebraSystemEngineException if the command caused an exception in the CAS
     */
    String enterCommand(String command) throws ComputerAlgebraSystemEngineException;

    /**
     * Manually force the CAS to clean the cash
     * @throws ComputerAlgebraSystemEngineException if the CAS threw an exception
     */
    void forceGC() throws ComputerAlgebraSystemEngineException;

    /**
     * Builds up a string representation of the given list (e.g. [..] vs {..}).
     * @param list the list
     * @return the CAS string representation of the given list
     */
    String buildList(List<String> list);
}
