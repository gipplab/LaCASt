package gov.nist.drmf.interpreter.mathematica.config;

import gov.nist.drmf.interpreter.common.config.CASConfig;
import gov.nist.drmf.interpreter.common.config.Config;
import gov.nist.drmf.interpreter.common.config.ConfigDiscovery;
import gov.nist.drmf.interpreter.common.config.RequirementChecker;
import gov.nist.drmf.interpreter.common.constants.Keys;
import gov.nist.drmf.interpreter.mathematica.extension.MathematicaInterface;
import gov.nist.drmf.interpreter.mathematica.wrapper.KernelLink;
import gov.nist.drmf.interpreter.mathematica.wrapper.MathLinkException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.apache.commons.lang3.SystemUtils.IS_OS_LINUX;
import static org.apache.commons.lang3.SystemUtils.IS_OS_WINDOWS;

/**
 * @author Andre Greiner-Petter
 */
public class MathematicaConfig {
    private static final Logger LOG = LogManager.getLogger(MathematicaConfig.class.getName());

    private MathematicaConfig() {
    }

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

    public static void setCharacterEncoding(KernelLink engine){
        try {
            engine.evaluate("$CharacterEncoding = \"ASCII\"");
            engine.discardAnswer();
        } catch (MathLinkException mle) {
            LOG.warn("Cannot change character encoding in Mathematica.");
            engine.clearError();
            engine.newPacket();
        }
    }

    public static boolean isMathematicaMathPathAvailable() {
        Path mathPath = MathematicaConfig.loadMathematicaPath();
        if ( mathPath == null || !Files.exists(mathPath) ) {
            LOG.warn("Mathematica installation path is not available. Specify the proper path in lacast.config.yaml.");
            return false;
        }
        return true;
    }

    /**
     * Checks if the LD_LIBRARY_PATH environment variable is properly set
     *
     * @return true if the library path is correctly set otherwise false
     */
    public static boolean isSystemEnvironmentVariableProperlySet() {
        return RequirementChecker.validEnvVariable(
                Keys.SYSTEM_ENV_LD_LIBRARY_PATH,
                Keys.KEY_MATHEMATICA,
                // this path is just an example for proper logging messages
                "<mathematica-installation-path>/SystemFiles/Links/JLink/SystemFiles/Libraries/Linux-x86-64/",
                // this is the regex that should appear in the path to pass the test
                "SystemFiles[/\\\\]Links[/\\\\]JLink"
        );
    }

    public static boolean isMathematicaPresent() {
        try {
            if (!isMathematicaMathPathAvailable() || !isSystemEnvironmentVariableProperlySet()) return false;
            MathematicaInterface m = MathematicaInterface.getInstance();
            return m != null;
        } catch (Exception | Error e) {
            return false;
        }
    }

    public static String getjLinkNativePath() {
        return getMathConfig().getNativeLibraryPath();
    }
}
