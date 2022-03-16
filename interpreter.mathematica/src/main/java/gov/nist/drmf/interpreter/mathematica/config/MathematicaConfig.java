package gov.nist.drmf.interpreter.mathematica.config;

import gov.nist.drmf.interpreter.common.config.CASConfig;
import gov.nist.drmf.interpreter.common.config.Config;
import gov.nist.drmf.interpreter.common.config.ConfigDiscovery;
import gov.nist.drmf.interpreter.common.constants.Keys;
import gov.nist.drmf.interpreter.common.exceptions.CASUnavailableException;
import gov.nist.drmf.interpreter.mathematica.wrapper.JLinkWrapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.apache.commons.lang3.SystemUtils.IS_OS_LINUX;
import static org.apache.commons.lang3.SystemUtils.IS_OS_WINDOWS;

/**
 * @author Andre Greiner-Petter
 */
public class MathematicaConfig {
    private static final Logger LOG = LogManager.getLogger(MathematicaConfig.class.getName());

    private static Boolean mathematicaWasPresent = null;

    private MathematicaConfig() {}

    public static Path loadMathematicaPath(){
        CASConfig mathConfig = getMathConfig();
        if ( mathConfig == null ) return null;
        Path baseInstallPath = mathConfig.getInstallPath();
        if ( baseInstallPath == null ) return null;
        if ( IS_OS_WINDOWS ){
            return baseInstallPath.resolve("MathKernel.exe");
        } else {
            if ( !IS_OS_LINUX ) {
                LOG.warn("The system you are using is not Linux which may require a different " +
                        "Mathematica execution path (compared to Executables/math). " +
                        "Please open an issue on github.com/ag-gipp/LaCASt/issues if you face any " +
                        "issues starting Mathematica from here on.");
            }
            return baseInstallPath.resolve("Executables/math");
        }
    }

    private static CASConfig getMathConfig() {
        Config config = ConfigDiscovery.getConfig();
        return config.getCasConfigs().get(Keys.KEY_MATHEMATICA);
    }

    public static String loadMathematicaLicense() {
        CASConfig mathConfig = getMathConfig();
        if ( mathConfig == null ) return null;
        return mathConfig.getLicenseKey();
    }

    protected static boolean isMathematicaMathPathAvailable() {
        Path mathPath = MathematicaConfig.loadMathematicaPath();
        if ( mathPath == null || !Files.exists(mathPath) ) {
            LOG.warn("Mathematica installation path is not available. Specify the proper path in lacast.config.yaml. " +
                    (mathPath != null ? "Broken path: " + mathPath : ""));
            return false;
        }

        return true;
    }

    protected static boolean isMathematicaJLinkAvailable() {
        String pathStr = getJLinkNativePath();
        if ( pathStr == null || pathStr.isBlank() ) {
            LOG.warn("No JLink path for mathematica specified in lacast.config.yaml");
            return false;
        }

        Path jlinkPath = Paths.get(pathStr);
        if ( !Files.exists(jlinkPath) ) {
            LOG.warn("The path to the JLink library does not exist. Specify a proper path in lacast.config.yaml. " +
                    "(Broken path: "+ pathStr +")");
            return false;
        }

        return true;
    }

    public static String getJLinkNativePath() {
        return getMathConfig().getNativeLibraryPath();
    }

    /**
     * @return the path to the JLink.jar file based on the config
     * @throws CASUnavailableException if the JAR does not exist
     */
    public static Path getJLinkJarPath() throws CASUnavailableException {
        try {
            Path nativePath = Paths.get(getJLinkNativePath());
            Path jarFolderPath = nativePath.subpath(0, nativePath.getNameCount()-3);
            Path jarFilePath = jarFolderPath.resolve("JLink.jar");
            if ( nativePath.getRoot() != null ) {
                jarFilePath = nativePath.getRoot().resolve(jarFilePath);
            }
            if ( Files.notExists(jarFilePath) )
                throw new CASUnavailableException("Unable to locate JLink.jar under " + jarFilePath.toString());
            return jarFilePath;
        } catch (RuntimeException e) {
            throw new CASUnavailableException("Unable to find JLink.jar", e);
        }
    }

    /**
     * Checks if the KernelLink exists in the class path.
     * @return true if the KernelLink exists in the classpath
     */
    private static boolean isKernelClassAvailable() {
        try {
            Class.forName(
                    "com.wolfram.jlink.KernelLink",
                    false,
                    JLinkWrapper.getClassLoader()
            );
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Mathematica's presence is determined by three facts:
     * 1) The lacast.config.yaml sets a valid path to the install directory of mathematica
     * 2) The lacast.config.yaml defines a valid path to the system specific native library for JLink.
     * 3) The J/Link KernelLink class must be available in the classpath
     *
     * Note that this method no longer initiates mathematica connection itself!
     *
     * @return true if both facts are valid
     */
    public static boolean isMathematicaPresent() {
        // This avoids heavy IO checks all the time, either Mathematica is available on boot or never,
        // the state does not change during runtime!
        if ( mathematicaWasPresent == null ) {
            // the order matters: isKernelClassAvailable() should be the last check because it is 100% false
            // if one of the previous test failed
            mathematicaWasPresent =
                    isMathematicaMathPathAvailable() &&
                    isMathematicaJLinkAvailable() &&
                    isKernelClassAvailable();
        }
        return mathematicaWasPresent;
    }
}
