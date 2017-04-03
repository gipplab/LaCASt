package gov.nist.drmf.interpreter.cas.mlp;

import com.sun.org.apache.xerces.internal.impl.xpath.regex.Match;
import gov.nist.drmf.interpreter.common.GlobalConstants;
import gov.nist.drmf.interpreter.common.GlobalPaths;
import gov.nist.drmf.interpreter.common.Keys;
import gov.nist.drmf.interpreter.mlp.extensions.MacrosLexicon;
import jdk.nashorn.internal.objects.Global;
import mlp.FeatureSet;
import mlp.Lexicon;
import mlp.LexiconFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Andre Greiner-Petter
 */
public class CSVtoLexiconConverter {
    protected final Logger ERROR_LOG = Logger.getLogger( CSVtoLexiconConverter.class.toString() );

    private Pattern cas_file_name_pattern =
            Pattern.compile("CAS_(\\w+)(\\.(txt|csv|TXT|CSV))?");

    public static final String DELIMITER = ";";

    private Path path_to_dlmf_lexicon;

    private Lexicon dlmf_lexicon;

    private Path csv_dlmf_file;

    private Path[] csv_cas_files;

    private LineAnalyzer lineAnalyzer;

    private String[] header;

//    private static int
//            internal_dlmf_counter,
//            internal_maple_trans_counter;


    public CSVtoLexiconConverter ( Path CSV_dlmf_file, Path... CSV_CAS_files ) throws Exception {
//        internal_dlmf_counter = 0;
//        internal_maple_trans_counter = 0;

        this.csv_dlmf_file =
                GlobalPaths.PATH_REFERENCE_DATA_CSV.resolve(CSV_dlmf_file);
        if ( !csv_dlmf_file.toFile().exists() )
            throw new FileNotFoundException(
                    "The given link to the CSV-DLMF file doesn't exists! " + csv_dlmf_file.toString()
            );

        this.csv_cas_files = CSV_CAS_files;
        for ( int i = 0; i < csv_cas_files.length; i++ ) {
            csv_cas_files[i] = GlobalPaths.PATH_REFERENCE_DATA_CSV.resolve(csv_cas_files[i]);
            if (!csv_cas_files[i].toFile().exists())
                throw new FileNotFoundException(
                        "The given CSV-CAS file doesn't exists. " + csv_cas_files[i].toString()
                );
        }

        path_to_dlmf_lexicon = GlobalPaths.DLMF_MACROS_LEXICON;
        File dlmf_lexicon_file = path_to_dlmf_lexicon.toFile();
        if ( !dlmf_lexicon_file.exists() ) {
            try {
                dlmf_lexicon_file.createNewFile();
            } catch ( IOException ioe ){
                ERROR_LOG.log(
                        Level.SEVERE,
                        "Cannot create a new lexicon file at " + path_to_dlmf_lexicon.toString(),
                        ioe);
                throw ioe;
            }
        }

        try {
            dlmf_lexicon = LexiconFactory.createLexicon(
                    path_to_dlmf_lexicon,
                    MacrosLexicon.SIGNAL_ENTRY,
                    MacrosLexicon.SIGNAL_FEATURESET,
                    MacrosLexicon.SIGNAL_LINE,
                    MacrosLexicon.SIGNAL_INLINE);
        } catch ( IOException ioe ){
            ERROR_LOG.log(
                    Level.SEVERE,
                    "Cannot read from lexicon file at " + path_to_dlmf_lexicon.toString(),
                    ioe);
            throw ioe;
        }
    }

    public void generateLexiconFile(){
        generateBasicDLMF();
        if ( csv_cas_files != null )
            for( Path csv : csv_cas_files )
                addCAS(csv);

        LexiconFactory.outputLexiconMap(
                dlmf_lexicon.getLexiconMap(),
                MacrosLexicon.SIGNAL_ENTRY,
                MacrosLexicon.SIGNAL_FEATURESET,
                path_to_dlmf_lexicon.toString()
        );
    }

    private void generateBasicDLMF(){
        try (BufferedReader br = Files.newBufferedReader(csv_dlmf_file) ){
            startReadingProcess( null, br, this::handleDLMFElements );
        } catch ( IOException ioe ){
            ioe.printStackTrace();
        }
    }

    private void addCAS( Path csv ){
        try ( BufferedReader br = Files.newBufferedReader( csv ) ){
            String fname = csv.getFileName().toString();
            Matcher m = cas_file_name_pattern.matcher(fname);
            if ( !m.matches() ){
                ERROR_LOG.warning("Wrong style of file name: " + fname + ". " +
                        "Must be CAS_<cas_name>.csv");
            } else {
                startReadingProcess( m.group(1), br, this::handleCasAddOn );
            }
        } catch ( IOException ioe ){
            ioe.printStackTrace();
        }
    }

    private void startReadingProcess( String additional_prefix,
                                      BufferedReader br,
                                      Consumer<? super String[]> method )
            throws IOException {
        String headerLine = br.readLine();

        if ( headerLine != null && !headerLine.isEmpty() ){
            header = headerLine.split( DELIMITER );
            lineAnalyzer = new LineAnalyzer( additional_prefix, DELIMITER, header );
        } else {
            throw new IOException("The header is empty! " + csv_dlmf_file.toString());
        }

        br.lines()//.limit(320) // TODO limit for debug
                .filter( line -> !line.startsWith( DELIMITER ) )
                .map( line -> line += line.endsWith(DELIMITER) ? " " : "" )
                .map( line -> line.split(DELIMITER) )
                .forEach( method );
    }

    private void handleOptionalParameters( Matcher m, String[] elements ){
        String mac = m.group(GlobalConstants.MACRO_PATTERN_INDEX_OPT_PARAS);
        mac = mac.substring(1, mac.length()-1);
        String[] info = mac.split( GlobalConstants.MACRO_OPT_PARAS_SPLITTER );

        Integer opt_para = Integer.parseInt(info[0]);

        List<FeatureSet> sets = dlmf_lexicon.getFeatureSets( info[1] );
        if ( sets == null )
            sets = new LinkedList<>();

        FeatureSet fset = new FeatureSet( Keys.KEY_DLMF_MACRO_OPTIONAL_PREFIX + opt_para );
        fset.addFeature(
                Keys.KEY_DLMF,
                m.group(0).substring(mac.length()+2),
                MacrosLexicon.SIGNAL_INLINE
        );

        // add all other information to the feature set
        for ( int i = 1; i < elements.length && i < header.length; i++ ){
            String value = lineAnalyzer.getValue( header[i] );
            if ( value != null && !value.isEmpty() )
                fset.addFeature( header[i], value, MacrosLexicon.SIGNAL_INLINE );
        }
        sets.add(fset);

        dlmf_lexicon.setEntry( info[1], sets );
    }

    private void handleDLMFElements( String[] elements ){
        lineAnalyzer.setLine(elements);

        // check if the input is a correct DLMF macro
        String macro = lineAnalyzer.getValue( Keys.KEY_DLMF );
        Matcher m = GlobalConstants.DLMF_MACRO_PATTERN.matcher( macro );
        if ( !m.matches() ){
            ERROR_LOG.info("Found a not supported DLMF macro: " + macro);
            return;
        }

        String optional_ats = m.group( GlobalConstants.MACRO_PATTERN_INDEX_OPT_PARAS );
        if ( optional_ats != null ){
            handleOptionalParameters(m, elements);
            return;
        }

        String macro_name = m.group(GlobalConstants.MACRO_PATTERN_INDEX_MACRO);

        // find out if it is a mathematical constant
        String role = lineAnalyzer.getValue( Keys.FEATURE_ROLE );

        // otherwise it is a usual DLMF macro and we can create our feature set for it
        // create a new feature set
        FeatureSet fset;
        if ( role.matches( Keys.FEATURE_VALUE_SYMBOL ) ) {
            // TODO we should handle symbols in a different way
            fset = new FeatureSet(Keys.KEY_DLMF_MACRO);
        }
        else if ( role.matches( Keys.FEATURE_VALUE_CONSTANT ) ){
            fset = new FeatureSet(Keys.KEY_DLMF_MACRO);
            // add the general representation for this macro
            fset.addFeature( Keys.KEY_DLMF, macro, MacrosLexicon.SIGNAL_INLINE );

            String dlmf_link = Keys.KEY_DLMF + Keys.KEY_LINK_SUFFIX;
            fset.addFeature( dlmf_link, lineAnalyzer.getValue(dlmf_link), MacrosLexicon.SIGNAL_INLINE );
            fset.addFeature( Keys.FEATURE_MEANINGS, lineAnalyzer.getValue(Keys.FEATURE_MEANINGS), MacrosLexicon.SIGNAL_INLINE );
            fset.addFeature( Keys.FEATURE_ROLE, Keys.FEATURE_VALUE_CONSTANT, MacrosLexicon.SIGNAL_INLINE );
            List<FeatureSet> fsets = new LinkedList<>();
            fsets.add(fset);
            dlmf_lexicon.setEntry(
                    macro_name,
                    fsets
            );
            //internal_dlmf_counter++;
            return;
        } else if ( role.matches( Keys.FEATURE_VALUE_FUNCTION ) ){
            fset = new FeatureSet( Keys.FEATURE_VALUE_FUNCTION );
        } else if ( role.matches( Keys.FEATURE_VALUE_IGNORE ) )
            return;
        else fset = new FeatureSet( Keys.KEY_DLMF_MACRO );

        // add the general representation for this macro
        fset.addFeature( Keys.KEY_DLMF, macro, MacrosLexicon.SIGNAL_INLINE );

        // add all other information to the feature set
        for ( int i = 1; i < elements.length && i < header.length; i++ ){
            String value = lineAnalyzer.getValue( header[i] );
            if ( value != null && !value.isEmpty() )
                fset.addFeature( header[i], value, MacrosLexicon.SIGNAL_INLINE );
        }

        // since each DLMF macro has only one feature set, create a list with one element
        List<FeatureSet> fsets = new LinkedList<>();
        fsets.add(fset);

        // group(1) is the DLMF macro without the suffix of parameters, ats and variables
        // just the plain macro
        dlmf_lexicon.setEntry( macro_name, fsets );
        //internal_dlmf_counter++;
    }

    private void handleCasAddOn( String[] elements ){
        lineAnalyzer.setLine( elements );

        //TODO
        //System.out.println( Arrays.toString(elements) );

        String macro_col = lineAnalyzer.getValue( Keys.KEY_DLMF );
        Matcher m = GlobalConstants.DLMF_MACRO_PATTERN.matcher( macro_col );
        if ( !m.matches() ){
            ERROR_LOG.info("Found a not supported DLMF macro for translation: " + macro_col);
            return;
        }

        String macro = m.group(GlobalConstants.MACRO_PATTERN_INDEX_OPT_PARAS);
        if ( macro == null ) {
            macro = m.group( GlobalConstants.MACRO_PATTERN_INDEX_MACRO );
        } else {
            macro = macro.substring(1, macro.length()-1);
            macro = macro.split( GlobalConstants.MACRO_OPT_PARAS_SPLITTER )[1];
        }
        List<FeatureSet> list = dlmf_lexicon.getFeatureSets(macro);

        if ( list == null || list.isEmpty() ){
            ERROR_LOG.info("SKIP "
                    + m.group(GlobalConstants.MACRO_PATTERN_INDEX_MACRO)
                    + " (Reason: Cannot find FeatureSet)" );
            return;
        }

        FeatureSet alternativeF = null;
        FeatureSet dlmfF = null;
        for ( FeatureSet f : list ){
            if ( f.getFeatureSetName().matches( Keys.KEY_DLMF_MACRO ) ) {
                dlmfF = f;
            } else if ( f.getFeatureSetName().matches( Keys.KEY_DLMF_MACRO_OPTIONAL_PREFIX+"\\d+" ) ) {
                alternativeF = f;
            }
        }

        FeatureSet fset;

        String opt_para = m.group(GlobalConstants.MACRO_PATTERN_INDEX_OPT_PARAS);
        String paras = m.group(GlobalConstants.MACRO_PATTERN_INDEX_OPT_PARAS_ELEMENTS);
        if ( opt_para != null ){
            ERROR_LOG.warning("OptPARA not null: " + opt_para);
            if ( alternativeF == null ){
                ERROR_LOG.warning("Null alternative set! " + macro_col);
                return;
            }
            fset = alternativeF;
        } else if (paras != null) {
            ERROR_LOG.warning("Parameters not in special syntax. " +
                    "Has to be defined as 'X<digit>X<Macro>'. " + macro_col);
            return;
        } else {
            if ( dlmfF == null ){
                ERROR_LOG.warning("There is no feature set for this term? " + macro_col);
                return;
            }
            fset = dlmfF;
        }

        //FeatureSet fset = list.get(0);
        String casPrefix = lineAnalyzer.getCasPrefix();
        for ( DLMFTranslationHeaders h : DLMFTranslationHeaders.values() ){
            String value = lineAnalyzer.getValue( h.getCSVKey( casPrefix ) );
            if ( opt_para != null ){
                ERROR_LOG.warning(h.getCSVKey( casPrefix ) + ": " + value);
            }


            if ( value != null && !value.isEmpty() ){
                fset.addFeature( h.getFeatureKey(casPrefix), value, MacrosLexicon.SIGNAL_INLINE );
            }
        }

        dlmf_lexicon.setEntry( macro, list );
    }

    public static void main(String[] args){
        System.setProperty( Keys.KEY_SYSTEM_LOGGING, GlobalPaths.PATH_LOGGING_CONFIG.toString() );

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
                csv_list.add( input );
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

        long start = System.currentTimeMillis();
        try{
            CSVtoLexiconConverter csvConv = new CSVtoLexiconConverter(
                GlobalPaths.PATH_MACRO_CSV_FILE_NAME, csv_paths
            );
            csvConv.generateLexiconFile();
        } catch (Exception e){
            e.printStackTrace();
        }
        System.out.println(((System.currentTimeMillis()-start)/1000.) + " s");
//        System.out.println("Number of DLMF-Macros: " + internal_dlmf_counter);
//        System.out.println("Number of Maple translations: " + internal_maple_trans_counter);
    }
}
