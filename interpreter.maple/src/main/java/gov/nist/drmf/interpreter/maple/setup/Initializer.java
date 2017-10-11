package gov.nist.drmf.interpreter.maple.setup;

import gov.nist.drmf.interpreter.common.GlobalPaths;
import gov.nist.drmf.interpreter.common.Keys;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Properties;

/**
 * Created by AndreG-P on 17.02.2017.
 */
public class Initializer {
    private static final Logger LOG = LogManager.getLogger( Initializer.class );
    private static final String java_lib_path = "java.library.path";

    /**
     * Hacky version to load Maple's native libraries.
     * Probably won't work on Linux systems!
     *
     * @return true if everything went fine.
     */
    public static boolean loadMapleNatives() {
        return true;

        /*
        try ( FileInputStream in = new FileInputStream(GlobalPaths.PATH_MAPLE_CONFIG.toFile()) ){
            Properties props = new Properties();
            props.load(in);
            String maple_bin = props.getProperty(Keys.KEY_MAPLE_BIN);
            LOG.info( "Extracted maple dir: " + maple_bin );
            return loadMapleNatives( maple_bin );
        } catch ( IOException ioe ){
            LOG.fatal("Cannot load the maple native directory " +
                    "information from the given " + GlobalPaths.PATH_MAPLE_CONFIG.getFileName() +
                    " file.", ioe
            );
            return false;
        }
        */
    }

    public static boolean loadMapleNatives( String maple_bin_dir ){
        try {
            // set java.library.path
            System.setProperty( java_lib_path, maple_bin_dir );
            // refresh java.library.path, a bit hacky but it works fine.
            Field field = ClassLoader.class.getDeclaredField("sys_paths");
            field.setAccessible(true);
            field.set(null, null);
            return true;
        } catch ( IllegalAccessException | NoSuchFieldException e ) {
            LOG.fatal("The program is not able to refresh " +
                    "the java.library.path at the runtime! " +
                    "An alternative is to start the JVM with the following flag: " +
                    "-Djava.library.path=\"" + maple_bin_dir + "\"", e
            );
            return false;
        }
    }
}
