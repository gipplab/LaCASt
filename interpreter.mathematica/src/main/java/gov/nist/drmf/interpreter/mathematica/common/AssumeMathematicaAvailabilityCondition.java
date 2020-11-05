package gov.nist.drmf.interpreter.mathematica.common;

import gov.nist.drmf.interpreter.common.tests.AssumeToolAvailabilityCondition;
import gov.nist.drmf.interpreter.mathematica.config.MathematicaConfig;

/**
 * This class checks if Mathematica is available to run tests. Tests will be skipped if
 * Mathematica is not available.
 *
 * @see AssumeMathematicaAvailability
 * @author Andre Greiner-Petter
 */
public class AssumeMathematicaAvailabilityCondition extends AssumeToolAvailabilityCondition<AssumeMathematicaAvailability> {
    @Override
    public Class<AssumeMathematicaAvailability> getInterface() {
        return AssumeMathematicaAvailability.class;
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
