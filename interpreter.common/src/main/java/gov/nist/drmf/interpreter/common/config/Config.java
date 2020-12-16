package gov.nist.drmf.interpreter.common.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Andre Greiner-Petter
 */
public final class Config {
    @JsonIgnore
    private static final Logger LOG = LogManager.getLogger(Config.class.getName());

    @JsonProperty("lacast.libs.path")
    private String libsPath;

    @JsonProperty("lacast.config.path")
    private String configPath;

    @JsonProperty("lacast.cas")
    private final Map<String, CASConfig> casConfigs = new HashMap<>();

    @JsonProperty("lacast.generic")
    private GenericLacastConfig genericLacastConfig = new GenericLacastConfig();

    private Config() {}

    public Path getLibsPath() {
        return Paths.get(libsPath);
    }

    public Path getConfigPath() {
        return Paths.get(configPath);
    }

    public Map<String, CASConfig> getCasConfigs() {
        return casConfigs;
    }

    public Collection<String> getSupportedCAS() {
        return casConfigs.keySet();
    }

    public GenericLacastConfig getGenericLacastConfig() {
        return genericLacastConfig;
    }

    /**
     * Checks the validity of this config file. It checks if all necessary information are available.
     * @return check if the current configuration is valid or not.
     */
    public boolean isValid() {
        if ( !isConfigValid() ) return false;
        if ( !isLibsValid() ) return false;

        for ( Map.Entry<String, CASConfig> casC : casConfigs.entrySet() ) {
            CASConfig conf = casC.getValue();
            if ( conf == null ) continue;
            boolean valid = conf.isValid();
            if ( !valid ) {
                LOG.printf(Level.WARN,
                        "The config path for the cas %s is invalid. " +
                                "The path %s does not exist! Ignoring the config path.",
                        casC.getKey(), casC.getValue().getStringInstallPath()
                );
                casC.getValue().setInstallPaths(null);
            }
        }

        return true;
    }

    private boolean isConfigValid() {
        if (!Files.exists(getConfigPath().resolve("replacements.yaml"))) return false;
        else return Files.exists(getConfigPath().resolve("dlmf-replacements.yaml"));
    }

    private boolean isLibsValid() {
        Path refData = getLibsPath().resolve("ReferenceData");
        return Files.exists(refData.resolve("BasicConversions")) &&
                Files.exists(refData.resolve("Lexicons"));
    }
}
