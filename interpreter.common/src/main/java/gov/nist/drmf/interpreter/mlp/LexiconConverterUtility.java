package gov.nist.drmf.interpreter.mlp;

import gov.nist.drmf.interpreter.common.constants.GlobalConstants;
import gov.nist.drmf.interpreter.common.constants.Keys;
import gov.nist.drmf.interpreter.mlp.data.CASInfo;
import gov.nist.drmf.interpreter.mlp.data.DLMFTranslationHeaders;
import gov.nist.drmf.interpreter.mlp.data.InfoHolder;
import gov.nist.drmf.interpreter.mlp.data.LexiconInfoConsumer;
import mlp.FeatureSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.regex.Matcher;

/**
 * @author Andre Greiner-Petter
 */
public abstract class LexiconConverterUtility {
    private static final Logger LOG = LogManager.getLogger(LexiconConverterUtility.class.getName());

    public static final String DELIMITER = ";";

    public static void parseCSV(
            String prefix,
            BufferedReader br,
            LexiconInfoConsumer consumer
    ) throws IOException {
        String headerLine = br.readLine();

        if ( headerLine != null && !headerLine.isEmpty() ){
            String[] header = headerLine.split( DELIMITER );
            LineAnalyzer lineAnalyzer = new LineAnalyzer( prefix, DELIMITER, header );
            consumer.setLineAnalyzer(lineAnalyzer);
        } else {
            throw new IOException("empty header");
        }

        br.lines()//.limit(21) // TODO limit for debug
                .sequential() // Map is not thread safe
                .filter( line -> !line.startsWith( DELIMITER ) )
                .map( line -> line += line.endsWith(DELIMITER) ? " " : "" )
                .map( line -> line.split(DELIMITER) )
                .forEach( consumer );
    }

    public static void fillFeatureWithInfos(CASInfo info, FeatureSet fset, String curr_cas ){
        if ( info == null ) return;
        DLMFTranslationHeaders h = DLMFTranslationHeaders.cas_link;
        fset.addFeature(
                h.getFeatureKey(curr_cas),
                info.getLink(),
                MacrosLexicon.SIGNAL_INLINE
        );
        h = DLMFTranslationHeaders.cas_constraint;
        fset.addFeature(
                h.getFeatureKey(curr_cas),
                info.getConstraints(),
                MacrosLexicon.SIGNAL_INLINE
        );
        h = DLMFTranslationHeaders.cas_branch_cuts;
        fset.addFeature(
                h.getFeatureKey(curr_cas),
                info.getBranch_cuts(),
                MacrosLexicon.SIGNAL_INLINE
        );
    }

    public static InfoHolder getFuncNameAndFillInteger(
            String expression,
            String error_message,
            LineAnalyzer analyzer
    ) throws NumberFormatException {
        InfoHolder holder = new InfoHolder();
        Matcher m = GlobalConstants.GENERAL_CAS_FUNC_PATTERN.matcher( expression );
        if ( !m.matches() ) {
            LOG.debug(error_message);
            return null;
        } else {
            String str = m.group( GlobalConstants.GEN_CAS_FUNC_SPECIFIER );
            if ( str != null ){
                holder.setPattern(expression.substring( str.length() ));
                String[] elms = str
                        .substring(1, str.length()-1) // delete leading and last X
                        .split( GlobalConstants.MACRO_OPT_PARAS_SPLITTER ); // split number:name
                holder.setCasName(elms[1]);
                holder.setNumVars(Integer.parseInt(elms[0]));

            } else {
                holder.setCasName(m.group( GlobalConstants.GEN_CAS_FUNC_PATTERN_NAME ));
                holder.setPattern(expression);
            }
        }

        if ( holder.getNumVars() == null ){
            String num_vars_str = analyzer.getValue( Keys.NUM_OF_VARS );
            try { holder.setNumVars(Integer.parseInt(num_vars_str)); }
            catch( NumberFormatException nfe ){
                //LOG.debug("Skip");
            }
        }

        return holder;
    }

}
