package gov.nist.drmf.interpreter.common.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;

/**
 * The class tries to load the paths for the config and the reference directories from
 * an external configuration file. The implemented logic try to discover the config file
 * from
 *  1) the environment variable {@code LACAST_CONFIG}, or
 *  2) lookup current working directory for the {@code lacast.config.yaml} file, or
 *  3) loads the default config from the resources.
 * The logic always follow this order. Hence, the environment variable always overwrites
 * the other configurations.
 *
 * @author Andre Greiner-Petter
 */
public final class ConfigDiscovery {
    private static final Logger LOG = LogManager.getLogger(ConfigDiscovery.class.getName());

    private static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory())
                    .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);

    private static Config config = null;

    private ConfigDiscovery(){}

    public static Config getConfig() {
        if ( config != null ) return config;

        Instant start = Instant.now();
        LOG.debug("Loading config");
        config = loadConfigFromEnvironmentVariable();
        if ( isValidConfig(config) ) {
            loadedLog("config from environment variable", start);
            return config;
        }
        config = loadConfigFromLocalDir();
        if ( isValidConfig(config) ) {
            loadedLog("config from working directory", start);
            return config;
        }
        config = loadConfigFromResources();
        if ( isValidConfig(config) ) {
            loadedLog("default config from resources", start);
            return config;
        }

        LOG.fatal("Unable to load configuration file. " +
                "Neither from environment variables nor from the local or resource path.");
        System.exit(1);
        return config;
    }

    private static void loadedLog(String msg, Instant start) {
        Instant end = Instant.now();
        LOG.printf(Level.INFO, "Loaded %s [%dms]", msg, Duration.between(start, end).toMillis());
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
            LOG.warn("Unable to read system environment variables " +
                    "because the security manager does not allow it: " + se.getMessage());
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
            LOG.warn("Unable to load config from the resources. " +
                    "This should always work as a fallback. Contact the developer!", ioe);
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
            LOG.debug("Unable to load config from " + path + ": " + e.getMessage());
            return null;
        }
    }
}
