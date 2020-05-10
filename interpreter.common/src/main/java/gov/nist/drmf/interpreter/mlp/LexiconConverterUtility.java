package gov.nist.drmf.interpreter.mlp;

import gov.nist.drmf.interpreter.common.constants.GlobalConstants;
import gov.nist.drmf.interpreter.common.constants.Keys;
import gov.nist.drmf.interpreter.mlp.data.CASFunctionMetaInfo;
import gov.nist.drmf.interpreter.mlp.data.DLMFTranslationHeaders;
import gov.nist.drmf.interpreter.mlp.data.FunctionInfoHolder;
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

    public static void fillFeatureWithInfos(CASFunctionMetaInfo info, FeatureSet fset, String cas ){
        if ( info == null ) return;
        addFeature(DLMFTranslationHeaders.cas_link, fset, info.getLink(), cas);
        addFeature(DLMFTranslationHeaders.cas_constraint, fset, info.getConstraints(), cas);
        addFeature(DLMFTranslationHeaders.cas_branch_cuts, fset, info.getBranchCuts(), cas);
    }

    private static void addFeature(DLMFTranslationHeaders h, FeatureSet fset, String content, String cas) {
        if ( content != null && !content.isBlank() ) {
            fset.addFeature(
                    h.getFeatureKey(cas),
                    content,
                    MacrosLexicon.SIGNAL_INLINE
            );
        }
    }

    public static FunctionInfoHolder getFuncNameAndFillInteger(
            String expression,
            String error_message,
            LineAnalyzer analyzer
    ) throws NumberFormatException {
        FunctionInfoHolder holder = new FunctionInfoHolder();
        Matcher m = GlobalConstants.GENERAL_CAS_FUNC_PATTERN.matcher( expression );
        if ( !m.matches() ) {
            LOG.debug(error_message);
            return null;
        } else {
            fillHolder(holder, m, expression);
        }

        if ( holder.getNumVars() < 0 ){
            String num_vars_str = analyzer.getValue( Keys.NUM_OF_VARS );
            try { holder.setNumVars(Integer.parseInt(num_vars_str)); }
            catch( NumberFormatException nfe ){
                //LOG.debug("Skip");
            }
        }

        return holder;
    }

    private static void fillHolder(FunctionInfoHolder holder, Matcher m, String expression) {
        String str = m.group( GlobalConstants.GEN_CAS_FUNC_SPECIFIER );
        if ( str != null ){
            holder.setPattern(expression.substring( str.length() ));
            String[] elms = str
                    .substring(1, str.length()-1) // delete leading and last X
                    .split( GlobalConstants.MACRO_OPT_PARAS_SPLITTER ); // split number:name
            holder.setCasFunctionName(elms[1]);
            holder.setNumVars(Integer.parseInt(elms[0]));
        } else {
            holder.setCasFunctionName(m.group( GlobalConstants.GEN_CAS_FUNC_PATTERN_NAME ));
            holder.setPattern(expression);
        }
    }

}
