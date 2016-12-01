package gov.nist.drmf.interpreter.cas.mlp;

import gov.nist.drmf.interpreter.common.GlobalConstants;
import gov.nist.drmf.interpreter.common.Keys;
import jdk.nashorn.internal.objects.Global;
import mlp.FeatureSet;
import mlp.Lexicon;
import mlp.LexiconFactory;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Andre Greiner-Petter
 */
public class CSVtoLexiconConverter {
    protected final Logger ERROR_LOG = Logger.getLogger( CSVtoLexiconConverter.class.toString() );

    public static final String DELIMITER = ";";

    private static final String SIGNAL_ENTRY = "Symbol: ";

    private static final String SIGNAL_FEATURESET = "Feature Set:";

    private static final String SIGNAL_LINE = "-";

    private static final String SIGNAL_INLINE = "\\|\\|";

    private static final Pattern PATTERN_MACRO =
            Pattern.compile("\\s*(\\\\\\w+)(\\[.*\\])*(\\{.*\\})*(@+\\{+.+\\}+)*\\s*");

    private Path path_to_dlmf_lexicon;

    private Lexicon dlmf_lexicon;

    private Path csv_dlmf_file;

    private Path[] csv_cas_files;

    private LineAnalyzer lineAnalyzer;

    private String[] header;

    public CSVtoLexiconConverter ( Path CSV_dlmf_file, Path... CSV_CAS_files ) throws Exception {
        this.csv_dlmf_file =
                GlobalConstants.PATH_REFERENCE_DATA_CSV.resolve(CSV_dlmf_file);
        if ( !csv_dlmf_file.toFile().exists() )
            throw new FileNotFoundException(
                    "The given link to the CSV-DLMF file doesn't exists! " + csv_dlmf_file.toString()
            );

        this.csv_cas_files = CSV_CAS_files;
        for ( int i = 0; i < csv_cas_files.length; i++ ) {
            csv_cas_files[i] = GlobalConstants.PATH_REFERENCE_DATA_CSV.resolve(csv_cas_files[i]);
            if (!csv_cas_files[i].toFile().exists())
                throw new FileNotFoundException(
                        "The given CSV-CAS file doesn't exists. " + csv_cas_files[i].toString()
                );
        }

        path_to_dlmf_lexicon =
                GlobalConstants.PATH_LEXICONS.resolve( GlobalConstants.DLMF_MACROS_LEXICON_NAME );
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
                    SIGNAL_ENTRY,
                    SIGNAL_FEATURESET,
                    SIGNAL_LINE,
                    SIGNAL_INLINE);
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
                SIGNAL_ENTRY,
                SIGNAL_FEATURESET,
                path_to_dlmf_lexicon.toString()
        );
    }

    private void generateBasicDLMF(){
        try (BufferedReader br = Files.newBufferedReader(csv_dlmf_file) ){
            startReadingProcess( br, this::handleDLMFElements );
        } catch ( IOException ioe ){
            ioe.printStackTrace();
        }
    }

    private void addCAS( Path csv ){
        try ( BufferedReader br = Files.newBufferedReader( csv ) ){
            startReadingProcess( br, this::handleCasAddOn );
        } catch ( IOException ioe ){
            ioe.printStackTrace();
        }
    }

    private void startReadingProcess( BufferedReader br, Consumer<? super String[]> method )
            throws IOException {
        String headerLine = br.readLine();

        if ( headerLine != null && !headerLine.isEmpty() ){
            header = headerLine.split( DELIMITER );
            lineAnalyzer = new LineAnalyzer( DELIMITER, header );
        } else {
            throw new IOException("The header is empty! " + csv_dlmf_file.toString());
        }

        br.lines()//.limit(320) // TODO limit for debug
                .filter( line -> !line.startsWith( DELIMITER ) )
                .map( line -> line += line.endsWith(DELIMITER) ? " " : "" )
                .map( line -> line.split(DELIMITER) )
                .forEach( method );
    }

    private void handleDLMFElements( String[] elements ){
        lineAnalyzer.setLine(elements);

        // check if the input is a correct DLMF macro
        // the DLMF macro always has to be on the first position
        String macro = lineAnalyzer.getValue( header[0] );
        Matcher m = PATTERN_MACRO.matcher( macro );
        if ( !m.matches() ){
            ERROR_LOG.info("Found a not supported DLMF macro: " + macro);
            return;
        }

        // find out if it is a mathematical constant
        String role = lineAnalyzer.getValue( Keys.FEATURE_ROLE );
        if ( role.matches( Keys.FEATURE_VALUE_CONSTANT ) ){
            // TODO handle constant
            ERROR_LOG.info("Found a constant: " + macro);
            return;
        }

        // otherwise it is a usual DLMF macro and we can create our feature set for it
        // create a new feature set
        FeatureSet fset = new FeatureSet( Keys.KEY_DLMF_MACRO );
        // add the general representation for this macro
        fset.addFeature( Keys.KEY_DLMF, macro, SIGNAL_INLINE );

        // add all other information to the feature set
        for ( int i = 1; i < elements.length && i < header.length; i++ ){
            String value = lineAnalyzer.getValue( header[i] );
            if ( value != null && !value.isEmpty() )
                fset.addFeature( header[i], value, SIGNAL_INLINE );
        }

        // since each DLMF macro has only one feature set, create a list with one element
        LinkedList<FeatureSet> fsets = new LinkedList<>();
        fsets.add(fset);

        // group(1) is the DLMF macro without the suffix of parameters, ats and variables
        // just the plain macro
        dlmf_lexicon.setEntry( m.group(1), fsets );
    }

    private void handleCasAddOn( String[] elements ){
        lineAnalyzer.setLine( elements );

        String macro = lineAnalyzer.getValue( header[0] );
        Matcher m = PATTERN_MACRO.matcher( macro );
        if ( !m.matches() ){
            ERROR_LOG.info("Found a not supported DLMF macro for translation: " + macro);
            return;
        }

        List<FeatureSet> list = dlmf_lexicon.getFeatureSets( m.group(1) );
        if ( list == null || list.isEmpty() ){
            ERROR_LOG.severe("Cannot find FeatureSet of " + m.group(1) );
            return;
        }

        FeatureSet fset = list.get(0);
        for ( int i = 1; i < elements.length && i < header.length; i++ ){
            String value = lineAnalyzer.getValue( header[i] );
            if ( value != null && !value.isEmpty() )
                fset.addFeature( header[i], value, SIGNAL_INLINE );
        }

        // hmm, i don't know if this is necessary because we are working on references
        //dlmf_lexicon.setEntry( m.group(1), fsets );
    }

    public static void main(String[] args){
        long start = System.currentTimeMillis();
        try{
            CSVtoLexiconConverter csvConv = new CSVtoLexiconConverter(
                Paths.get("DLMFMacro.csv"),
                Paths.get("CAS_Maple.csv")
            );
            csvConv.generateLexiconFile();
        } catch (Exception e){
            e.printStackTrace();
        }
        System.out.println(((System.currentTimeMillis()-start)/1000.) + " s");
    }
}
