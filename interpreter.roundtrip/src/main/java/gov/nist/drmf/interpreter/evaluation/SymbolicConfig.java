package gov.nist.drmf.interpreter.evaluation;

import gov.nist.drmf.interpreter.common.GlobalPaths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.regex.Matcher;

import static gov.nist.drmf.interpreter.evaluation.NumericalTestConstants.PATTERN_LHS;
import static gov.nist.drmf.interpreter.evaluation.NumericalTestConstants.PATTERN_RHS;

/**
 * @author Andre Greiner-Petter
 */
public class SymbolicConfig {

    private static final Logger LOG = LogManager.getLogger(SymbolicConfig.class.getName());

    public SymbolicConfig () {
        try ( FileInputStream in = new FileInputStream(GlobalPaths.PATH_SYMBOLIC_SETUP.toFile()) ){
            Properties props = new Properties();
            props.load(in);

            for ( SymbolicConfig.SymbolicProperties np : SymbolicConfig.SymbolicProperties.values() ){
                String val = props.getProperty(np.key);
                np.setValue(val);
            }

            SymbolicEvaluatorTypes.CONV_EXP.setActivated(enabledConvEXP());
            SymbolicEvaluatorTypes.CONV_HYP.setActivated(enabledConvHYP());
            SymbolicEvaluatorTypes.EXPAND.setActivated(enabledExpand());
            SymbolicEvaluatorTypes.EXPAND_EXP.setActivated(enabledExpandWithEXP());
            SymbolicEvaluatorTypes.EXPAND_HYP.setActivated(enabledExpandWithHYP());

            LOG.info( "Successfully loaded config for symbolic tests." );
        } catch ( IOException ioe ){
            LOG.fatal("Cannot load the maple native directory " +
                    "information from the given " + GlobalPaths.PATH_MAPLE_CONFIG.getFileName() +
                    " file.", ioe
            );
        }
    }

    public Path getDataset(){
        return Paths.get(SymbolicConfig.SymbolicProperties.KEY_DATASET.value);
    }

    public Path getLabelSet(){
        return Paths.get(SymbolicConfig.SymbolicProperties.KEY_LABELSET.value);
    }

    public Path getOutputPath(){
        return Paths.get(SymbolicConfig.SymbolicProperties.KEY_OUTPUT.value);
    }

    public int[] getSubset(){
        String in = SymbolicConfig.SymbolicProperties.KEY_SUBSET.value;
        if ( in == null ) return null;

        String[] splitted = in.split(",");
        return new int[]{
                Integer.parseInt(splitted[0]),
                Integer.parseInt(splitted[1])
        };
    }

    public String getRawTestExpression(){
        return SymbolicConfig.SymbolicProperties.KEY_EXPR.value;
    }

    public String getTestExpression( String LHS, String RHS ){
        String in = SymbolicConfig.SymbolicProperties.KEY_EXPR.value;
        in = in.replaceAll( PATTERN_LHS, Matcher.quoteReplacement(LHS) );
        in = in.replaceAll( PATTERN_RHS, Matcher.quoteReplacement(RHS) );
        return in;
    }

    public String getExpectationValue(){
        return SymbolicConfig.SymbolicProperties.KEY_EXPECT.value;
    }

    public boolean showDLMFLinks(){
        return Boolean.parseBoolean(SymbolicProperties.KEY_DLMF_LINK.value);
    }

    public boolean enabledConvEXP(){
        return Boolean.parseBoolean(SymbolicProperties.KEY_ENABLE_CONV_EXP.value);
    }

    public boolean enabledConvHYP(){
        return Boolean.parseBoolean(SymbolicProperties.KEY_ENABLE_CONV_HYP.value);
    }

    public boolean enabledExpand(){
        return Boolean.parseBoolean(SymbolicProperties.KEY_ENABLE_EXPAND.value);
    }

    public boolean enabledExpandWithEXP(){
        return Boolean.parseBoolean(SymbolicProperties.KEY_ENABLE_EXPAND_EXP.value);
    }

    public boolean enabledExpandWithHYP(){
        return Boolean.parseBoolean(SymbolicProperties.KEY_ENABLE_EXPAND_HYP.value);
    }

    public String getEntireTestSuiteAssumptions(){
        return SymbolicProperties.KEY_ASSUMPTION.value;
    }

    private enum SymbolicProperties{
        KEY_DATASET("dlmf_dataset", null),
        KEY_LABELSET("dlmf_labelset", null),
        KEY_SUBSET("subset_tests", null),
        KEY_EXPR("test_expression", null),
        KEY_EXPECT("test_expectation", null),
        KEY_OUTPUT("output", null),
        KEY_DLMF_LINK("show_dlmf_links", null),
        KEY_ENABLE_CONV_EXP("enable_conversion_exp", null),
        KEY_ENABLE_CONV_HYP("enable_conversion_hypergeom", null),
        KEY_ENABLE_EXPAND("enable_pre_expansion", null),
        KEY_ENABLE_EXPAND_EXP("enable_pre_expansion_with_exp", null),
        KEY_ENABLE_EXPAND_HYP("enable_pre_expansion_with_hypergeom", null),
        KEY_ASSUMPTION("entire_test_set_assumptions", null);

        private String key, value;

        SymbolicProperties( String key, String value ){
            this.key = key;
            this.value = value;
        }

        void setValue( String value ){
            this.value = value;
        }
    }

}
