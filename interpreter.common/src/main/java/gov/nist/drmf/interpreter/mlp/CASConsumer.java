package gov.nist.drmf.interpreter.mlp;

import gov.nist.drmf.interpreter.common.constants.Keys;
import gov.nist.drmf.interpreter.mlp.data.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.regex.Matcher;

/**
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
        String cas_func = null;
        InfoHolder holder = null;
        try {
            lineAnalyzer.setLine(elements);
            String curr_cas = lineAnalyzer.getCasPrefix();

            cas_func = lineAnalyzer.getValue( curr_cas );
            holder = LexiconConverterUtility.getFuncNameAndFillInteger(
                    cas_func,
                    "Skip cache entry: " + cas_func,
                    lineAnalyzer
            );

            if ( holder == null ) return;

            CASInfo info = new CASInfo();
            info.constraints = lineAnalyzer.getValue(
                    DLMFTranslationHeaders.cas_constraint.getCSVKey(curr_cas) );
            info.branch_cuts = lineAnalyzer.getValue(
                    DLMFTranslationHeaders.cas_branch_cuts.getCSVKey(curr_cas) );
            info.link = lineAnalyzer.getValue(
                    DLMFTranslationHeaders.cas_link.getCSVKey(curr_cas) );

            if (info.extra_package != null && !info.extra_package.isEmpty())
                LOG.debug("EXTRA PACKAGE: " + info.extra_package);

            if ( holder.getCasName() != null )
                cache.add( holder.getCasName(), holder.getNumVars(), info );
        } catch ( NumberFormatException nfe ){
            LOG.debug("Skip cache entry, because number of variables is missing for: " + cas_func);
        } catch ( NullPointerException npe ){
            LOG.debug("Skip cache entry, caused by missing information: " + Arrays.toString(elements));
        } catch ( Exception e ){
            LOG.debug("Error - Skip cache entry for: " + Arrays.toString(elements), e);
        }
    }
}
