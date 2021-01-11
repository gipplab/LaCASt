package gov.nist.drmf.interpreter.common.cas;

import gov.nist.drmf.interpreter.common.eval.EvaluatorType;
import gov.nist.drmf.interpreter.common.eval.SymbolicalTest;
import gov.nist.drmf.interpreter.common.exceptions.ComputerAlgebraSystemEngineException;
import gov.nist.drmf.interpreter.common.eval.SymbolicResult;

import java.util.List;

/**
 * @author Andre Greiner-Petter
 */
public interface ICASEngineSymbolicEvaluator extends IAbortEvaluator {

    SymbolicResult performSymbolicTest(SymbolicalTest test);

    /**
     * Sets global assumptions that will be applied to all following tests.
     * @param assumptions list of assumptions
     */
    default void setGlobalSymbolicAssumptions(List<String> assumptions) throws ComputerAlgebraSystemEngineException {
        // ignore by default
    }

    default void setTimeout(double timeoutInSeconds) {
        setTimeout(EvaluatorType.SYMBOLIC, timeoutInSeconds);
    }

    default void disableTimeout() {
        disableTimeout(EvaluatorType.SYMBOLIC);
    }
}
