package gov.nist.drmf.interpreter.generic.elasticsearch;

/**
 * @author Andre Greiner-Petter
 */
public class ElasticSearchConfig {
    private String host = "localhost";
    private int port = 9200;

    public ElasticSearchConfig(){}

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }
}
