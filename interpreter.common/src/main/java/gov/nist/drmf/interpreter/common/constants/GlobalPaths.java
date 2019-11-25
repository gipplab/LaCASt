package gov.nist.drmf.interpreter.common.constants;

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

    public static final Path PATH_MACRO_CSV_FILE_NAME =
            Paths.get("DLMFMacro.csv");

    public static final Path PATH_MAPLE_CONFIG =
            PATH_LIBS.resolve( "maple_config.properties" );

    public static final Path PATH_MATHEMATICA_CONFIG =
            PATH_LIBS.resolve( "mathematica_config.properties" );

    public static final Path PATH_NUMERICAL_SETUP =
            PATH_LIBS.resolve( "numerical_tests.properties" );

    public static final Path PATH_SYMBOLIC_SETUP =
            PATH_LIBS.resolve( "symbolic_tests.properties" );

    // path variable to the lexicon files in the reference data dir
    public static final Path PATH_LEXICONS =
            PATH_REFERENCE_DATA.resolve( "Lexicons" );

    // path variable to the csv files in the reference data dir
    public static final Path PATH_REFERENCE_DATA_CSV =
            PATH_REFERENCE_DATA.resolve( "CSVTables" );

    // path variable to the csv files in the reference data dir
    public static final Path PATH_REFERENCE_DATA_BASIC_CONVERSION =
            PATH_REFERENCE_DATA.resolve( "BasicConversions" );

    public static final Path PATH_REFERENCE_DATA_CAS_LEXICONS =
            PATH_REFERENCE_DATA.resolve( "CASLexicons" );

    // the name of the lexicon file
    public static final Path DLMF_MACROS_LEXICON =
            PATH_LEXICONS.resolve("DLMF-macros-lexicon.txt");

    public static final String DLMF_MACROS_LEXICON_NAME =
            DLMF_MACROS_LEXICON.getFileName().toString();

    public static final Path PATH_MAPLE_FUNCTIONS_LEXICON_FILE =
            PATH_REFERENCE_DATA_CAS_LEXICONS.resolve("Maple-functions-lexicon.txt");

    // path to the json file with greek letters and constants
    public static final Path PATH_GREEK_LETTERS_AND_CONSTANTS_FILE =
            PATH_REFERENCE_DATA_BASIC_CONVERSION.resolve("GreekLettersAndConstants.json");

    // path to the json file with basic functions
    public static final Path PATH_BASIC_FUNCTIONS =
            PATH_REFERENCE_DATA_BASIC_CONVERSION.resolve("BasicFunctions.json");

    public static final Path PATH_MAPLE_PROCS =
            PATH_REFERENCE_DATA_CAS_LEXICONS.resolve( "MapleProcedures" );

    public static final Path PATH_MAPLE_LIST_PROCEDURE =
            PATH_MAPLE_PROCS.resolve("maple_list_procedure.txt");

    public static final Path PATH_MAPLE_TO_INERT_PROCEDURE =
            PATH_MAPLE_PROCS.resolve("maple_toinert_procedure.txt");

    public static final Path PATH_MAPLE_NUMERICAL_PROCEDURES =
            PATH_MAPLE_PROCS.resolve("maple_numerical_procedures.txt");

    public static final Path PATH_MAPLE_NUMERICAL_SIEVE_PROCEDURE =
            PATH_MAPLE_PROCS.resolve("maple_numerical_sieve.txt");

    // path variable to the resources of the interpreter
    public static final Path PATH_CORE_RESOURCES =
            Paths.get("interpreter.common", "src", "main", "resources");

    public static final Path PATH_LOGGING_CONFIG =
            PATH_CORE_RESOURCES.resolve("log4j2.xml");
}
