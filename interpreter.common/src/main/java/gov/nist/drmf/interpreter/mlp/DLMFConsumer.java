package gov.nist.drmf.interpreter.mlp;

import gov.nist.drmf.interpreter.common.constants.GlobalConstants;
import gov.nist.drmf.interpreter.common.constants.Keys;
import gov.nist.drmf.interpreter.mlp.data.LexiconInfoConsumer;
import gov.nist.drmf.interpreter.mlp.data.Stats;
import mlp.FeatureSet;
import mlp.Lexicon;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;

/**
 * @author Andre Greiner-Petter
 */
public class DLMFConsumer implements LexiconInfoConsumer {
    private static final Logger LOG = LogManager.getLogger(DLMFConsumer.class.getName());

    private final Lexicon lexicon;

    private LineAnalyzer lineAnalyzer;

    private String[] header;
    private Stats stats;

    public DLMFConsumer(Stats stats, Lexicon lexicon) {
        this.stats = stats;
        this.lexicon = lexicon;
    }

    public void parse(Path csvDLMFPath){
        LOG.info("Start reading " + csvDLMFPath);
        try (BufferedReader br = Files.newBufferedReader(csvDLMFPath) ){
            LexiconConverterUtility.parseCSV( null, br, this );
            LOG.info("Finished build basic information for DLMF macros.");
        } catch ( IOException ioe ){
            LOG.error( "Error occured in reading process of " + csvDLMFPath, ioe );
            ioe.printStackTrace();
        }
    }

    @Override
    public void setLineAnalyzer(LineAnalyzer lineAnalyzer) {
        this.lineAnalyzer = lineAnalyzer;
        this.header = lineAnalyzer.getHeader();
    }

    @Override
    public void accept(String[] elements) {
        lineAnalyzer.setLine(elements);
        //System.out.println(Arrays.toString(elements));

        // check if the input is a correct DLMF macro
        String macro = lineAnalyzer.getValue( Keys.KEY_DLMF );
        Matcher m = GlobalConstants.DLMF_MACRO_PATTERN.matcher( macro );
        if ( !m.matches() ){
            LOG.info("Found a not supported DLMF macro: " + macro);
            return;
        }

        String optional_ats = m.group( GlobalConstants.MACRO_PATTERN_INDEX_OPT_PARAS );
        if ( optional_ats != null ){
            LOG.debug("Found optional parameter. " + optional_ats);
            handleOptionalParametersByDLMF(m, elements);
            return;
        }

        String macro_name = m.group(GlobalConstants.MACRO_PATTERN_INDEX_MACRO);
        FeatureSet fset = chooseFeatureSet(m);
        if ( fset == null ) return;

        // add the general representation for this macro
        fset.addFeature( Keys.KEY_DLMF, macro, MacrosLexicon.SIGNAL_INLINE );

        // add all other information to the feature set
        fillFeature(elements, fset);

        // since each DLMF macro has only one feature set, create a list with one element
        List<FeatureSet> fsets = new LinkedList<>();
        fsets.add(fset);

        // group(1) is the DLMF macro without the suffix of parameters, ats and variables
        // just the plain macro
        lexicon.setEntry( macro_name, fsets );
        stats.tickDLMF();
    }

    private void handleOptionalParametersByDLMF( Matcher m, String[] elements ){
        String mac = m.group( GlobalConstants.MACRO_PATTERN_INDEX_OPT_PARAS );
        mac = mac.substring(1, mac.length()-1);
        String[] info = mac.split( GlobalConstants.MACRO_OPT_PARAS_SPLITTER );

        Integer opt_para = Integer.parseInt(info[0]);

        List<FeatureSet> sets = lexicon.getFeatureSets( info[1] );
        if ( sets == null )
            sets = new LinkedList<>();

        FeatureSet fset = new FeatureSet( Keys.KEY_DLMF_MACRO_OPTIONAL_PREFIX + opt_para );
        fset.addFeature(
                Keys.KEY_DLMF,
                m.group(0).substring(mac.length()+2),
                MacrosLexicon.SIGNAL_INLINE
        );

        fillFeature(elements, fset);

        sets.add(fset);
        lexicon.setEntry( info[1], sets );
    }

    private void fillFeature(String[] elements, FeatureSet fset) {
        // add all other information to the feature set
        for ( int i = 1; i < elements.length && i < header.length; i++ ){
            String value = lineAnalyzer.getValue( header[i] );
            if ( value != null && !value.isEmpty() )
                fset.addFeature( header[i], value, MacrosLexicon.SIGNAL_INLINE );
        }
    }

    private FeatureSet chooseFeatureSet(Matcher m) {
        String macro = lineAnalyzer.getValue( Keys.KEY_DLMF );
        String macro_name = m.group(GlobalConstants.MACRO_PATTERN_INDEX_MACRO);
        // find out if it is a mathematical constant
        String role = lineAnalyzer.getValue( Keys.FEATURE_ROLE );
        // otherwise it is a usual DLMF macro and we can create our feature set for it
        // create a new feature set
        FeatureSet fset;
        switch (role) {
            case Keys.FEATURE_VALUE_CONSTANT:
                handleConstantFeature(macro, macro_name);
            case Keys.FEATURE_VALUE_IGNORE:
                return null;
            case Keys.FEATURE_VALUE_FUNCTION:
                fset = new FeatureSet( Keys.FEATURE_VALUE_FUNCTION );
                break;
            case Keys.FEATURE_VALUE_SYMBOL:
                // TODO we may handle symbols in a different way
            default:
                fset = new FeatureSet( Keys.KEY_DLMF_MACRO );
        }

        return fset;
    }

    private void handleConstantFeature(String macro, String macro_name) {
        FeatureSet fset = new FeatureSet(Keys.KEY_DLMF_MACRO);
        // add the general representation for this macro
        fset.addFeature( Keys.KEY_DLMF, macro, MacrosLexicon.SIGNAL_INLINE );

        String dlmf_link = Keys.KEY_DLMF + Keys.KEY_LINK_SUFFIX;
        fset.addFeature( dlmf_link, lineAnalyzer.getValue(dlmf_link), MacrosLexicon.SIGNAL_INLINE );
        fset.addFeature( Keys.FEATURE_MEANINGS, lineAnalyzer.getValue(Keys.FEATURE_MEANINGS), MacrosLexicon.SIGNAL_INLINE );
        fset.addFeature( Keys.FEATURE_ROLE, Keys.FEATURE_VALUE_CONSTANT, MacrosLexicon.SIGNAL_INLINE );
        List<FeatureSet> fsets = new LinkedList<>();
        fsets.add(fset);
        lexicon.setEntry(
                macro_name,
                fsets
        );
        stats.tickDLMF();
    }
}
