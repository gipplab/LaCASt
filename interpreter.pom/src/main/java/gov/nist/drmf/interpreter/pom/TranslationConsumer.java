package gov.nist.drmf.interpreter.pom;

import gov.nist.drmf.interpreter.common.constants.GlobalConstants;
import gov.nist.drmf.interpreter.common.constants.Keys;
import gov.nist.drmf.interpreter.pom.data.*;
import mlp.FeatureSet;
import mlp.Lexicon;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.regex.Matcher;

/**
 * @author Andre Greiner-Petter
 */
public class TranslationConsumer implements LexiconInfoConsumer {
    private static final Logger LOG = LogManager.getLogger(CASConsumer.class.getName());

    private final String cas;

    private final CASCache cache;
    private final Lexicon lexicon;

    private final Stats stats;

    private LineAnalyzer lineAnalyzer;

    public TranslationConsumer(String cas, Lexicon lexicon, CASCache cache, Stats stats) {
        this.cas = cas;
        this.lexicon = lexicon;
        this.cache = cache;
        this.stats = stats;
    }

    @Override
    public void setLineAnalyzer(LineAnalyzer lineAnalyzer) {
        this.lineAnalyzer = lineAnalyzer;
    }

    @Override
    public void accept(String[] elements) {
        lineAnalyzer.setLine( elements );

        // get feature set of the DLMF macro, it should already exist
        FeatureSet fset = getFeatureSet();
        // if not, we cant do anything here
        if ( fset == null ) return;

        if ( lineAnalyzer.getValue(cas).isBlank() && lineAnalyzer.getValue(cas+Keys.KEY_ALTERNATIVE_SUFFX).isBlank() ) {
            LOG.info("No translation available for " + cas + ": function -> " + fset.getFeature(Keys.KEY_DLMF).first());
            return;
        }

        try {
            String originalDLMF = fset.getFeature(Keys.KEY_DLMF).first();
            ForwardTranslationFileHeaders.DLMF.fillFeatureSet(fset, lineAnalyzer, cas);
            fset.setFeature(Keys.KEY_DLMF, originalDLMF, MacrosLexicon.SIGNAL_INLINE);
            addTranslation(fset);
        } catch (NullPointerException npe) {
            LOG.warn("Unable to read necessary translation information for " + fset.getFeature(Keys.KEY_DLMF) + " ("+npe.getMessage()+")", npe);
        }
    }

    private void addTranslation(FeatureSet fset) {
        // get the name and pattern of the translation
        boolean isStraightAvailable = true;
        SortedSet<String> f = fset.getFeature(cas);
        if ( f == null || f.isEmpty() ) {
            isStraightAvailable = false;
            f = fset.getFeature(cas+Keys.KEY_ALTERNATIVE_SUFFX);
        }
        String function = f.first();

        FunctionInfoHolder holder = LexiconConverterUtility.getFuncNameAndFillInteger(
                function,
                "Not able to link further information about " + function,
                lineAnalyzer
        );

        if ( holder != null ) function = holder.getPattern();

        // add translation info
        if ( isStraightAvailable ) fset.setFeature( cas, function, MacrosLexicon.SIGNAL_INLINE );
        addFurtherInfoToFeature(holder, fset, cas);
        stats.tickCAS(cas);
    }

    private FeatureSet getFeatureSet() {
        String macro_col = lineAnalyzer.getValue( Keys.KEY_DLMF );
        Integer num = null;
        Matcher m = GlobalConstants.DLMF_MACRO_PATTERN.matcher( macro_col );
        if ( !m.matches() ){
            LOG.info("Found a not supported DLMF macro for translation: " + macro_col);
            return null;
        }

        String macro = m.group(GlobalConstants.MACRO_PATTERN_INDEX_OPT_PARAS);
        String opt_para = macro;
        if ( macro == null ){
            macro = m.group( GlobalConstants.MACRO_PATTERN_INDEX_MACRO );
        } else {
            macro = macro.substring(1, macro.length()-1);
            String[] infos = macro.split( GlobalConstants.MACRO_OPT_PARAS_SPLITTER );
            macro = infos[1];
            num = Integer.parseInt(infos[0]);
        }

        List<FeatureSet> list = lexicon.getFeatureSets(macro);
        if ( list == null || list.isEmpty() ){
            LOG.info("SKIP "
                    + m.group(GlobalConstants.MACRO_PATTERN_INDEX_MACRO)
                    + " (Reason: Cannot find FeatureSet)" );
            return null;
        }

        return chooseFeatureSet(list, opt_para, num, m);
    }

    private FeatureSet chooseFeatureSet(
            List<FeatureSet> list,
            String opt_para,
            Integer num,
            Matcher m
    ) {
        FeatureSet alternativeF = null;
        FeatureSet dlmfF = null;
        for ( FeatureSet f : list ){
            if ( f.getFeatureSetName().matches( Keys.KEY_DLMF_MACRO ) ) {
                dlmfF = f;
            } else if (
                    f.getFeatureSetName().matches( Keys.KEY_DLMF_MACRO_OPTIONAL_PREFIX+num ) ) {
                alternativeF = f;
            }
        }

        String paras = m.group(GlobalConstants.MACRO_PATTERN_INDEX_OPT_PARAS_ELEMENTS);
        return logAndChooseFeatureSet(dlmfF, alternativeF, opt_para, paras);
    }

    private FeatureSet logAndChooseFeatureSet(
            FeatureSet dlmfF,
            FeatureSet alternativeF,
            String opt_para,
            String paras
    ) {
        String macro_col = lineAnalyzer.getValue( Keys.KEY_DLMF );
        if ( opt_para != null ){
            return checkFeatureSet(alternativeF, "No alternative feature set found, but is required!", macro_col);
        } else if (paras != null) {
            LOG.warn("Parameters are not in special syntax. " +
                    "Has to be defined as 'X<digit>:<name>X<Macro>'. [CAS: " + cas + ", Macro: " + macro_col + "]");
            return null;
        } else {
            return checkFeatureSet(dlmfF, "There is no feature set for this term?", macro_col);
        }
    }

    private void addFurtherInfoToFeature(FunctionInfoHolder holder, FeatureSet fset, String casPrefix) {
        if ( holder != null && holder.getCasFunctionName() != null ){
            CASFunctionMetaInfo info = cache.get( holder.getCasFunctionName(), holder.getNumVars() );
            LexiconConverterUtility.fillFeatureWithInfos(info, fset, casPrefix);
        }
    }

    private FeatureSet checkFeatureSet(FeatureSet fset, String message, String macro) {
        if ( fset == null ){
            LOG.warn(message + " [CAS: " + cas + ", Macro: " + macro + "]");
            return null;
        } return fset;
    }
}
