package gov.nist.drmf.interpreter.mathematica.config;

import gov.nist.drmf.interpreter.common.GlobalPaths;
import gov.nist.drmf.interpreter.common.Keys;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * @author Andre Greiner-Petter
 */
public class MathematicaConfig {
    private static final Logger LOG = LogManager.getLogger(MathematicaConfig.class.getName());

    private MathematicaConfig(){}

    public static Path loadMathematicaPath(){
        try {
            Properties props = new Properties();
            props.load(new FileInputStream(GlobalPaths.PATH_MATHEMATICA_CONFIG.toString()));
            String path = props.getProperty(Keys.KEY_MATHEMATICA_MATH_DIR);
            return Paths.get(path);
        } catch (IOException e) {
            LOG.fatal( "Cannot write the path into the properties file.", e );
            return null;
        }
    }
}
