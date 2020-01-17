package gov.nist.drmf.interpreter.evaluation.numeric;

import gov.nist.drmf.interpreter.common.constants.GlobalPaths;
import gov.nist.drmf.interpreter.constraints.IConstraintTranslator;
import gov.nist.drmf.interpreter.evaluation.EvaluationConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;

import static gov.nist.drmf.interpreter.evaluation.numeric.NumericalTestConstants.*;

/**
 * @author Andre Greiner-Petter
 */
public class NumericalConfig implements EvaluationConfig {

    private static final Logger LOG = LogManager.getLogger(NumericalConfig.class.getName());

    public static final String ENTRY_SPLITTER = ",";

    private static final NumericalConfig config = new NumericalConfig();

    private NumericalConfig () {
        try ( FileInputStream in = new FileInputStream(GlobalPaths.PATH_NUMERICAL_SETUP.toFile()) ){
            Properties props = new Properties();
            props.load(in);

            for ( NumericalProperties np : NumericalProperties.values() ){
                String val = props.getProperty(np.key);
                np.setValue(val);
            }
            LOG.debug( "Successfully loaded config for numerical tests." );
        } catch ( IOException ioe ){
            LOG.fatal("Cannot load the maple native directory " +
                    "information from the given " + GlobalPaths.PATH_MAPLE_CONFIG.getFileName() +
                    " file.", ioe
            );
        }
    }

    public static NumericalConfig config(){
        return config;
    }

    public Path getDataset(){
        return Paths.get(NumericalProperties.KEY_DATASET.value);
    }

    public Path getLabelSet(){
        return Paths.get(NumericalProperties.KEY_LABELSET.value);
    }

    public Path getOutputPath(){
        return Paths.get(NumericalProperties.KEY_OUTPUT.value);
    }

    @Override
    public int[] getSubSetInterval(){
        String in = NumericalProperties.KEY_SUBSET.value;
        if ( in == null ) return null;

        String[] splitted = in.split(",");
        return new int[]{
                Integer.parseInt(splitted[0]),
                Integer.parseInt(splitted[1])
        };
    }

    public String getNumericalValues(){
        return NumericalProperties.KEY_VALUES.value;
    }

    public int getMaximumNumberOfCombs(){
        return Integer.parseInt(NumericalProperties.KEY_SKIP_IF_MORE_COMBS.value);
    }

    public String getRawTestExpression(){
        return NumericalProperties.KEY_EXPR.value;
    }

    public String getTestExpression( ICASEngineNumericalEvaluator evaluator, String LHS, String RHS ){
        String in = NumericalProperties.KEY_EXPR.value;

        if ( LHS == null || LHS.isEmpty() ){
            LOG.debug("LHS is 0, use special " + NumericalProperties.KEY_IF_LHS_NULL.key + " pattern.");
            in = NumericalProperties.KEY_IF_LHS_NULL.value;
        } else if ( RHS == null || RHS.isEmpty() ) {
            LOG.debug("RHS is 0, use special " + NumericalProperties.KEY_IF_LHS_NULL.key + " pattern.");
            in = NumericalProperties.KEY_IF_RHS_NULL.value;
        }

        in = in.replaceAll( PATTERN_LHS, Matcher.quoteReplacement(LHS) );
        in = in.replaceAll( PATTERN_RHS, Matcher.quoteReplacement(RHS) );
//        in = "evalf(" + in + ")";
        return evaluator.generateNumericalTestExpression(in);
    }

    public String getExpectationTemplate(){
        String in = NumericalProperties.KEY_EXPECT.value;
        in = in.replaceAll( PATTERN_RES, PATTERN_SIEVE_METHOD_RESULT );
        in = in.replaceAll( PATTERN_THRESHOLD, Double.toString(getThreshold()) );
        return in;
    }

    public double getThreshold(){
        String in = NumericalProperties.KEY_THRESHOLD.value;
        return Double.parseDouble(in);
    }

    public int getPrecision(){
        String in = NumericalProperties.KEY_PREC.value;
        return Integer.parseInt(in);
    }

    public boolean showDLMFLinks(){
        String in = NumericalProperties.KEY_DLMF_LINK.value;
        return in.equals("true");
    }

    public String getSpecialVariables(){
        return NumericalProperties.KEY_SPECIAL_VARS.value;
    }

    public Path getSymbolicResultsPath() {
        try {
            return Paths.get(NumericalProperties.KEY_PREV_RESULTS.value);
        } catch (NullPointerException npe) {
            return null;
        }
    }

    public String getSpecialVariablesValues(){
        return NumericalProperties.KEY_SPECIAL_VARS_VALUES.value;
    }

    private List<String> numericalValues = null, specVars = null, specVarsVals = null;

    public List<String> getListOfNumericalValues(IConstraintTranslator translator, String label) {
        if ( numericalValues == null ) {
            numericalValues = translateElements(translator, getNumericalValues(), label);
        }
        return numericalValues;
    }

    public List<String> getListOfSpecialVariables(IConstraintTranslator translator, String label) {
        if ( specVars == null ) {
            specVars = translateElements(translator, getSpecialVariables(), label);
        }
        return specVars;
    }

    public List<String> getListOfSpecialVariableValues( IConstraintTranslator translator, String label ) {
        if ( specVarsVals == null ) {
            specVarsVals = translateElements(translator, getSpecialVariablesValues(), label);
        }
        return specVarsVals;
    }

    private List<String> translateElements(IConstraintTranslator translator, String str, String label) {
        LinkedList<String> results = new LinkedList<>();

        if ( str == null || str.length() < 2 ) return results;

        str = str.substring(1, str.length()-1);
        String[] elements = str.split(ENTRY_SPLITTER);
        String[] transEls = translator.translateEachConstraint(elements, label);
        results.addAll(Arrays.asList(transEls));
        return results;
    }

    @Override
    public String getTestExpression() {
        return getRawTestExpression();
    }

    @Override
    public Path getMissingMacrosOutputPath() {
        if ( NumericalProperties.KEY_MISSING_MACRO_OUTPUT.value != null )
            return Paths.get(NumericalProperties.KEY_MISSING_MACRO_OUTPUT.value);
        else return null;
    }

    public enum NumericalProperties{
        KEY_DATASET("dlmf_dataset", null),
        KEY_LABELSET("dlmf_labelset", null),
        KEY_SUBSET("subset_tests", null),
        KEY_MISSING_MACRO_OUTPUT("missing_macro_output", null),
        KEY_VALUES("numerical_values", null),
        KEY_EXPR("test_expression", null),
        KEY_EXPECT("test_expectation", null),
        KEY_THRESHOLD("test_threshold", null),
        KEY_PREC("test_precision", null),
        KEY_IF_RHS_NULL("test_if_rhs_null", null),
        KEY_IF_LHS_NULL("test_if_lhs_null", null),
        KEY_OUTPUT("output", null),
        KEY_DLMF_LINK("show_dlmf_links", null),
        KEY_SKIP_IF_MORE_COMBS("skip_if_more_combintations", null),
        KEY_SPECIAL_VARS("special_variables", null),
        KEY_SPECIAL_VARS_VALUES("special_variables_values", null),
        KEY_PREV_RESULTS("symbolic_results_data", null);

        private String key, value;

        NumericalProperties( String key, String value ){
            this.key = key;
            this.value = value;
        }

        public void setValue( String value ){
            this.value = value;
        }
    }
}
