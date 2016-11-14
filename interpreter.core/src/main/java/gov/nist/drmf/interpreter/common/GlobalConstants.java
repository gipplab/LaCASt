package gov.nist.drmf.interpreter.common;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * This class provides some useful global constants.
 *
 * Created by Andre Greiner-Petter on 02.11.2016.
 */
public class GlobalConstants {
    // path variable to libs folder
    public static final Path PATH_LIBS =
            Paths.get("libs");

    // path variable to the ReferenceData directory
    public static final Path PATH_REFERENCE_DATA =
            Paths.get("libs", "ReferenceData");

    // path variable to the resources of the core
    public static final Path PATH_CORE_RESOURCES =
            Paths.get("interpreter.core", "src", "main", "resources");

    // path to the json file with greek letters and constants
    public static final Path PATH_GREEK_LETTERS_AND_CONSTANTS_FILE =
            Paths.get("libs", "GreekLettersAndConstants.json");

    // Key value for LaTeX
    public static final String KEY_LATEX = "LaTeX";

    // Key value for Maple
    public static final String KEY_MAPLE = "Maple";

    // Key value for Mathematica
    public static final String KEY_MATHEMATICA = "Mathematica";

    // Key value for DLMF
    public static final String KEY_DLMF = "DLMF-Macro";

    // this will be setup on runtime
    public static String CAS_KEY = "";
}
