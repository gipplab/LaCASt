package gov.nist.drmf.interpreter.mlp;

import gov.nist.drmf.interpreter.common.constants.GlobalPaths;
import gov.nist.drmf.interpreter.common.constants.Keys;
import gov.nist.drmf.interpreter.mlp.data.CASCache;
import gov.nist.drmf.interpreter.mlp.data.LexiconConverterConfig;
import gov.nist.drmf.interpreter.mlp.data.Stats;
import mlp.Lexicon;
import mlp.LexiconFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author Andre Greiner-Petter
 */
public class CSVtoLexiconConverter {
    private static final Logger LOG = LogManager.getLogger( CSVtoLexiconConverter.class.toString() );

    private static final Pattern CAS_FILE_NAME_PATTERN = Pattern.compile("CAS_(\\w+)(\\.(txt|csv|TXT|CSV))?");

    private Lexicon lexicon;
    private CASCache casCache;
    private Stats stats;

    private Path dlmfLibPath;
    private List<Path> casLibPath;
    private List<String> casNames;

    private final LexiconConverterConfig config;

    public CSVtoLexiconConverter(Path csvDLMF, Path... csvCAS) throws Exception {
        this(new LexiconConverterConfig(), csvDLMF, csvCAS);
    }

    public CSVtoLexiconConverter (LexiconConverterConfig config, Path csvDLMF, Path... csvCAS )
            throws Exception
    {
        this.config = config;
        dlmfLibPath = config.getCsvPath().resolve(csvDLMF);

        casNames = new LinkedList<>();
        casLibPath = Arrays.stream(csvCAS)
                .sequential()
                .map(config.getCsvPath()::resolve)
                .map(p -> {
                    String fileName = p.getFileName().toString();
                    Matcher m = CAS_FILE_NAME_PATTERN.matcher(fileName);
                    if ( m.matches() ) {
                        casNames.add(m.group(1));
                        return p;
                    } else {
                        LOG.warn("File does not match file name rule: CAS_<casName>.csv");
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        stats = new Stats(casLibPath.size());
        casCache = new CASCache();

        LOG.info("Delete existing macros lexicon if exist.");
        Path lexiconFilePath = config.getDlmfMacroLexiconPath();
        Files.deleteIfExists(lexiconFilePath);
        Files.createFile(lexiconFilePath);

        try {
            LOG.info("Create new lexicon");
            lexicon = LexiconFactory.createLexicon(
                    lexiconFilePath.toString(),
                    MacrosLexicon.SIGNAL_ENTRY,
                    MacrosLexicon.SIGNAL_FEATURESET,
                    MacrosLexicon.SIGNAL_LINE,
                    MacrosLexicon.SIGNAL_INLINE
            );
        } catch ( IOException ioe ){
            LOG.error(
                    "Cannot read from lexicon file at " + lexiconFilePath.toString(),
                    ioe);
            throw ioe;
        }
    }

    public void generateLexiconFile(){
        Instant start = Instant.now();
        stats.reset();
        DLMFConsumer dlmfConsumer = new DLMFConsumer(stats, lexicon);

        LOG.info("Load DLMF base information");
        dlmfConsumer.parse(dlmfLibPath);

        for ( int i = 0; i < casNames.size(); i++ ) {
            String cas = casNames.get(i);
            Path csv = casLibPath.get(i);
            parseCAS(cas, csv);
        }

        LOG.info("Finished to load all data. Start writing lexicon.");
        LexiconFactory.outputLexiconMap(
                lexicon.getLexiconMap(),
                MacrosLexicon.SIGNAL_ENTRY,
                MacrosLexicon.SIGNAL_FEATURESET,
                config.getDlmfMacroLexiconPath().toString()
        );

        Instant end = Instant.now();
        Duration elapsed = Duration.between(start, end);
        logResults(elapsed);
    }

    private void parseCAS(String cas, Path csv){
        LOG.info("Start reading " + csv);
        CASConsumer casConsumer = new CASConsumer(casCache);
        TranslationConsumer translationConsumer = new TranslationConsumer(
                cas, lexicon, casCache, stats
        );

        LOG.info("Fill cache with "+cas+" function information.");
        try ( BufferedReader br = Files.newBufferedReader( csv ) ){
            LexiconConverterUtility.parseCSV( cas, br, casConsumer );
            LOG.info("Finished filling cache.");
        } catch ( IOException ioe ){
            LOG.error("Cannot build cache for " + cas, ioe);
        }

        Path translationFilePath = csv.getParent().resolve( getDLMFCasFileName(cas) );
        LOG.info("Fill lexicon with translation information.");
        try ( BufferedReader br = Files.newBufferedReader( translationFilePath ) ){
            LexiconConverterUtility.parseCSV( cas, br, translationConsumer );
            LOG.info("Finished lexicon for " + cas);
        } catch ( IOException ioe ){
            LOG.error("Cannot add lexicon information for " + cas, ioe);
        }

        LOG.info("Clear cache for " + cas);
        casCache.clear();
    }

    private String getDLMFCasFileName( String CAS ){
        return Keys.KEY_DLMF + "_" + CAS + ".csv";
    }

    private void logResults(Duration elapsed) {
        StringBuilder sb = new StringBuilder("Number of DLMF-Macros: ");
        sb.append(stats.getCountDLMF()).append("\n");
        casNames.forEach(n -> {
            sb.append("Number of supported ").append(n).append(" translations: ")
                    .append(stats.getCountCAS(n)).append("\n");
        });

        String info = String.format(
                "Successfully updated lexicon.\nTime elapsed: %2d,%3d seconds\n%s",
                elapsed.toSeconds(),
                elapsed.toMillisPart(),
                sb.toString()
        );
        LOG.info(info);
    }

    public static void analyzeInput(List<String> argumentList, String input) {
        if ( input.matches("CAS_.*\\.(?:csv|CSV)") ) {
            argumentList.add( input );
        } else if ( input.matches("-{0,2}[aA][lL]{2}") ) {
            argumentList.add("CAS_Maple.csv");
            argumentList.add("CAS_Mathematica.csv");
        } else {
            argumentList.add( "CAS_" + input + ".csv" );
        }
        System.out.println("Current list: " + argumentList);
    }

    public static Path[] convertToPaths(List<String> csvList, String[] programArgs) {
        Path[] csv_paths;

        if ( programArgs != null && programArgs.length > 0 ){
            csv_paths = new Path[programArgs.length];
            for ( int i = 0; i < programArgs.length; i++ ){
                csv_paths[i] = Paths.get( programArgs[i] );
            }
        } else {
            csv_paths = new Path[csvList.size()];
            for ( int i = 0; i < csvList.size(); i++ ){
                csv_paths[i] = Paths.get( csvList.get(i) );
            }
        }

        return csv_paths;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Welcome, this converter translates given CSV files to lexicon files.");
        ArrayList<String> argumentList = new ArrayList<>();

        if ( args == null || args.length == 0 ){
            System.out.println(
                    "You didn't specified CAS CSV files.\n" +
                    "Please enter the name of CAS you to generate (one CAS per line)\n" +
                    "or enter '-all' to fill up the list with default values.\n" +
                    "To exit, enter '-end'."
            );

            Scanner scanner = new Scanner(System.in);
            String input = scanner.nextLine();
            while ( input != null && !input.matches("\\s*['\"]*-end[\'\"]*\\s*") ){
                analyzeInput(argumentList, input);
                input = scanner.nextLine();
            }
            System.out.println("You added: " + argumentList.toString());
        }

        Path[] csvPaths = convertToPaths(argumentList, args);
        CSVtoLexiconConverter converter = new CSVtoLexiconConverter(
                GlobalPaths.PATH_MACRO_CSV_FILE_NAME, csvPaths
        );
        converter.generateLexiconFile();
    }
}