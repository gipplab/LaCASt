package gov.nist.drmf.interpreter.maple.grammar.lexicon;

import gov.nist.drmf.interpreter.common.constants.GlobalConstants;
import gov.nist.drmf.interpreter.common.constants.GlobalPaths;
import gov.nist.drmf.interpreter.common.constants.Keys;
import gov.nist.drmf.interpreter.pom.common.grammar.DLMFFeatureValues;
import gov.nist.drmf.interpreter.maple.common.MapleConstants;
import gov.nist.drmf.interpreter.pom.MacrosLexicon;
import mlp.FeatureSet;
import mlp.Lexicon;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
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

    private static int counter = 0;

    MapleLexiconFactory ( String[] keyWords ){
        indices = new HashMap<>();
        counter = 0;

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
        // empty line
        if ( values == null ) return null;

        // extract maple function and DLMF pattern
        String MAPLE, DLMF;
        try {
            MAPLE = values[indices.get(MapleHeader.Function)];
            DLMF = values[indices.get(MapleHeader.DLMF_Pattern)];
        } catch ( Exception e ){
            LOG.debug("Cannot extract Maple or DLMF pattern. " + Arrays.toString(values));
            return null;
        }

        if ( DLMF == null || DLMF.isEmpty() ) return null;
        counter++;

        // Try to generate MapleFunction object first
        String maple_func = getMapleFunctionName( MAPLE );
        if ( maple_func == null ) return null;

        String maple_link = extractInfoMaple( MapleHeader.Link, values );
        String numOfVars  = extractInfoMaple( MapleHeader.Num_Of_Vars, values );
        Integer vars;
        try { vars = Integer.parseInt(numOfVars); }
        catch( NumberFormatException nfe ){
            LOG.debug( "Not able to parse number of variables: " + numOfVars + " for " + MAPLE );
            return null;
        }

        // the pattern is a complex expression.
        // if the pattern starts with X<num>:<macro>X...
        // it means, the lexicon should reference to <macro> with <num> optional parameters.
        Matcher m = GlobalConstants.GENERAL_MACRO_TRANSLATION_PATTERN.matcher(DLMF);
        if ( !m.matches() ){
            LOG.debug("Skip, illegal translation pattern: " + DLMF);
            return null;
        }

        FeatureSet fset = null;
        String alternate_pref = m.group(1);
        String dlmf_pattern = null;
        List<FeatureSet> feature_list;
        if ( alternate_pref != null ){ // starts with X<num>:<macro>X...
            dlmf_pattern = DLMF.substring( alternate_pref.length() );
            alternate_pref = alternate_pref.substring( 1, alternate_pref.length()-1 );
            String[] parts = alternate_pref.split( GlobalConstants.MACRO_OPT_PARAS_SPLITTER );
            feature_list = dlmf_lex.getFeatureSets( parts[1] );
            if (feature_list != null) {
                for ( FeatureSet f : feature_list ){
                    if ( parts[0].matches("0") && f.getFeatureSetName().matches( Keys.KEY_DLMF_MACRO ) ){
                        fset = f;
                        break;
                    } else if ( f.getFeatureSetName().matches( Keys.KEY_DLMF_MACRO_OPTIONAL_PREFIX+parts[0] ) ){
                        fset = f;
                        break;
                    }
                }
            }
        } else { // no extra link
            dlmf_pattern = DLMF;
            String func_link = m.group(2);
            if ( func_link == null ){
                LOG.debug("Cannot recognize the DLMF macro and store plain info: " + DLMF);
                return new MapleFunction(
                        maple_func,
                        dlmf_pattern,
                        maple_link,
                        vars
                );
            }

            feature_list = dlmf_lex.getFeatureSets( func_link );
            if ( feature_list != null) {
                for ( FeatureSet f : feature_list ){
                    if ( f.getFeatureSetName().matches( Keys.KEY_DLMF_MACRO ) ){
                        fset = f;
                        break;
                    }
                }
            }
        }

        // generate lexicon object
        MapleFunction mf = new MapleFunction(
                maple_func,
                dlmf_pattern,
                maple_link,
                vars
        );

        try { mf = enrichFunctionWithMapleInfos( mf, values ); }
        catch ( Exception npe ){
            LOG.debug("Fail while enriching basic info for: " + maple_func +
                    ". Reason: " + npe.getMessage());
        }

        // if fset is still null, return null
        if ( fset == null ){
            LOG.warn("Wasn't able to find FeatureSet of: " + DLMF);
            return mf;
        }

        try { return enrichFunctionInfosFromLexicon( mf, fset ); }
        catch ( Exception npe ){
            LOG.debug("Not able to enrich " + maple_func +
                    " with additional information! Reason: " + npe.getMessage());
            return mf;
        }
    }

    private String getMapleFunctionName( String raw ){
        Pattern pat = MapleConstants.MAPLE_FUNC_PATTERN;
        Matcher dlmfMatcher = pat.matcher(raw);
        if ( !dlmfMatcher.matches() ) {
            LOG.warn("Not able to extract Maple function from: " + raw);
            return null;
        }
        return dlmfMatcher.group(1);
    }

    private String extractInfoMaple( MapleHeader h, String[] values ){
        Integer i = indices.get( h );
        try { return values[i]; }
        catch ( NullPointerException | IndexOutOfBoundsException ie ){
            return "";
        }
    }

    private MapleFunction enrichFunctionWithMapleInfos( MapleFunction mf, String[] values ){
        // Maple comment, branch cuts and domains
        mf.setMapleComment( extractInfoMaple( MapleHeader.Comment, values ) );
        mf.setMapleBranchCuts( extractInfoMaple( MapleHeader.Branch_Cuts, values ) );
        mf.setMapleConstraints( extractInfoMaple( MapleHeader.Constraints, values ) );

        String alternatives = extractInfoMaple( MapleHeader.Alternatives, values );
        mf.setAlternativePatterns(
                alternatives.split( MacrosLexicon.SIGNAL_INLINE )
        );

        return mf;
    }

    private MapleFunction enrichFunctionInfosFromLexicon( MapleFunction mf, FeatureSet fset ){
        mf.setDlmfBranchCuts(DLMFFeatureValues.BRANCH_CUTS.getFeatureValue(fset, null));
        mf.setDlmfConstraints(DLMFFeatureValues.CONSTRAINTS.getFeatureValue(fset, null));
        mf.setDlmfLink(DLMFFeatureValues.DLMF_LINK.getFeatureValue(fset, null));
        mf.setDlmfMeaning(DLMFFeatureValues.MEANING.getFeatureValue(fset, null));
        return mf;
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
            System.out.println("#Trans" + lex.numberOfFunctions());
            System.out.println("Out of: " + counter);
        } catch ( IOException ioe ){
            ioe.printStackTrace();
        }
    }
}
