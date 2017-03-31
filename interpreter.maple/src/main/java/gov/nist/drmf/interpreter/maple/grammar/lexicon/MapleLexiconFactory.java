package gov.nist.drmf.interpreter.maple.grammar.lexicon;

import gov.nist.drmf.interpreter.common.GlobalConstants;
import gov.nist.drmf.interpreter.common.GlobalPaths;
import gov.nist.drmf.interpreter.common.Keys;
import gov.nist.drmf.interpreter.common.grammar.DLMFFeatureValues;
import gov.nist.drmf.interpreter.maple.common.MapleConstants;
import gov.nist.drmf.interpreter.mlp.extensions.MacrosLexicon;
import mlp.FeatureSet;
import mlp.Lexicon;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * Created by AndreG-P on 13.03.2017.
 */
public class MapleLexiconFactory {
    public static final Logger LOG = LogManager.getLogger( MapleLexiconFactory.class );

    public static final String DELIMITER = ";";

    private HashMap<MapleHeader, Integer> indices;
    private Lexicon dlmf_lex;

    MapleLexiconFactory ( String[] keyWords ){
        indices = new HashMap<>();

        //LOG.info(Arrays.toString(keyWords));
        for ( int i = 0; i < keyWords.length; i++ ){
            MapleHeader h = MapleHeader.getHeader(keyWords[i]);
            //LOG.info( keyWords[i] + ", " + h );
            if ( h != null ) indices.put( h, i );
        }

        String s = "";
        for ( MapleHeader h : indices.keySet() )
            s += h.toString() + ": " + indices.get(h) + ", ";
        //LOG.info(s);

        dlmf_lex = MacrosLexicon.getDLMFMacroLexicon();
        if ( dlmf_lex == null ){
            LOG.error("Macro Lexicon is not yet initialized!");
        }
    }

    /**
     * The constructor get an array of key words. The given
     * values array must be in the same order as these pre-
     * defined key words.
     * @see #MapleLexiconFactory(String[])
     * @param values in the order of key words (constructor)
     * @return Maple function object
     */
    MapleFunction createMapleFunction( String[] values ){
        if ( values == null ) return null;
        String errorMessage = "";
        try {
            errorMessage = "Wasn't able to extract information from array. " + Arrays.toString(values);
            // get maple expression
            //LOG.info(indices.get(MapleHeader.Function));
            //LOG.info(indices.get(MapleHeader.DLMF_Pattern));
            String MAPLE = values[indices.get(MapleHeader.Function)];
            String DLMF = values[indices.get(MapleHeader.DLMF_Pattern)];

            //LOG.info(DLMF);
            int opt = 0;
            Matcher m = GlobalConstants.DLMF_MACRO_PATTERN.matcher(DLMF);
            if ( m.matches() ){
                String opt_para = m.group( GlobalConstants.MACRO_PATTERN_INDEX_OPT_PARA );
                if ( opt_para != null ) {
                    DLMF = DLMF.substring(opt_para.length());
                    opt = Integer.parseInt(opt_para.replace("X",""));
                }
            } else {
                LOG.warn("Not able to handle");
                return null;
            }

            errorMessage = "Cannot extract function name. " + Arrays.toString(values);
            // check style of function
            String maple_func = getMapleFunctionName( MAPLE );
            String dlmf = getDLMFFunctionName( DLMF );

            errorMessage = "Cannot extract link or number of variables. " + Arrays.toString(values);
            String maple_link = extractInfoMaple( MapleHeader.Link, values );
            String numOfVars  = extractInfoMaple( MapleHeader.Num_Of_Vars, values );
            Integer vars = Integer.parseInt(numOfVars);

            errorMessage = "Cannot create maple function object. " + Arrays.toString(values);
            MapleFunction mf = new MapleFunction(
                    maple_func,
                    DLMF,
                    maple_link,
                    vars
            );

            try {
                enrichFunctionInfos( mf, dlmf, opt, values );
            } catch ( NullPointerException npe ){
                errorMessage = "Cannot enrich the information of MapleFunction object. "
                        + Arrays.toString(values);;
            }

            return mf;
        } catch ( NullPointerException | IndexOutOfBoundsException | NumberFormatException e ){
            LOG.warn(errorMessage);
            return null;
        }
    }

    private String getMapleFunctionName( String raw ){
        Pattern pat = MapleConstants.MAPLE_FUNC_PATTERN;
        Matcher dlmfMatcher = pat.matcher(raw);
        if ( !dlmfMatcher.matches() ) {
            LOG.warn("Not able to extract function from: " + raw);
            return null;
        }
        return dlmfMatcher.group(1);
    }

    private String getDLMFFunctionName( String raw ){
        Pattern pat = GlobalConstants.DLMF_MACRO_PATTERN;
        Matcher dlmfMatcher = pat.matcher(raw);
        if ( !dlmfMatcher.matches() ) {
            LOG.warn("Not able to extract function from: " + raw);
            return null;
        }
        return dlmfMatcher.group( GlobalConstants.MACRO_PATTERN_INDEX_MACRO );
    }

    private MapleFunction enrichFunctionInfos( MapleFunction mf, String dlmf_func, int opt, String[] values ){
        // Maple comment, branch cuts and domains
        mf.setMapleComment( extractInfoMaple( MapleHeader.Comment, values ) );
        mf.setMapleBranchCuts( extractInfoMaple( MapleHeader.Branch_Cuts, values ) );
        mf.setMapleConstraints( extractInfoMaple( MapleHeader.Constraints, values ) );

        String alternatives = extractInfoMaple( MapleHeader.Alternatives, values );
        mf.setAlternativePatterns(
                alternatives.split( GlobalConstants.ALTERNATIVE_SPLIT )
        );

        List<FeatureSet> fsets = dlmf_lex.getFeatureSets( dlmf_func );
        if ( fsets == null || fsets.isEmpty() ) return mf;
        FeatureSet fset = null;

        for ( FeatureSet f : fsets ){
            if ((f.getFeatureSetName().matches(Keys.KEY_DLMF_MACRO)&&opt == 0) ||
                    f.getFeatureSetName().matches(Keys.KEY_DLMF_MACRO_OPTIONAL_PREFIX+opt)
                    ){
                fset = f;
                break;
            }
        }

        if ( fset == null ) return mf;

        mf.setDlmfBranchCuts(DLMFFeatureValues.branch_cuts.getFeatureValue(fset));
        mf.setDlmfConstraints(DLMFFeatureValues.constraints.getFeatureValue(fset));
        mf.setDlmfLink(DLMFFeatureValues.dlmf_link.getFeatureValue(fset));
        mf.setDlmfMeaning(DLMFFeatureValues.meaning.getFeatureValue(fset));

        return mf;
    }

    private String extractInfoMaple( MapleHeader h, String[] values ){
        Integer i = indices.get( h );
        try { return values[i]; }
        catch ( NullPointerException | IndexOutOfBoundsException ie ){
            return "";
        }
    }

    public static MapleLexicon createLexiconFromCSVFile( Path maple_csv_file )
            throws IOException {
        try (BufferedReader reader = Files.newBufferedReader( maple_csv_file )){
            String header = reader.readLine();
            MapleLexiconFactory mlf = new MapleLexiconFactory(header.split( DELIMITER ));
            MapleLexicon mapleLexicon = new MapleLexicon();

            reader.lines()
                    //.limit(5) // TODO DEBUG
                    //.parallel()
                    .filter( line -> !line.startsWith(DELIMITER) )
                    .map( line -> line.split(DELIMITER) )
                    .map( mlf::createMapleFunction )
                    .filter( Objects::nonNull )
                    .forEach( mapleLexicon::addFunction );

            LOG.info("Loaded maple's lexicon file.");
            return mapleLexicon;
        } catch ( FileNotFoundException fnfe ){
            LOG.fatal("Cannot find maple CSV file.", fnfe);
        }
        return null;
    }

    public static void storeLexicon( Path lexicon_file, MapleLexicon lexicon ){
        Map<String, MapleFunction> map = lexicon.getFunctionMap();
        try ( BufferedWriter writer = Files.newBufferedWriter(lexicon_file) ){
            map.keySet().stream()
                    .sorted( String::compareTo )
                    .map( map::get )
                    .map( MapleFunction::toStorage )
                    .forEach( s -> {
                        try { writer.write(s); }
                        catch ( IOException ioe ){
                            LOG.error("Cannot write: " + s);
                        }
                    } );
        } catch ( IOException ioe ){
            ioe.printStackTrace();
        }
    }

    public static MapleLexicon loadLexicon( Path maple_lexicon ){
        MapleLexicon lexicon = new MapleLexicon();
        try ( BufferedReader reader = Files.newBufferedReader(maple_lexicon) ){
            reader.lines()
                    .filter( line -> !(line == null || line.isEmpty()) )
                    .map( t -> innerLoader(reader) )
                    .filter( Objects::nonNull )
                    .forEach( lexicon::addFunction );
            return lexicon;
        } catch ( IOException ioe ){
            ioe.printStackTrace();
            return null;
        }
    }

    private static final String NL = System.lineSeparator();

    private static MapleFunction innerLoader( BufferedReader reader ){
        String in = "-" + NL;
        try {
            for ( int i = 0; i < 12; i++ ){
                in += reader.readLine() + NL;
            }
            return MapleFunction.loadMapleFunction( in );
        } catch ( IOException ioe ){
            ioe.printStackTrace();
            return null;
        }
    }

    public static void main( String[] args ){
        String csv_file;
        if ( args == null || args.length < 1 ){
            System.out.println("Please specify the name of the Maple-CSV file in CSVTables directory:");
            Scanner sc = new Scanner(System.in);
            String in = sc.nextLine();
            if ( !in.toLowerCase().matches(".+\\.csv") ){
                System.err.println("Your specification is not a csv file...");
                return;
            }
            csv_file = in;
        } else csv_file = args[0];

        Path p = GlobalPaths.PATH_REFERENCE_DATA_CSV.resolve( csv_file );
        try {
            MacrosLexicon.init();
            MapleLexicon lex = createLexiconFromCSVFile( p );
            storeLexicon( GlobalPaths.PATH_MAPLE_FUNCTIONS_LEXICON_FILE, lex );
            System.out.println("Done!");
        } catch ( IOException ioe ){
            ioe.printStackTrace();
        }
    }
}
