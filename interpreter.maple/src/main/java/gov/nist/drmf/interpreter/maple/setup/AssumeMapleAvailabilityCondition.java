package gov.nist.drmf.interpreter.maple.setup;

import gov.nist.drmf.interpreter.common.tests.AssumeToolAvailabilityCondition;
import gov.nist.drmf.interpreter.maple.extension.MapleInterface;
import org.junit.platform.commons.support.AnnotationSupport;

import java.lang.reflect.AnnotatedElement;
import java.util.Optional;

/**
 * This class checks if Maple is available to run tests. Tests will be skipped if
 * Maple is not available.
 *
 * @see AssumeMapleAvailability
 * @author Andre Greiner-Petter
 */
public class AssumeMapleAvailabilityCondition extends AssumeToolAvailabilityCondition<AssumeMapleAvailability> {
    @Override
    public Optional<AssumeMapleAvailability> getAnnotations(Optional<? extends AnnotatedElement> element) {
        return AnnotationSupport.findAnnotation(element, AssumeMapleAvailability.class);
    }

    @Override
    public boolean isToolAvailable() {
        return MapleInterface.isMaplePresent();
    }

    @Override
    public String getToolName() {
        return "Maple";
    }
}
