package gov.nist.drmf.interpreter.mathematica.config;

import com.wolfram.jlink.KernelLink;
import com.wolfram.jlink.MathLinkException;
import gov.nist.drmf.interpreter.common.config.CASConfig;
import gov.nist.drmf.interpreter.common.config.Config;
import gov.nist.drmf.interpreter.common.config.ConfigDiscovery;
import gov.nist.drmf.interpreter.common.config.RequirementChecker;
import gov.nist.drmf.interpreter.common.constants.Keys;
import gov.nist.drmf.interpreter.mathematica.extension.MathematicaInterface;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * @author Andre Greiner-Petter
 */
public class MathematicaConfig {
    private static final Logger LOG = LogManager.getLogger(MathematicaConfig.class.getName());

    private static final String STD_PATH_TO_MATH = "SystemFiles/Links/JLink/SystemFiles/Libraries/Linux-x86-64/";

    private MathematicaConfig(){}

    public static Path loadMathematicaPath(){
        Config config = ConfigDiscovery.getConfig();
        CASConfig mathConfig = config.getCasConfigs().get(Keys.KEY_MATHEMATICA);
        if ( mathConfig == null ) return null;
        Path baseInstallPath = mathConfig.getInstallPath();
        if ( baseInstallPath == null ) return null;
        return baseInstallPath.resolve("Executables/math");
    }

    public static String loadMathematicaLicense() {
        Config config = ConfigDiscovery.getConfig();
        CASConfig mathConfig = config.getCasConfigs().get(Keys.KEY_MATHEMATICA);
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

    public static boolean isSystemEnvironmentVariableProperlySet() {
        return RequirementChecker.validEnvVariable(
                Keys.SYSTEM_ENV_LD_LIBRARY_PATH,
                Keys.KEY_MATHEMATICA,
                "<mathematica-installation-path>/" + STD_PATH_TO_MATH,
                "SystemFiles/Links/JLink"
        );
    }

    public static boolean isMathematicaPresent() {
        try {
            if ( !isMathematicaMathPathAvailable() || !isSystemEnvironmentVariableProperlySet() ) return false;
            MathematicaInterface m = MathematicaInterface.getInstance();
            return m != null;
        } catch (Exception | Error e) {
            return false;
        }
    }
}
