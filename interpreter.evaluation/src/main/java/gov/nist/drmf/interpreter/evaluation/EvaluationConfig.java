package gov.nist.drmf.interpreter.evaluation;

import java.nio.file.Path;

/**
 * @author Andre Greiner-Petter
 */
public interface EvaluationConfig {
    int[] getSubSetInterval();

    String getTestExpression();

    boolean showDLMFLinks();

    Path getOutputPath();

    Path getMissingMacrosOutputPath();
}
