package gov.nist.drmf.interpreter.common.config;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Andre Greiner-Petter
 */
public class GenericLacastConfig {
    @JsonProperty("elasticsearch.host")
    private String esHost = "localhost";

    @JsonProperty("elasticsearch.port")
    private int esPort = 9200;

    @JsonProperty("elasticsearch.macros.index")
    private String macroIndex = "dlmf-macros";

    @JsonProperty("mathoid.url")
    private String mathoidUrl = "http://localhost:10044/texvcinfo";

    @JsonProperty("settings")
    private Settings settings;

    public GenericLacastConfig() {}

    public String getEsHost() {
        return esHost;
    }

    public int getEsPort() {
        return esPort;
    }

    public String getMacroIndex() {
        return macroIndex;
    }

    public String getMathoidUrl() {
        return mathoidUrl;
    }

    public int getMaxRelations() {
        return settings.maxRelations;
    }

    public int getMaxMacros() {
        return settings.maxMacros;
    }

    public int getMaxDepth() {
        return settings.maxDepth;
    }

    public static GenericLacastConfig getConfig() {
        return ConfigDiscovery.getConfig().getGenericLacastConfig();
    }

    private class Settings {
        @JsonProperty("max.relations")
        private int maxRelations = 3;

        @JsonProperty("max.macros")
        private int maxMacros = 5;

        @JsonProperty("max.depth")
        private int maxDepth = -1;

        public Settings() {}
    }
}
