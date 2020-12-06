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
}
