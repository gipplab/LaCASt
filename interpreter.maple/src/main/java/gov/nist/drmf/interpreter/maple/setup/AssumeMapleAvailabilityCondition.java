package gov.nist.drmf.interpreter.maple.setup;

import gov.nist.drmf.interpreter.common.tests.AssumeToolAvailabilityCondition;
import gov.nist.drmf.interpreter.maple.MapleConnector;
import gov.nist.drmf.interpreter.maple.extension.MapleInterface;
import gov.nist.drmf.interpreter.maple.secure.MapleRmiClient;

/**
 * This class checks if Maple is available to run tests. Tests will be skipped if
 * Maple is not available.
 *
 * @see AssumeMapleAvailability
 * @author Andre Greiner-Petter
 */
public class AssumeMapleAvailabilityCondition extends AssumeToolAvailabilityCondition<AssumeMapleAvailability> {
    @Override
    public Class<AssumeMapleAvailability> getInterface() {
        return AssumeMapleAvailability.class;
    }

    @Override
    public boolean isToolAvailable() {
        return MapleConnector.isMapleAvailable();
    }

    @Override
    public String getToolName() {
        return "Maple";
    }
}
