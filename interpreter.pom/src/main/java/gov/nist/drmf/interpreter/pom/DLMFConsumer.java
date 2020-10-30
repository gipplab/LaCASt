package gov.nist.drmf.interpreter.pom;

import gov.nist.drmf.interpreter.common.constants.GlobalConstants;
import gov.nist.drmf.interpreter.common.constants.Keys;
import gov.nist.drmf.interpreter.pom.data.DLMFMacroFileHeaders;
import gov.nist.drmf.interpreter.pom.data.LexiconInfoConsumer;
import gov.nist.drmf.interpreter.pom.data.Stats;
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
        String macro = DLMFMacroFileHeaders.DLMF.getValue("", lineAnalyzer);
        Matcher m = GlobalConstants.DLMF_MACRO_PATTERN.matcher( macro );
        if ( !m.matches() ){
            LOG.info("Found a not supported DLMF macro: " + macro);
            return;
        }

        String optional_ats = m.group( GlobalConstants.MACRO_PATTERN_INDEX_OPT_PARAS );
        if ( optional_ats != null ){
            LOG.debug("Found optional parameter. " + optional_ats);
            handleOptionalParametersByDLMF(m);
            return;
        }

        String macro_name = m.group(GlobalConstants.MACRO_PATTERN_INDEX_MACRO);
        FeatureSet fset = chooseFeatureSet(m);
        if ( fset == null ) return;

        handleGeneralFeature(fset, macro, macro_name);
    }

    private void handleOptionalParametersByDLMF( Matcher m ){
        String mac = m.group( GlobalConstants.MACRO_PATTERN_INDEX_OPT_PARAS );
        mac = mac.substring(1, mac.length()-1);
        String[] info = mac.split( GlobalConstants.MACRO_OPT_PARAS_SPLITTER );

        int opt_para = Integer.parseInt(info[0]);
        List<FeatureSet> sets = lexicon.getFeatureSets( info[1] );
        if ( sets == null )
            sets = new LinkedList<>();

        try {
            FeatureSet fset = new FeatureSet( Keys.KEY_DLMF_MACRO_OPTIONAL_PREFIX + opt_para );
            DLMFMacroFileHeaders.fillFeatureSet(fset, lineAnalyzer);
            // overwrite macro feature
            fset.setFeature(
                    Keys.KEY_DLMF,
                    m.group(0).substring(mac.length()+2),
                    MacrosLexicon.SIGNAL_INLINE
            );

            sets.add(fset);
            lexicon.setEntry( info[1], sets );
        } catch (NullPointerException npe) {
            LOG.error("Unable to load information for macro with optional parameters "
                    + mac + "("+npe.getMessage()+")");
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
                // the annotators were lazy, if there is no role defined, it is a macro by default
                fset = new FeatureSet( Keys.KEY_DLMF_MACRO );
        }

        return fset;
    }

    private void handleConstantFeature(String macro, String macro_name) {
        FeatureSet fset = new FeatureSet(Keys.KEY_DLMF_MACRO);
        handleGeneralFeature(fset, macro, macro_name);
    }

    private void handleGeneralFeature(FeatureSet fset, String macro, String macro_name) {
        try {
            // add all other information to the feature set
            DLMFMacroFileHeaders.fillFeatureSet(fset, lineAnalyzer);
            // since each DLMF macro has only one feature set, create a list with one element
            addFeatureSet(new LinkedList<>(), fset, macro_name);
        } catch (NullPointerException npe) {
            LOG.error("Unable to load information for constant " + macro + "("+npe.getMessage()+")");
        }
    }

    private void addFeatureSet(List<FeatureSet> fsets, FeatureSet fset, String macro_name) {
        fsets.add(fset);

        // group(1) is the DLMF macro without the suffix of parameters, ats and variables
        // just the plain macro
        lexicon.setEntry( macro_name, fsets );
        stats.tickDLMF();
    }
}
