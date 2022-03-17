package gov.nist.drmf.interpreter.common.config;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.intellij.lang.annotations.Language;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Andre Greiner-Petter
 */
public final class RequirementChecker {
    private static final Logger LOG = LogManager.getLogger(RequirementChecker.class.getName());

    private RequirementChecker() {}

    /**
     * Checks if the given system environment variable exist and if the path it is set to also exist.
     * For example, if LD_LIBRARY_PATH is set and you want to check if all paths for (let's say Maple)
     * are present in LD_LIBRARY_PATH than you call this via
     *
     * <code>
     *     RequirementChecker.validEnvVariable(
     *            "LD_LIBRARY_PATH",
     *            "Maple",
     *            "<maple-installation-path>",
     *            "maple\d+/bin\.X86_64_LINUX"
     *     );
     * </code>
     *
     * This returns true if the LD_LIBRARY_PATH contains "maple2020/bin.X86_64_LINUX" and the given path
     * actually exist in on the system.
     *
     * @param systemEnv the system environment to check
     * @param name the name of the tool to check
     * @param expectedPath the expected path if it is not found (for insightful logging)
     * @param mustContain the environment must contain this expression (as regex)
     * @return true if the given system env variable is set properly, otherwise false
     */
    public static boolean validEnvVariable(
            String systemEnv,
            String name,
            String expectedPath,
            @Language("RegExp") String mustContain
    ) {
        String variable = System.getenv(systemEnv);
        if ( variable == null ) {
            LOG.printf(Level.WARN,
                    "The system variable %s is required for %s but is not available. Set it to, for example, '%s'",
                    systemEnv, name, expectedPath
            );
            return false;
        }

        Pattern mustContainPattern = Pattern.compile(mustContain);
        // check requirements in path
        String[] paths = variable.split(System.getProperty("path.separator"));
        // find wolfram installation path if there are multiple
        for ( String p : paths ) {
            Matcher m = mustContainPattern.matcher(p);
            if ( m.find() ) {
                Path path = Paths.get(p);
                if ( Files.exists(path) ) return true;
                else {
                    LOG.printf(Level.WARN,
                            "The specified path %s for %s does not exist! It should be something like: '%s'",
                            p, name, expectedPath
                    );
                }
            }
        }

        LOG.warn("Unable to find expected '" + expectedPath + "' in " + systemEnv + ": " + Arrays.toString(paths));
        return false;
    }

}
