package gov.nist.drmf.interpreter.evaluation;

import gov.nist.drmf.interpreter.common.GlobalPaths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * @author Andre Greiner-Petter
 */
public class NumericalConfig {

    private static final Logger LOG = LogManager.getLogger(NumericalConfig.class.getName());

    public NumericalConfig () {
        try ( FileInputStream in = new FileInputStream(GlobalPaths.PATH_NUMERICA_SETUP.toFile()) ){
            Properties props = new Properties();
            props.load(in);

            for ( NumericalProperties np : NumericalProperties.values() ){
                String val = props.getProperty(np.key);
                np.setValue(val);
            }
            LOG.info( "Successfully loaded config for numerical tests." );
        } catch ( IOException ioe ){
            LOG.fatal("Cannot load the maple native directory " +
                    "information from the given " + GlobalPaths.PATH_MAPLE_CONFIG.getFileName() +
                    " file.", ioe
            );
        }
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

    public int[] getSubset(){
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

    public int getMaximumNumberOfVariables(){
        return Integer.parseInt(NumericalProperties.KEY_SKIP_VARS.value);
    }

    private static final String
            PATTERN_LHS = "#LHS",
            PATTERN_RHS = "#RHS",
            PATTERN_RES = "#RESULT",
            PATTERN_THRESHOLD = "#THRESHOLD";

    public String getRawTestExpression(){
        return NumericalProperties.KEY_EXPR.value;
    }

    public String getTestExpression( String LHS, String RHS ){
        String in = NumericalProperties.KEY_EXPR.value;

        if ( LHS == null || LHS.isEmpty() ){
            LOG.debug("LHS is 0, use special " + NumericalProperties.KEY_IF_LHS_NULL.key + " pattern.");
            in = NumericalProperties.KEY_IF_LHS_NULL.value;
        } else if ( RHS == null || RHS.isEmpty() ) {
            LOG.debug("RHS is 0, use special " + NumericalProperties.KEY_IF_LHS_NULL.key + " pattern.");
            in = NumericalProperties.KEY_IF_RHS_NULL.value;
        }

        in = in.replaceAll( PATTERN_LHS, LHS );
        in = in.replaceAll( PATTERN_RHS, RHS );
        in = "evalf(" + in + ")";
        return in;
    }

    public String getExpectation( String result ){
        String in = NumericalProperties.KEY_EXPECT.value;
        in = in.replaceAll( PATTERN_RES, result );
        in = in.replaceAll( PATTERN_THRESHOLD, Double.toString(getThreshold()) );
        in = "evalb(" + in + ");";
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

    private enum NumericalProperties{
        KEY_DATASET("dlmf_dataset", null),
        KEY_LABELSET("dlmf_labelset", null),
        KEY_SUBSET("subset_tests", null),
        KEY_VALUES("numerical_values", null),
        KEY_EXPR("test_expression", null),
        KEY_EXPECT("test_expectation", null),
        KEY_THRESHOLD("test_threshold", null),
        KEY_PREC("test_precision", null),
        KEY_IF_RHS_NULL("test_if_rhs_null", null),
        KEY_IF_LHS_NULL("test_if_lhs_null", null),
        KEY_OUTPUT("output", null),
        KEY_DLMF_LINK("show_dlmf_links", null),
        KEY_SKIP_VARS("skip_num_vars", null);

        private String key, value;

        NumericalProperties( String key, String value ){
            this.key = key;
            this.value = value;
        }

        void setValue( String value ){
            this.value = value;
        }
    }
}
