package gov.nist.drmf.interpreter.pom.common.tests;

import gov.nist.drmf.interpreter.pom.common.meta.AssumeMLPAvailability;
import gov.nist.drmf.interpreter.common.tests.AssumeToolAvailabilityCondition;
import gov.nist.drmf.interpreter.pom.MLPWrapper;

/**
 * This class checks if MLP is available to run tests.
 *
 * @author Andre Greiner-Petter
 */
public class AssumeMLPAvailabilityCondition extends AssumeToolAvailabilityCondition<AssumeMLPAvailability> {
    @Override
    public Class<AssumeMLPAvailability> getInterface() {
        return AssumeMLPAvailability.class;
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
