package gov.nist.drmf.interpreter.common.eval;

import gov.nist.drmf.interpreter.common.constants.GlobalPaths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;

import static gov.nist.drmf.interpreter.common.eval.NumericalTestConstants.PATTERN_LHS;
import static gov.nist.drmf.interpreter.common.eval.NumericalTestConstants.PATTERN_RHS;

/**
 * @author Andre Greiner-Petter
 */
public class SymbolicalConfig implements EvaluationConfig {

    private static final Logger LOG = LogManager.getLogger(SymbolicalConfig.class.getName());

    private final Map<SymbolicProperties, String> settings;

    public SymbolicalConfig(ISymbolicTestCases[] symbolicTestCases) {
        this(symbolicTestCases, GlobalPaths.PATH_SYMBOLIC_SETUP);
    }

    public SymbolicalConfig(ISymbolicTestCases[] symbolicTestCases, Path configFile) {
        this.settings = new HashMap<>();
        try ( FileInputStream in = new FileInputStream(configFile.toFile()) ){
            Properties props = new Properties();
            props.load(in);

            for ( SymbolicalConfig.SymbolicProperties np : SymbolicalConfig.SymbolicProperties.values() ){
                String val = props.getProperty(np.key);
                if ( val != null && !val.isBlank() )
                    settings.put(np, val); // set definition value
                else settings.put(np, np.value); // store default value
            }

            activateSymbolicTests(symbolicTestCases);
            LOG.info( "Successfully loaded config for symbolic tests." );
        } catch ( IOException ioe ){
            LOG.fatal("Cannot load the symbolic test config from " + configFile.getFileName(), ioe);
        }
    }

    private void activateSymbolicTests(ISymbolicTestCases[] testCases) {
        for ( ISymbolicTestCases test : testCases ) {
            switch ( test.getID() ) {
                case SIMPLE: test.setActivated(true); break;
                case CONV_EXP: test.setActivated(enabledConvEXP()); break;
                case CONV_HYP: test.setActivated(enabledConvHYP()); break;
                case EXPAND: test.setActivated(enabledExpand()); break;
                case EXPAND_EXP: test.setActivated(enabledExpandWithEXP()); break;
                case EXPAND_HYP: test.setActivated(enabledExpandWithHYP()); break;
            }
        }
    }

    public Path getDataset(){
        return Paths.get(settings.get(SymbolicProperties.KEY_DATASET));
    }

    @Override
    public Path getOutputPath(){
        return Paths.get(settings.get(SymbolicProperties.KEY_OUTPUT));
    }

    @Override
    public Path getMissingMacrosOutputPath() {
        return Paths.get(settings.get(SymbolicProperties.KEY_MISSING_MACRO_OUTPUT));
    }

    @Override
    public int[] getSubSetInterval(){
        String in = settings.get(SymbolicProperties.KEY_SUBSET);
        if ( in == null ) return null;

        String[] splitted = in.split(",");
        return new int[]{
                Integer.parseInt(splitted[0]),
                Integer.parseInt(splitted[1])
        };
    }

    @Override
    public String getTestExpression(){
        return settings.get(SymbolicProperties.KEY_EXPR);
    }

    public String getTestExpression( String LHS, String RHS ){
        String in = settings.get(SymbolicProperties.KEY_EXPR);
        in = in.replaceAll( PATTERN_LHS, Matcher.quoteReplacement(LHS) );
        in = in.replaceAll( PATTERN_RHS, Matcher.quoteReplacement(RHS) );
        return in;
    }

    public String getExpectationValue(){
        String val = settings.get(SymbolicProperties.KEY_EXPECT);
        return val == null ? "0" : val;
    }

    @Override
    public boolean showDLMFLinks(){
        return Boolean.parseBoolean(settings.get(SymbolicProperties.KEY_DLMF_LINK));
    }

    public boolean enabledConvEXP(){
        return Boolean.parseBoolean(settings.get(SymbolicProperties.KEY_ENABLE_CONV_EXP));
    }

    public boolean enabledConvHYP(){
        return Boolean.parseBoolean(settings.get(SymbolicProperties.KEY_ENABLE_CONV_HYP));
    }

    public boolean enabledExpand(){
        return Boolean.parseBoolean(settings.get(SymbolicProperties.KEY_ENABLE_EXPAND));
    }

    public boolean enabledExpandWithEXP(){
        return Boolean.parseBoolean(settings.get(SymbolicProperties.KEY_ENABLE_EXPAND_EXP));
    }

    public boolean enabledExpandWithHYP(){
        return Boolean.parseBoolean(settings.get(SymbolicProperties.KEY_ENABLE_EXPAND_HYP));
    }

    public String getEntireTestSuiteAssumptions(){
        return settings.get(SymbolicProperties.KEY_ASSUMPTION);
    }

    public double getTimeout() {
        return Double.parseDouble(settings.get(SymbolicProperties.KEY_TIMEOUT));
    }

    private enum SymbolicProperties{
        KEY_DATASET("dlmf_dataset", null),
        KEY_SUBSET("subset_tests", null),
        KEY_EXPR("test_expression", null),
        KEY_EXPECT("test_expectation", null),
        KEY_OUTPUT("output", null),
        KEY_MISSING_MACRO_OUTPUT("missing_macro_output", null),
        KEY_DLMF_LINK("show_dlmf_links", null),
        KEY_ENABLE_CONV_EXP("enable_conversion_exp", "true"),
        KEY_ENABLE_CONV_HYP("enable_conversion_hypergeom", "true"),
        KEY_ENABLE_EXPAND("enable_pre_expansion", "true"),
        KEY_ENABLE_EXPAND_EXP("enable_pre_expansion_with_exp", "true"),
        KEY_ENABLE_EXPAND_HYP("enable_pre_expansion_with_hypergeom", "true"),
        KEY_ASSUMPTION("entire_test_set_assumptions", null),
        KEY_TIMEOUT("timeout", "10");

        private final String key, value;

        SymbolicProperties( String key, String value ){
            this.key = key;
            this.value = value;
        }
    }

}
