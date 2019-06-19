package gov.nist.drmf.interpreter.mathematica.config;

import gov.nist.drmf.interpreter.common.GlobalPaths;
import gov.nist.drmf.interpreter.common.Keys;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Properties;

/**
 * @author Andre Greiner-Petter
 */
public class MathematicaConfig {

    public static final String MATH_KEY

    private MathematicaConfig(){}

    public static Path loadConfig(){
        try (FileOutputStream out =
                     new FileOutputStream(GlobalPaths.PATH_MATHEMATICA_CONFIG.toFile() )){
            Properties props = new Properties();
            props.setProperty( Keys.KEY_MAPLE_BIN, maple_dir.toAbsolutePath().toString() );
            props.store(out, GlobalConstants.PROPS_COMMENTS);
            LOG.debug("Finished to setup the properties file.");
        } catch ( IOException ioe ){
            LOG.fatal( "Cannot write the path into the properties file.", ioe );
        }
    }
}
