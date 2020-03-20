package gov.nist.drmf.interpreter.evaluation.core.numeric;

import gov.nist.drmf.interpreter.evaluation.common.Case;

/**
 * @author Andre Greiner-Petter
 */
public interface INumericalEvaluationScripts {
    String getPostProcessingScriptName(Case c);
}
