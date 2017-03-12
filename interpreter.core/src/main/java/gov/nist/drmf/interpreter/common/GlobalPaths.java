package gov.nist.drmf.interpreter.common;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * This class provides some useful global constants.
 *
 * Created by Andre Greiner-Petter on 02.11.2016.
 */
public class GlobalPaths {
    // path variable to libs folder
    public static final Path PATH_LIBS =
            Paths.get("libs");

    // path variable to the ReferenceData directory
    public static final Path PATH_REFERENCE_DATA =
            PATH_LIBS.resolve( "ReferenceData" );

    public static final Path PATH_MAPLE_CONFIG =
            PATH_LIBS.resolve( "maple_config.properties" );

    // path variable to the lexicon files in the reference data dir
    public static final Path PATH_LEXICONS =
            PATH_REFERENCE_DATA.resolve( "Lexicons" );

    // path variable to the csv files in the reference data dir
    public static final Path PATH_REFERENCE_DATA_CSV =
            PATH_REFERENCE_DATA.resolve( "CSVTables" );

    // path variable to the csv files in the reference data dir
    public static final Path PATH_REFERENCE_DATA_BASIC_CONVERSION =
            PATH_REFERENCE_DATA.resolve( "BasicConversions" );

    // the name of the lexicon file
    public static final Path DLMF_MACROS_LEXICON =
            PATH_LEXICONS.resolve("DLMF-macros-lexicon.txt");

    public static final String DLMF_MACROS_LEXICON_NAME =
            DLMF_MACROS_LEXICON.getFileName().toString();

    // path to the json file with greek letters and constants
    public static final Path PATH_GREEK_LETTERS_AND_CONSTANTS_FILE =
            PATH_REFERENCE_DATA_BASIC_CONVERSION.resolve("GreekLettersAndConstants.json");

    // path to the json file with basic functions
    public static final Path PATH_BASIC_FUNCTIONS =
            PATH_REFERENCE_DATA_BASIC_CONVERSION.resolve("BasicFunctions.json");

    public static final Path PATH_MAPLE_PROCS =
            PATH_REFERENCE_DATA.resolve( "MapleProcedures" );


    public static final Path PATH_MAPLE_LIST_PROCEDURE =
            PATH_MAPLE_PROCS.resolve("maple_list_procedure.txt");

    public static final Path PATH_MAPLE_TO_INERT_PROCEDURE =
            PATH_MAPLE_PROCS.resolve("maple_toinert_procedure.txt");

    // path variable to the resources of the core
    public static final Path PATH_CORE_RESOURCES =
            Paths.get("interpreter.core", "src", "main", "resources");

    public static final Path PATH_LOGGING_CONFIG =
            PATH_CORE_RESOURCES.resolve("log4j2.xml");
}
