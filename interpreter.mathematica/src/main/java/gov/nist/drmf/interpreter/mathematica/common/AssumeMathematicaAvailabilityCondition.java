package gov.nist.drmf.interpreter.mathematica.common;

import gov.nist.drmf.interpreter.mathematica.config.MathematicaConfig;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.Optional;

import static org.junit.platform.commons.util.AnnotationUtils.findAnnotation;

/**
 * @author Andre Greiner-Petter
 */
public class AssumeMathematicaAvailabilityCondition implements ExecutionCondition {
    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext extensionContext) {
        Optional<AssumeMathematicaAvailability> annotation =
                findAnnotation(extensionContext.getElement(), AssumeMathematicaAvailability.class);

        if ( annotation.isPresent() ){
            if (MathematicaConfig.isMathematicaPresent()) {
                return ConditionEvaluationResult.enabled("Mathematica is available. Continuing tests.");
            } else {
                return ConditionEvaluationResult.disabled("Mathematica is not available, skip related tests.");
            }
        } else {
            return ConditionEvaluationResult.enabled("No availability checks. Continuing tests without conditions.");
        }
    }
}
