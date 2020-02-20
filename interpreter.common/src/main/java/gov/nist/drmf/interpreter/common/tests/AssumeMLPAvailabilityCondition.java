package gov.nist.drmf.interpreter.common.tests;

import gov.nist.drmf.interpreter.mlp.MLPWrapper;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.Optional;

import static org.junit.platform.commons.support.AnnotationSupport.findAnnotation;

/**
 * @author Andre Greiner-Petter
 */
public class AssumeMLPAvailabilityCondition implements ExecutionCondition {
    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext extensionContext) {
        Optional<AssumeMLPAvailability> annotation =
                findAnnotation(extensionContext.getElement(), AssumeMLPAvailability.class);
        if ( annotation.isPresent() ){
            if (MLPWrapper.isMLPPresent()) {
                return ConditionEvaluationResult.enabled("MLP is available. Continuing tests.");
            } else {
                return ConditionEvaluationResult.disabled("MLP is not available, skip related tests.");
            }
        } else {
            return ConditionEvaluationResult.enabled("No availability checks. Continuing tests without conditions.");
        }
    }
}
