package gov.nist.drmf.interpreter.common.tests;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.support.AnnotationSupport;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.Optional;

/**
 * A general class to check if a tool is available. It implements the {@link ExecutionCondition} to
 * run tests only when the required tool is available. Simply annotate the test class or method
 * with the annotation {@link T}. For an example of usage, take a look at
 * {@link gov.nist.drmf.interpreter.common.meta.AssumeMLPAvailability} and the extension of this class
 * {@link gov.nist.drmf.interpreter.common.meta.AssumeMLPAvailability} to skip tests, if MLP is not available.
 *
 * @author Andre Greiner-Petter
 */
public abstract class AssumeToolAvailabilityCondition<T extends Annotation> implements ExecutionCondition {
    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext extensionContext) {
        Optional<T> annotation = getAnnotations(extensionContext.getElement());
        if ( annotation.isPresent() ){
            if (isToolAvailable()) {
                return ConditionEvaluationResult.enabled(getToolName() + " is available. Continuing tests.");
            } else {
                return ConditionEvaluationResult.disabled(getToolName() + " is not available, skip related tests.");
            }
        } else {
            return ConditionEvaluationResult.enabled("No availability checks. Continuing tests without conditions.");
        }
    }

    /**
     * Get classes of annotations.
     * {@link AnnotationSupport#findAnnotation(AnnotatedElement, Class)}.
     *
     * @param element an element that might be annotated
     * @return the annotated element
     */
    public abstract Optional<T> getAnnotations(Optional<? extends AnnotatedElement> element);

    /**
     * The actual test method to test the tools availability.
     * This should not throw an exception!
     * @return true if the tool is available, false otherwise.
     */
    public abstract boolean isToolAvailable();

    /**
     * Returns the name of the tool for proper logging.
     * @return the name of the tool
     */
    public abstract String getToolName();
}
