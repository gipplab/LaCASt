package gov.nist.drmf.interpreter.common.constants;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import gov.nist.drmf.interpreter.common.config.Config;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Andre Greiner-Petter
 */
public final class CASSupporter {
    @JsonIgnore
    private static final Logger LOG = LogManager.getLogger(CASSupporter.class.getName());

    @JsonIgnore
    private static CASSupporter supporter;

    @JsonProperty("cas")
    private List<String> supportedCAS;

    private CASSupporter() {
        supportedCAS = new LinkedList<>();
    }

    public List<String> getAllCAS() {
        return supportedCAS;
    }

    public static CASSupporter getSupportedCAS() {
        if ( supporter != null ) return supporter;

        Path configPath = GlobalPaths.PATH_SUPPORTED_CAS_CONFIG;
        try {
            ObjectMapper om = new ObjectMapper(new YAMLFactory());
            supporter = om.readValue(configPath.toFile(), CASSupporter.class);
            return supporter;
        } catch (Exception e) {
            LOG.warn("Unable to load supported CAS file from " + configPath + ". Load default CAS.");
            LOG.debug("Cannot load supported CAS file because " + e.getMessage(), e);
            supporter = new CASSupporter();
            supporter.supportedCAS.add("Maple");
            supporter.supportedCAS.add("Mathematica");
            return supporter;
        }
    }
}
