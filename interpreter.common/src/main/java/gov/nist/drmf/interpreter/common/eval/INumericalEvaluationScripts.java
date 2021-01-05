package gov.nist.drmf.interpreter.common.eval;

/**
 * @author Andre Greiner-Petter
 */
public interface INumericalEvaluationScripts {
    /**
     * Returns the numerical evaluation script depending on equation or relation.
     * Maple uses different evaluation scripts for an equation and a general relation.
     * This method returns the appropriate script for both cases.
     *
     * @param isEquation should be true if the test case represents an equation
     * @return the appropriate script
     */
    String getPostProcessingScriptName(boolean isEquation);
}
