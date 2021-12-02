package gov.nist.drmf.interpreter.common.eval;

import gov.nist.drmf.interpreter.common.constants.GlobalPaths;
import gov.nist.drmf.interpreter.common.interfaces.IConstraintTranslator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;

import static gov.nist.drmf.interpreter.common.eval.NumericalTestConstants.*;

/**
 * @author Andre Greiner-Petter
 */
public class NumericalConfig implements EvaluationConfig {

    private static final Logger LOG = LogManager.getLogger(NumericalConfig.class.getName());

    private final Map<NumericalProperties, String> settings;

    public static final String ENTRY_SPLITTER = ",";

    public NumericalConfig() {
        this(GlobalPaths.PATH_NUMERICAL_SETUP);
    }

    public NumericalConfig(Path configFile) {
        settings = new HashMap<>();
        try (FileInputStream in = new FileInputStream(configFile.toFile())) {
            Properties props = new Properties();
            props.load(in);

            for (NumericalProperties np : NumericalProperties.values()) {
                String val = props.getProperty(np.key);
                if (val != null && !val.isBlank())
                    settings.put(np, val); // set definition value
                else settings.put(np, np.value); // store default value
            }
            LOG.debug("Successfully loaded config for numerical tests.");
        } catch (IOException ioe) {
            LOG.fatal("Cannot load the numerical test config from " + configFile.getFileName(), ioe);
        }
    }

    public Path getDataset() {
        return Paths.get(settings.get(NumericalProperties.KEY_DATASET));
    }

    public Path getLabelSet() {
        return Paths.get(settings.get(NumericalProperties.KEY_LABELSET));
    }

    public Path getOutputPath() {
        return Paths.get(settings.get(NumericalProperties.KEY_OUTPUT));
    }

    @Override
    public int[] getSubSetInterval() {
        String in = settings.get(NumericalProperties.KEY_SUBSET);
        if (in == null) return null;

        String[] splitted = in.split(",");
        return new int[]{
                Integer.parseInt(splitted[0]),
                Integer.parseInt(splitted[1])
        };
    }

    public String getNumericalValues() {
        return settings.get(NumericalProperties.KEY_VALUES);
    }

    public int getMaximumNumberOfCombs() {
        return Integer.parseInt(settings.get(NumericalProperties.KEY_SKIP_IF_MORE_COMBS));
    }

    public String getRawTestExpression() {
        return settings.get(NumericalProperties.KEY_EXPR);
    }

    public String getTestExpression(Function<String, String> testExpressionGenerator, String LHS, String RHS) {
        String in = settings.get(NumericalProperties.KEY_EXPR);

        if (LHS == null || LHS.isEmpty()) {
            LOG.debug("LHS is 0, use special " + NumericalProperties.KEY_IF_LHS_NULL.key + " pattern.");
            in = settings.get(NumericalProperties.KEY_IF_LHS_NULL);
            LHS = "";
        } else if (RHS == null || RHS.isEmpty()) {
            LOG.debug("RHS is 0, use special " + NumericalProperties.KEY_IF_LHS_NULL.key + " pattern.");
            in = settings.get(NumericalProperties.KEY_IF_RHS_NULL);
            RHS = "";
        }

        in = in.replaceAll(PATTERN_LHS, Matcher.quoteReplacement(LHS));
        in = in.replaceAll(PATTERN_RHS, Matcher.quoteReplacement(RHS));
//        in = "evalf(" + in + ")";
        return testExpressionGenerator.apply(in);
    }

    public String getExpectationTemplate() {
        String in = settings.get(NumericalProperties.KEY_EXPECT);
        in = in.replaceAll(PATTERN_RES, PATTERN_SIEVE_METHOD_RESULT);
        in = in.replaceAll(PATTERN_THRESHOLD, Double.toString(getThreshold()));
        return in;
    }

    public double getThreshold() {
        String in = settings.get(NumericalProperties.KEY_THRESHOLD);
        return Double.parseDouble(in);
    }

    public int getPrecision() {
        String in = settings.get(NumericalProperties.KEY_PREC);
        return Integer.parseInt(in);
    }

    public boolean showDLMFLinks() {
        String in = settings.get(NumericalProperties.KEY_DLMF_LINK);
        return in.equals("true");
    }

    public String getSpecialVariables() {
        return settings.get(NumericalProperties.KEY_SPECIAL_VARS);
    }

    public Path getSymbolicResultsPath() {
        try {
            return Paths.get(settings.get(NumericalProperties.KEY_PREV_RESULTS));
        } catch (NullPointerException npe) {
            return null;
        }
    }

    public String getSpecialVariablesValues() {
        return settings.get(NumericalProperties.KEY_SPECIAL_VARS_VALUES);
    }

    private List<String> numericalValues = null, specVars = null, specVarsVals = null;

    public List<String> getListOfNumericalValues(IConstraintTranslator<?> translator, String label) {
        if (numericalValues == null) {
            numericalValues = translateElements(translator, getNumericalValues(), label);
        }
        return numericalValues;
    }

    public List<String> getListOfSpecialVariables(IConstraintTranslator<?> translator) {
        if (specVars == null) {
            specVars = translateElements(translator, getSpecialVariables(), null);
        }
        return specVars;
    }

    public List<String> getListOfSpecialVariableValues(IConstraintTranslator<?> translator) {
        if (specVarsVals == null) {
            specVarsVals = translateElements(translator, getSpecialVariablesValues(), null);
        }
        return specVarsVals;
    }

    private List<String> translateElements(IConstraintTranslator<?> translator, String str, String label) {
        LinkedList<String> results = new LinkedList<>();

        if (str == null || str.length() < 2) return results;

        str = str.substring(1, str.length() - 1);
        String[] elements = str.split(ENTRY_SPLITTER);
        String[] transEls = translator.translateEachConstraint(elements, label);
        results.addAll(Arrays.asList(transEls));
        return results;
    }

    public double getTimeout() {
        String val = settings.get(NumericalProperties.KEY_TIMEOUT);
        return Double.parseDouble(val == null ? "0" : val);
    }

    public String getEntireTestSuiteAssumptions() {
        return settings.get(NumericalProperties.KEY_ASSUMPTION);
    }

    public String[] getEntireTestSuiteAssumptionsList() {
        return getEntireTestSuiteAssumptions().split(" \\|\\| ");
    }

    @Override
    public String getTestExpression() {
        return getRawTestExpression();
    }

    @Override
    public Path getMissingMacrosOutputPath() {
        if (settings.get(NumericalProperties.KEY_MISSING_MACRO_OUTPUT) != null)
            return Paths.get(settings.get(NumericalProperties.KEY_MISSING_MACRO_OUTPUT));
        else return null;
    }

    public enum NumericalProperties {
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
        KEY_PREV_RESULTS("symbolic_results_data", null),
        KEY_ASSUMPTION("entire_test_set_assumptions", null),
        KEY_TIMEOUT("timeout", null);

        private final String key, value;

        NumericalProperties(String key, String value) {
            this.key = key;
            this.value = value;
        }
    }
}
