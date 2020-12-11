package gov.nist.drmf.interpreter.common.config;

import gov.nist.drmf.interpreter.common.constants.Keys;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * @author Andre Greiner-Petter
 */
public final class RequirementChecker {
    private static final Logger LOG = LogManager.getLogger(RequirementChecker.class.getName());

    private RequirementChecker() {}

    public static boolean validEnvVariable(
            String systemEnv,
            String name,
            String expectedPath,
            String mustContain
    ) {
        String variable = System.getenv(systemEnv);
        if ( variable == null ) {
            LOG.printf(Level.WARN,
                    "The system variable %s is required for %s but is not available. Set it to %s",
                    systemEnv, name, expectedPath
            );
            return false;
        }

        // check requirements in path
        String[] paths = variable.split(System.getProperty("path.separator"));
        // find wolfram installation path if there are multiple
        for ( String p : paths ) {
            if ( p.contains(mustContain) ) {
                Path path = Paths.get(p);
                if ( Files.exists(path) ) return true;
                else {
                    LOG.printf(Level.WARN,
                            "The specified path %s for %s does not exist! It should be %s",
                            p, name, expectedPath
                    );
                }
            }
        }

        return false;
    }

}
