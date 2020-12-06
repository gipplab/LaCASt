package gov.nist.drmf.interpreter.common.config;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Andre Greiner-Petter
 */
public final class Config {
    @JsonProperty("lacast.libs.path")
    private String libsPath;

    @JsonProperty("lacast.config.path")
    private String configPath;

    @JsonProperty("lacast.cas")
    private final List<CASConfig> casConfigList = new LinkedList<>();

    @JsonProperty("lacast.generic")
    private GenericLacastConfig genericLacastConfig = new GenericLacastConfig();

    private Config() {}

    public Path getLibsPath() {
        return Paths.get(libsPath);
    }

    public Path getConfigPath() {
        return Paths.get(configPath);
    }

    public List<CASConfig> getCasConfigList() {
        return casConfigList;
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

        boolean valid = true;
        for ( CASConfig casC : casConfigList ) {
            valid &= casC.isValid();
        }

        return valid;
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
