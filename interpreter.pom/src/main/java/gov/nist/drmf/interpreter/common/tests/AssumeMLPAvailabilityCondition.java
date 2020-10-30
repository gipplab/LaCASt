package gov.nist.drmf.interpreter.common.tests;

import gov.nist.drmf.interpreter.common.meta.AssumeMLPAvailability;
import gov.nist.drmf.interpreter.pom.MLPWrapper;
import org.junit.platform.commons.support.AnnotationSupport;

import java.lang.reflect.AnnotatedElement;
import java.util.Optional;

/**
 * This class checks if MLP is available to run tests.
 *
 * @author Andre Greiner-Petter
 */
public class AssumeMLPAvailabilityCondition extends AssumeToolAvailabilityCondition<AssumeMLPAvailability> {
    @Override
    public Optional<AssumeMLPAvailability> getAnnotations(Optional<? extends AnnotatedElement> element) {
        return AnnotationSupport.findAnnotation(element, AssumeMLPAvailability.class);
    }

    @Override
    public boolean isToolAvailable() {
        return MLPWrapper.isMLPPresent();
    }

    @Override
    public String getToolName() {
        return "MLP";
    }
}
