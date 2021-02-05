package gov.nist.drmf.interpreter.common.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import gov.nist.drmf.interpreter.common.process.RmiSubprocessInfo;

import java.util.LinkedList;
import java.util.List;

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
    private final Settings settings;

    @JsonIgnore
    private RmiSubprocessInfo mapleSubprocessInfo = null;

    public GenericLacastConfig() {
        settings = new Settings();
    }

    public GenericLacastConfig(GenericLacastConfig copy) {
        this.esHost = copy.esHost;
        this.esPort = copy.esPort;
        this.macroIndex = copy.macroIndex;
        this.mathoidUrl = copy.mathoidUrl;
        this.settings = copy.settings;
        this.mapleSubprocessInfo = copy.mapleSubprocessInfo;
    }

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

    public void setMaxRelations(int maxRelations) {
        settings.maxRelations = maxRelations;
    }

    public void setMaxMacros(int maxMacros) {
        settings.maxMacros = maxMacros;
    }

    public void setMaxDepth(int maxDepth) {
        settings.maxDepth = maxDepth;
    }

    public void setEsHost(String esHost) {
        this.esHost = esHost;
    }

    public void setEsPort(int esPort) {
        this.esPort = esPort;
    }

    public void setMacroIndex(String macroIndex) {
        this.macroIndex = macroIndex;
    }

    public void setMathoidUrl(String mathoidUrl) {
        this.mathoidUrl = mathoidUrl;
    }

    public List<String> getSuppressedMacros() {
        return settings.suppressedMacros;
    }

    public void setSuppressedMacros(List<String> suppressedMacros) {
        this.settings.suppressedMacros = suppressedMacros;
    }

    public List<String> getSupportDescriptions() {
        return settings.supportDescriptions;
    }

    public void setSupportDescriptions(List<String> supportDescriptions) {
        this.settings.supportDescriptions = supportDescriptions;
    }

    public ElasticSearchConfig getESConfig() {
        return new ElasticSearchConfig(
                esHost, esPort, macroIndex
        );
    }

    @JsonIgnore
    public RmiSubprocessInfo getMapleSubprocessInfo() {
        return mapleSubprocessInfo;
    }

    @JsonIgnore
    public void setMapleSubprocessInfo(RmiSubprocessInfo mapleSubprocessInfo) {
        this.mapleSubprocessInfo = mapleSubprocessInfo;
    }

    public static GenericLacastConfig getDefaultConfig() {
        return ConfigDiscovery.getConfig().getGenericLacastConfig();
    }

    private class Settings {
        @JsonProperty("max.relations")
        private int maxRelations = 3;

        @JsonProperty("max.macros")
        private int maxMacros = 5;

        @JsonProperty("max.depth")
        private int maxDepth = -1;

        @JsonProperty("suppressMacros")
        private List<String> suppressedMacros = new LinkedList<>();

        @JsonProperty("supportDescriptions")
        private List<String> supportDescriptions = new LinkedList<>();

        public Settings() {}
    }
}
