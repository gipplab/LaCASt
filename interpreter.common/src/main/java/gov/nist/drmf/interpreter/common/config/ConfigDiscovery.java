package gov.nist.drmf.interpreter.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author Andre Greiner-Petter
 */
public final class ConfigDiscovery {
    private static final Logger LOG = LogManager.getLogger(ConfigDiscovery.class.getName());

    private static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory());

    private static Config config = null;

    private ConfigDiscovery(){}

    public static Config getConfig() {
        if ( config != null ) return config;

        config = loadConfigFromEnvironmentVariable();
        if ( isValidConfig(config) ) return config;
        config = loadConfigFromLocalDir();
        if ( isValidConfig(config) ) return config;
        config = loadConfigFromResources();
        if ( isValidConfig(config) ) return config;

        LOG.fatal("Unable to load configuration file. " +
                "Neither from environment variables nor from the local or resource path.");
        System.exit(1);
        return config;
    }

    private static boolean isValidConfig(Config config) {
        if ( config == null ) return false;
        boolean valid = config.isValid();
        if ( !valid ) {
            LOG.warn("Found an invalid configuration file. " +
                    "The given paths do not exist or do not contain necessary files.");
        }
        return valid;
    }

    private static Config loadConfigFromEnvironmentVariable() {
        LOG.debug("Try to load config from environment variable LACAST_CONFIG");
        try {
            String evPath = System.getenv("LACAST_CONFIG");
            return loadConfigFromYamlPath(evPath);
        } catch ( SecurityException se ) {
            LOG.debug("Unable to read system environment variable.");
            return null;
        }
    }

    private static Config loadConfigFromLocalDir() {
        LOG.debug("Try to load config from the local directory.");
        Path localPath = Paths.get("./lacast.config.yaml");
        return loadConfigFromYamlPath(localPath);
    }

    private static Config loadConfigFromResources() {
        LOG.debug("Try to load config from resources.");
        try {
            URL url = ClassLoader.getSystemResources("lacast.config.yaml").nextElement();
            return MAPPER.readValue(url, Config.class);
        } catch (IOException ioe) {
            LOG.error("Unable to load config from resources. " +
                    "This should always work as a fallback.", ioe);
            return null;
        }
    }

    private static Config loadConfigFromYamlPath(String path) {
        if ( path == null ) return null;
        return loadConfigFromYamlPath(Paths.get(path));
    }

    private static Config loadConfigFromYamlPath(Path path) {
        if (!Files.exists(path)) return null;
        try {
            return MAPPER.readValue(path.toFile(), Config.class);
        } catch (Exception e) {
            LOG.trace("Unable to load config from " + path + ": " + e.getMessage());
            return null;
        }
    }
}
