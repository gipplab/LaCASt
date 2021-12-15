package gov.nist.drmf.interpreter.maple.common;

import gov.nist.drmf.interpreter.common.config.CASConfig;
import gov.nist.drmf.interpreter.common.config.Config;
import gov.nist.drmf.interpreter.common.config.ConfigDiscovery;
import gov.nist.drmf.interpreter.common.config.RequirementChecker;
import gov.nist.drmf.interpreter.common.constants.Keys;
import gov.nist.drmf.interpreter.common.exceptions.CASUnavailableException;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * @author Andre Greiner-Petter
 */
public final class MapleConfig {
    private static final Logger LOG = LogManager.getLogger(MapleConfig.class.getName());

    private MapleConfig(){}

    public static boolean isMapleSetup() {
        return isMaplePathAvailable() &&
                areSystemVariablesSetProperly() &&
                isThreadStackIncreased();
    }

    public static boolean areSystemVariablesSetProperly() {
        boolean libExists = RequirementChecker.validEnvVariable(
                Keys.SYSTEM_ENV_LD_LIBRARY_PATH,
                Keys.KEY_MAPLE,
                "<maple-installation-path>",
                "maple\\d+/bin\\.X86_64_LINUX"
        );

        if ( !libExists ) return false;

        return RequirementChecker.validEnvVariable(
                Keys.SYSTEM_ENV_MAPLE,
                Keys.KEY_MAPLE,
                "for Linux: <maple-installation-path>/bin.X86_64_LINUX",
                "maple\\d+"
        );
    }

    public static boolean isThreadStackIncreased() {
        // try check JVM option -Xss is set
        try {
            RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
            List<String> arguments = runtimeMXBean.getInputArguments();
            for ( String arg : arguments ) {
                if ( arg.startsWith("-Xss") || arg.startsWith("-XX:ThreadStackSize") ) {
                    if ( arg.matches("-Xss\\d+(?:[MmGg][Bb]|[GgMm])?") || arg.matches("-XX:ThreadStackSize=\\d+(?:[MmGg][Bb]|[GgMm])?") ) {
                        // looks good, lets just continue
                    } else {
                        LOG.printf(Level.WARN,
                                "Identified setting of specific thread stack size %s but it looks too small. "+
                                        "You may not specify sufficient thread stack size for Maple 2019 or above. " +
                                        "Use 10M or more, otherwise Maple might crash.",
                                arg);
                    }
                    return true;
                }
            }
            // this means we reached the end without proper JVM option. This does not work for maple unfortunately.
            LOG.warn("Maple 2020 and above require a higher thread stack size to work properly. " +
                    "Specify it on JVM start via -Xss or -XX:ThreadStackSize. Use at least 10M, e.g., -Xss50M.");
            return false;
        } catch ( Error | Exception e ) {
            LOG.debug("Unable to check -Xss variable on this JVM. Let's hope the user properly set -Xss.");
            return true;
        }
    }

    private static CASConfig getMapleConfig() {
        Config config = ConfigDiscovery.getConfig();
        return config.getCasConfigs().get(Keys.KEY_MAPLE);
    }

    private static boolean isMaplePathAvailable() {
        Path mapleInstallPath = getMapleConfig().getInstallPath();
        if ( mapleInstallPath == null || !Files.exists(mapleInstallPath) ) {
            LOG.warn("Maple installation path is not available. Specify the proper path in lacast.config.yaml. " +
                    (mapleInstallPath != null ? "Broken path: " + mapleInstallPath : ""));
            return false;
        }

        return true;
    }

    public static Path getMapleJarPath() throws CASUnavailableException {
        return getNativeJarPath("Maple.jar");
    }

    public static Path getMapleExternalCallJarPath() throws CASUnavailableException {
        return getNativeJarPath("externalcall.jar");
    }

    private static Path getNativeJarPath(String jarName) throws CASUnavailableException {
        try {
            Path mapleInstallPath = getMapleConfig().getInstallPath();
            Path jar = mapleInstallPath.resolve("java").resolve(jarName);
            if ( Files.notExists(jar) ) {
                throw new CASUnavailableException("Unable to locate " + jarName + " at " + jar.toString());
            } else return jar;
        } catch (RuntimeException e) {
            throw new CASUnavailableException("Unable to locate " + jarName );
        }
    }
}
