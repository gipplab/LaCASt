package gov.nist.drmf.interpreter.common.eval;

import gov.nist.drmf.interpreter.common.cas.ICASEngineNumericalEvaluator;
import gov.nist.drmf.interpreter.common.cas.ICASEngineSymbolicEvaluator;
import gov.nist.drmf.interpreter.common.cas.IComputerAlgebraSystemEngine;
import gov.nist.drmf.interpreter.common.exceptions.CASUnavailableException;

/**
 * This is an evaluation builder for CAS. It returns all necessary
 * translators and other interfaces to perform numeric evaluations.
 *
 * @author Andre Greiner-Petter
 */
public interface NativeComputerAlgebraInterfaceBuilder<T> {
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

    IComputerAlgebraSystemEngine<T> getCASEngine() throws CASUnavailableException;

    ICASEngineNumericalEvaluator<T> getNumericEvaluator() throws CASUnavailableException;

    ICASEngineSymbolicEvaluator<T> getSymbolicEvaluator() throws CASUnavailableException;

    /* Symbolic Calculation Connections */
    ISymbolicTestCases[] getDefaultSymbolicTestCases();

    /* Numeric Calculation Connections */
    default INumericalEvaluationScripts getEvaluationScriptHandler() throws CASUnavailableException {
        return (e -> e ? "" : "");
    }

    default String[] getDefaultPrePostComputationCommands() {
        return null;
    }

    default String[] getNumericProcedures() {
        return null;
    }
}
