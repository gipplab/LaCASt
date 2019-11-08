package gov.nist.drmf.interpreter.maple.setup;

import gov.nist.drmf.interpreter.maple.translation.MapleInterface;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.Optional;

import static org.junit.platform.commons.util.AnnotationUtils.findAnnotation;

/**
 * This class checks if Maple is available to run tests. Tests will be skipped if
 * Maple is not available.
 *
 * @see AssumeMapleAvailability
 * @author Andre Greiner-Petter
 */
public class AssumeMapleAvailabilityCondition implements ExecutionCondition {
    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext extensionContext) {
        Optional<AssumeMapleAvailability> annotation =
                findAnnotation(extensionContext.getElement(), AssumeMapleAvailability.class);

        if ( annotation.isPresent() ){
            if (MapleInterface.isMaplePresent()) {
                return ConditionEvaluationResult.enabled("Maple is available. Continuing tests.");
            } else {
                return ConditionEvaluationResult.disabled("Maple is not available, skip related tests.");
            }
        } else {
            return ConditionEvaluationResult.enabled("No availability checks. Continuing tests without conditions.");
        }
    }
}
