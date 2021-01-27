package gov.nist.drmf.interpreter.common.config;

/**
 * @author Andre Greiner-Petter
 */
public class ElasticSearchConfig {
    private final String host;

    private final String index;

    private final int port;

    public ElasticSearchConfig(){
        GenericLacastConfig c = GenericLacastConfig.getDefaultConfig();
        this.host = c.getEsHost();
        this.index = c.getMacroIndex();
        this.port = c.getEsPort();
    }

    public ElasticSearchConfig(String host, int port, String macroIndex) {
        this.host = host;
        this.port = port;
        this.index = macroIndex;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getIndex() {
        return index;
    }
}
