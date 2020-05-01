package gov.nist.drmf.interpreter.mlp;

import gov.nist.drmf.interpreter.common.constants.GlobalPaths;
import gov.nist.drmf.interpreter.common.constants.Keys;
import gov.nist.drmf.interpreter.mlp.data.CASCache;
import gov.nist.drmf.interpreter.mlp.data.Stats;
import mlp.Lexicon;
import mlp.LexiconFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
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

    public CSVtoLexiconConverter ( Path csvDLMF, Path... csvCAS ) throws Exception {
        dlmfLibPath = GlobalPaths.PATH_REFERENCE_DATA_CSV.resolve(csvDLMF);

        casNames = new LinkedList<>();
        casLibPath = Arrays.stream(csvCAS)
                .sequential()
                .map(GlobalPaths.PATH_REFERENCE_DATA_CSV::resolve)
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
        Path lexiconFilePath = GlobalPaths.DLMF_MACROS_LEXICON;
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
                GlobalPaths.DLMF_MACROS_LEXICON.toString()
        );

        Instant end = Instant.now();
        Duration elapsed = Duration.between(start, end);

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

    public static void main(String[] args){
//        System.setProperty( Keys.KEY_SYSTEM_LOGGING, GlobalPaths.PATH_LOGGING_CONFIG.toString() );

        String welcome =
                "Welcome, this converter translates given CSV files to lexicon files.";
        System.out.println(welcome);
        ArrayList<String> csv_list = new ArrayList<>();

        if ( args == null || args.length == 0 ){
            System.out.println("You didn't specified CSV files (do not add DLMFMacro.csv).");
            Scanner scanner = new Scanner(System.in);
            System.out.println("Add a new CSV file and hit enter or enter \'-end\' to stop the adding process.");
            String input = scanner.nextLine();
            while ( input != null && !input.matches("\\s*[\'\"]*-end[\'\"]*\\s*") ){
                if ( input.matches("CAS_.*\\.(?:csv|CSV)") ) {
                    csv_list.add( input );
                } else if ( input.matches("-{0,2}[aA][lL]{2}") ) {
                    csv_list.add("CAS_Maple.csv");
                    csv_list.add("CAS_Mathematica.csv");
                } else {
                    csv_list.add( "CAS_" + input + ".csv" );
                }
                System.out.println("Current list: " + csv_list);
                input = scanner.nextLine();
            }
            System.out.println("You added: " + csv_list.toString());
        }

        Path[] csv_paths;
        if ( args != null && args.length > 0 ){
            csv_paths = new Path[args.length];
            for ( int i = 0; i < args.length; i++ ){
                csv_paths[i] = Paths.get( args[i] );
            }
        } else {
            csv_paths = new Path[csv_list.size()];
            for ( int i = 0; i < csv_list.size(); i++ ){
                csv_paths[i] = Paths.get( csv_list.get(i) );
            }
        }

        try{
            CSVtoLexiconConverter csvConv = new CSVtoLexiconConverter(
                    GlobalPaths.PATH_MACRO_CSV_FILE_NAME, csv_paths
            );
            csvConv.generateLexiconFile();
        } catch (Exception e){
            e.printStackTrace();
        }
    }
}