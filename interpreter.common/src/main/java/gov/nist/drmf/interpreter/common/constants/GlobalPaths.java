package gov.nist.drmf.interpreter.common.constants;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * This class provides some useful global constants.
 * <p>
 * Created by Andre Greiner-Petter on 02.11.2016.
 */
public final class GlobalPaths {
    private GlobalPaths() {
    }

    // path variable to libs folder
    private static final Path PATH_LIBS =
            Paths.get("libs");

    private static final Path PATH_CONFIGS =
            Paths.get("config");

    // path variable to the ReferenceData directory
    public static final Path PATH_REFERENCE_DATA =
            PATH_LIBS.resolve("ReferenceData");

    public static final Path PATH_MAPLE_CONFIG =
            PATH_CONFIGS.resolve("maple_config.properties");

    public static final Path PATH_MATHEMATICA_CONFIG =
            PATH_CONFIGS.resolve("mathematica_config.properties");

    public static final Path PATH_NUMERICAL_SETUP =
            PATH_CONFIGS.resolve("numerical_tests.properties");

    public static final Path PATH_SYMBOLIC_SETUP =
            PATH_CONFIGS.resolve("symbolic_tests.properties");

    public static final Path PATH_DLMF_REPLACEMENT_RULES =
            PATH_CONFIGS.resolve("dlmf-replacements.yml");

    public static final Path PATH_REPLACEMENT_RULES =
            PATH_CONFIGS.resolve("replacements.yml");

    public static final Path PATH_BLUEPRINTS =
            PATH_CONFIGS.resolve("blueprints.txt");

    public static final Path PATH_LIMES_BLUEPRINTS =
            PATH_CONFIGS.resolve("lim-blueprints.txt");

    public static final Path PATH_LIMITED_BLUEPRINTS =
            PATH_CONFIGS.resolve("limit-blueprints.txt");

    public static final Path PATH_ELASTICSEARCH_INDEX_CONFIG =
            PATH_CONFIGS.resolve("elasticsearch").resolve("index-config.json");

    // path variable to the lexicon files in the reference data dir
    public static final Path PATH_LEXICONS =
            PATH_REFERENCE_DATA.resolve("Lexicons");

    // path variable to the csv files in the reference data dir
    public static final Path PATH_REFERENCE_DATA_CSV =
            PATH_REFERENCE_DATA.resolve("CSVTables");

    // path variable to the csv files in the reference data dir
    public static final Path PATH_REFERENCE_DATA_BASIC_CONVERSION =
            PATH_REFERENCE_DATA.resolve("BasicConversions");

    // path to the CAS lexicon files (include CAS procedures)
    public static final Path PATH_REFERENCE_DATA_CAS_LEXICONS =
            PATH_REFERENCE_DATA.resolve("CASLexicons");

    // path to the macros definitions
    public static final Path PATH_REFERENCE_DATA_MACROS =
            PATH_REFERENCE_DATA.resolve("Macros");

    // macro definition file
    public static final Path PATH_SEMANTIC_MACROS_DEFINITIONS =
            PATH_REFERENCE_DATA_MACROS.resolve("DLMFfcns.sty");

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
            PATH_REFERENCE_DATA_CAS_LEXICONS.resolve("MapleProcedures");

    public static final Path PATH_MAPLE_LIST_PROCEDURE =
            PATH_MAPLE_PROCS.resolve("maple_list_procedure.txt");

    public static final Path PATH_MAPLE_TO_INERT_PROCEDURE =
            PATH_MAPLE_PROCS.resolve("maple_toinert_procedure.txt");

    public static final Path PATH_MAPLE_NUMERICAL_PROCEDURES =
            PATH_MAPLE_PROCS.resolve("maple_numerical_procedures.txt");

    public static final Path PATH_MAPLE_NUMERICAL_SIEVE_PROCEDURE =
            PATH_MAPLE_PROCS.resolve("maple_numerical_sieve.txt");

    public static final Path PATH_MATHEMATICA_PROCS =
            PATH_REFERENCE_DATA_CAS_LEXICONS.resolve("MathematicaProcedures");

    public static final Path PATH_MATHEMATICA_NUMERICAL_PROCEDURES =
            PATH_MATHEMATICA_PROCS.resolve("mathematica_numerical_procedures.txt");

    public static final Path PATH_MATHEMATICA_DIFFERENCE_PROCEDURES =
            PATH_MATHEMATICA_PROCS.resolve("difference_calc.txt");

    // path variable to the resources of the interpreter
    private static final Path PATH_CORE_RESOURCES =
            Paths.get("interpreter.common", "src", "main", "resources");

    public static final Path PATH_LOGGING_CONFIG =
            PATH_CORE_RESOURCES.resolve("log4j2.xml");

    public static final Path PATH_MACRO_CSV_FILE_NAME =
            Paths.get("DLMFMacro.csv");
}
