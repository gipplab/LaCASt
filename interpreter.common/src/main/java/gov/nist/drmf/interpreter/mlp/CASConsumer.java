package gov.nist.drmf.interpreter.mlp;

import gov.nist.drmf.interpreter.mlp.data.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;

/**
 * This loads info from CAS_Maple.csv or CAS_Mathematica.csv.
 * It only loads the rudimentary information from these files, such as links, constraints, etc.
 * @author Andre Greiner-Petter
 */
public class CASConsumer implements LexiconInfoConsumer {
    private static final Logger LOG = LogManager.getLogger(CASConsumer.class.getName());

    private LineAnalyzer lineAnalyzer;

    private final CASCache cache;

    public CASConsumer(CASCache cache) {
        this.cache = cache;
    }

    @Override
    public void setLineAnalyzer(LineAnalyzer lineAnalyzer) {
        this.lineAnalyzer = lineAnalyzer;
    }

    @Override
    public void accept(String[] elements) {
        lineAnalyzer.setLine(elements);
        try {
            FunctionInfoHolder holder = getInfoHolder();
            if (holder == null) return;

            CASFunctionMetaInfo info = getCasInfo(lineAnalyzer.getCasPrefix());

            if (holder.getCasFunctionName() != null)
                cache.add(holder.getCasFunctionName(), holder.getNumVars(), info);
        } catch (NumberFormatException nfe) {
            String cas = lineAnalyzer.getCasPrefix();
            LOG.debug("Skip cache entry, because number of variables is missing for: " + lineAnalyzer.getValue(cas));
        } catch (NullPointerException npe) {
            LOG.debug("Skip cache entry, caused by missing information: " + Arrays.toString(elements));
        } catch (Exception e) {
            LOG.debug("Error - Skip cache entry for: " + Arrays.toString(elements), e);
        }
    }

    private CASFunctionMetaInfo getCasInfo(String currCas) {
        CASFunctionMetaInfo info = new CASFunctionMetaInfo();
        String value = lineAnalyzer.getValue(DLMFTranslationHeaders.cas_constraint.getCSVKey(currCas));
        if ( !value.isEmpty() ) info.setConstraints(value);

        value = lineAnalyzer.getValue(DLMFTranslationHeaders.cas_branch_cuts.getCSVKey(currCas));
        if ( !value.isEmpty() ) info.setBranchCuts(value);

        value = lineAnalyzer.getValue(DLMFTranslationHeaders.cas_link.getCSVKey(currCas));
        if ( !value.isEmpty() ) {
            if ( value.startsWith("https://") ) value = value.substring("https://".length());
            info.setLink(value);
        }

        return info;
    }

    private FunctionInfoHolder getInfoHolder() {
        String cas_func = lineAnalyzer.getValue(lineAnalyzer.getCasPrefix());
        return LexiconConverterUtility.getFuncNameAndFillInteger(
                cas_func,
                "Skip cache entry: " + cas_func,
                lineAnalyzer
        );
    }
}
