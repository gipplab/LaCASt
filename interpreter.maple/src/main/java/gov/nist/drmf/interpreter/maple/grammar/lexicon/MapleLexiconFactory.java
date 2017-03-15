package gov.nist.drmf.interpreter.maple.grammar.lexicon;

import gov.nist.drmf.interpreter.common.GlobalConstants;
import gov.nist.drmf.interpreter.common.grammar.DLMFFeatureValues;
import gov.nist.drmf.interpreter.maple.common.MapleConstants;
import gov.nist.drmf.interpreter.mlp.extensions.MacrosLexicon;
import mlp.FeatureSet;
import mlp.Lexicon;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
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
        for ( int i = 0; i < keyWords.length; i++ ){
            MapleHeader h = MapleHeader.getHeader(keyWords[i]);
            if ( h != null ) indices.put( h, i );
        }
        dlmf_lex = MacrosLexicon.getDLMFMacroLexicon();
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
            String MAPLE = values[indices.get(MapleHeader.Function)];
            String DLMF = values[indices.get(MapleHeader.DLMF_Pattern)];

            errorMessage = "Cannot extract function name. " + Arrays.toString(values);
            // check style of function
            String maple_func = getFunctionName( MapleConstants.MAPLE_FUNC_PATTERN, MAPLE );
            String dlmf = getFunctionName( GlobalConstants.DLMF_MACRO_PATTERN, DLMF );

            String maple_link = extractInfoMaple( MapleHeader.Link, values );
            String numOfVars  = extractInfoMaple( MapleHeader.Num_Of_Vars, values );
            Integer vars = Integer.parseInt(numOfVars);

            MapleFunction mf = new MapleFunction(
                    maple_func,
                    DLMF,
                    maple_link,
                    vars
            );

            return enrichFunctionInfos( mf, dlmf, values );
        } catch ( NullPointerException | IndexOutOfBoundsException | NumberFormatException e ){
            LOG.warn(errorMessage);
            return null;
        }
    }

    private String getFunctionName(Pattern pat, String raw ){
        Matcher dlmfMatcher = pat.matcher(raw);
        if ( !dlmfMatcher.matches() ) {
            LOG.warn("Not able to extract function from: " + raw);
            return null;
        }
        return dlmfMatcher.group(1);
    }

    private MapleFunction enrichFunctionInfos( MapleFunction mf, String dlmf_func, String[] values ){
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
        FeatureSet fset = fsets.get(0);

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
                    //.limit(20) // TODO DEBUG
                    .parallel()
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
}
