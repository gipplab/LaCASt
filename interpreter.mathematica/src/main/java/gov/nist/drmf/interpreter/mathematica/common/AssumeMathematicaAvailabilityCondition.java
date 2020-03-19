package gov.nist.drmf.interpreter.mathematica.common;

import gov.nist.drmf.interpreter.common.tests.AssumeToolAvailabilityCondition;
import gov.nist.drmf.interpreter.mathematica.config.MathematicaConfig;
import org.junit.platform.commons.support.AnnotationSupport;

import java.lang.reflect.AnnotatedElement;
import java.util.Optional;

/**
 * This class checks if Mathematica is available to run tests. Tests will be skipped if
 * Mathematica is not available.
 *
 * @see AssumeMathematicaAvailability
 * @author Andre Greiner-Petter
 */
public class AssumeMathematicaAvailabilityCondition extends AssumeToolAvailabilityCondition<AssumeMathematicaAvailability> {
    @Override
    public Optional<AssumeMathematicaAvailability> getAnnotations(Optional<? extends AnnotatedElement> element) {
        return AnnotationSupport.findAnnotation(element, AssumeMathematicaAvailability.class);
    }

    @Override
    public boolean isToolAvailable() {
        return MathematicaConfig.isMathematicaPresent();
    }

    @Override
    public String getToolName() {
        return "Mathematica";
    }
}
