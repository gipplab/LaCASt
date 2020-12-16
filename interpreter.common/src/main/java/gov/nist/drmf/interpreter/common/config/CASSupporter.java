package gov.nist.drmf.interpreter.common.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.LinkedList;
import java.util.List;

/**
 * @author Andre Greiner-Petter
 */
public final class CASSupporter {
    private static final Logger LOG = LogManager.getLogger(CASSupporter.class.getName());

    private static CASSupporter supporter;

    private final List<String> supportedCAS;

    private CASSupporter() {
        supportedCAS = new LinkedList<>();
    }

    public List<String> getAllCAS() {
        return supportedCAS;
    }

    public static CASSupporter getSupportedCAS() {
        if ( supporter != null ) return supporter;

        Config config = ConfigDiscovery.getConfig();
        supporter = new CASSupporter();
        supporter.supportedCAS.addAll(config.getSupportedCAS());
        addDefaultCAS(supporter);
        return supporter;
    }

    private static void addDefaultCAS(CASSupporter casSupporter) {
        if ( !casSupporter.supportedCAS.contains("Maple") ) {
            LOG.info("Added default Maple support");
            casSupporter.supportedCAS.add(0, "Maple");
        }
        if ( !casSupporter.supportedCAS.contains("Mathematica") ) {
            LOG.info("Added default Mathematica support");
            casSupporter.supportedCAS.add(0, "Mathematica");
        }
    }
}
