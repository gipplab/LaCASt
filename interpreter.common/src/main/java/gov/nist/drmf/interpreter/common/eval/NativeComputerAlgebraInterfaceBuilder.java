package gov.nist.drmf.interpreter.common.eval;

import gov.nist.drmf.interpreter.common.cas.ICASEngineNumericalEvaluator;
import gov.nist.drmf.interpreter.common.cas.ICASEngineSymbolicEvaluator;
import gov.nist.drmf.interpreter.common.cas.IComputerAlgebraSystemEngine;
import gov.nist.drmf.interpreter.common.exceptions.CASUnavailableException;
import gov.nist.drmf.interpreter.common.exceptions.ComputerAlgebraSystemEngineException;

/**
 * This is an evaluation builder for CAS. It returns all necessary
 * translators and other interfaces to perform numeric evaluations.
 *
 * @author Andre Greiner-Petter
 */
public interface NativeComputerAlgebraInterfaceBuilder {
    /**
     * This is the most critical function. It returns true if the underlying
     * native code this CAS is available. If this method returns false, none of
     * the other methods will return anything useful! So make sure checking first
     * if the native CAS code is available with this method before using anything else.
     *
     * @return true if the native code of the CAS is available (installed on the system and properly
     * attached to the VM). Otherwise false.
     */
    boolean isCASAvailable();

    /**
     * The string representation of the language this CAS represents. For example, for Maple and Mathematica it
     * is simply the name of the CAS.
     * @return the name of the CAS, or more precisely the language key this CAS represents
     */
    String getLanguageKey();

    /**
     * Returns the CAS engine itself
     * @return a direct interface to the native CAS
     * @throws CASUnavailableException if {@link #isCASAvailable()} returns false
     */
    IComputerAlgebraSystemEngine getCASEngine() throws CASUnavailableException;

    /**
     * Returns the numeric evaluator of the CAS
     * @return an interface to the numeric evaluator of the cas
     * @throws CASUnavailableException if {@link #isCASAvailable()} returns false
     */
    ICASEngineNumericalEvaluator getNumericEvaluator() throws CASUnavailableException, ComputerAlgebraSystemEngineException;

    /**
     * Returns the symbolic evaluator of the CAS
     * @return an interface to the symbolic evaluator of the cas
     * @throws CASUnavailableException if {@link #isCASAvailable()} returns false
     */
    ICASEngineSymbolicEvaluator getSymbolicEvaluator() throws CASUnavailableException;

    /* Symbolic Calculation Connections */

    /**
     * Returns the default symbolic test cases that this CAS supports.
     * For example, Mathematica by default only supports 'FullSimplify'.
     * @return the default symbolic test cases
     */
    ISymbolicTestCases[] getDefaultSymbolicTestCases();

    /* Numeric Calculation Connections */

    /**
     * Returns the evaluation script handler
     * @return script handler for numerical tests
     * @throws CASUnavailableException if {@link #isCASAvailable()} returns false
     */
    default INumericalEvaluationScripts getEvaluationScriptHandler() throws CASUnavailableException {
        return (e -> e ? "" : "");
    }

    /**
     * Gets the default pre- and post-commands that will be performed before (in the first entry)
     * and after (second entry) a numerical test
     * @return the default commands for numerical tests (default is null)
     */
    default String[] getDefaultPrePostComputationCommands() {
        return null;
    }

    /**
     * Returns the names of the numeric procedures.
     * @return the numeric procedures that are necessary to perform numerical tests
     */
    default String[] getNumericProcedures() {
        return null;
    }

    /**
     * Loads the numeric procedures as given by {@link #getNumericProcedures()}.
     * @throws CASUnavailableException if {@link #isCASAvailable()} returns false
     * @throws ComputerAlgebraSystemEngineException if the values cannot be entered to the CAS
     */
    default void loadNumericProcedures() throws CASUnavailableException, ComputerAlgebraSystemEngineException {
        if (!isCASAvailable()) throw new CASUnavailableException();
        String[] procedures = getNumericProcedures();
        if ( procedures == null ) return;

        for ( String proc : procedures ) {
            getCASEngine().enterCommand(proc);
        }
    }
}
