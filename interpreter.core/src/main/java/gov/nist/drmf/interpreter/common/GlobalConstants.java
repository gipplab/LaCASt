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

    // path variable to the lexicon files in the reference data dir
    public static final Path PATH_LEXICONS =
            Paths.get("libs", "ReferenceData", "Lexicons");

    // path variable to the csv files in the reference data dir
    public static final Path PATH_REFERENCE_DATA_CSV =
            Paths.get("libs", "ReferenceData", "CSVTables");

    // path variable to the csv files in the reference data dir
    public static final Path PATH_REFERENCE_DATA_BASIC_CONVERSION =
            Paths.get("libs", "ReferenceData", "BasicConversions");

    // path variable to the resources of the core
    public static final Path PATH_CORE_RESOURCES =
            Paths.get("interpreter.core", "src", "main", "resources");

    // path to the json file with greek letters and constants
    public static final Path PATH_GREEK_LETTERS_AND_CONSTANTS_FILE =
            Paths.get("libs", "ReferenceData", "BasicConversions", "GreekLettersAndConstants.json");

    // path to the json file with basic functions
    public static final Path PATH_BASIC_FUNCTIONS =
            Paths.get("libs", "ReferenceData", "BasicConversions", "BasicFunctions.json");

    public static final Path PATH_MAPLE_PROCEDURE =
            Paths.get("libs", "ReferenceData", "MapleProcedures", "maple_list_procedure.txt");

    // the name of the lexicon file
    public static final String DLMF_MACROS_LEXICON_NAME =
            "DLMF-macros-lexicon.txt";
}
