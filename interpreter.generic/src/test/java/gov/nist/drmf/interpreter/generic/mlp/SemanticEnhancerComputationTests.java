package gov.nist.drmf.interpreter.generic.mlp;

import com.fasterxml.jackson.core.JsonProcessingException;
import gov.nist.drmf.interpreter.common.constants.Keys;
import gov.nist.drmf.interpreter.common.meta.DLMF;
import gov.nist.drmf.interpreter.common.pojo.NumericResult;
import gov.nist.drmf.interpreter.generic.mlp.pojo.SemanticEnhancedDocument;
import gov.nist.drmf.interpreter.maple.setup.AssumeMapleAvailability;
import gov.nist.drmf.interpreter.mathematica.common.AssumeMathematicaAvailability;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Andre Greiner-Petter
 */
public class SemanticEnhancerComputationTests {
    private static final Logger LOG = LogManager.getLogger(SemanticEnhancerComputationTests.class.getName());
    private static SemanticEnhancer enhancer;

    @BeforeAll
    static void setup() {
        enhancer = new SemanticEnhancer();
    }

    @Test
    @AssumeMathematicaAvailability
    void numericComputationMathematicaTest() {
        NumericResult nr = enhancer.computeNumerically("x - 1", Keys.KEY_MATHEMATICA);
        assertNotNull( nr );
        assertFalse( nr.isSuccessful() );
        assertTrue( nr.getNumberOfTotalTests() > 0 );
        assertTrue( nr.getNumberOfFailedTests() > 0 );
        assertTrue( nr.getTestCalculations().size() > 0 );
        try {
            LOG.debug(SemanticEnhancedDocument.getMapper().writeValueAsString(nr));
        } catch (JsonProcessingException e) {
            LOG.debug("Unable to print numeric test calculation");
        }
    }

    @Test
    @AssumeMapleAvailability
    void numericComputationMapleTest() {
        NumericResult nr = enhancer.computeNumerically("x - 1", Keys.KEY_MAPLE);
        assertNotNull( nr );
        assertFalse( nr.isSuccessful() );
        assertTrue( nr.getNumberOfTotalTests() > 0 );
        assertTrue( nr.getNumberOfFailedTests() > 0 );
        assertTrue( nr.getTestCalculations().size() > 0 );
        try {
            LOG.debug(SemanticEnhancedDocument.getMapper().writeValueAsString(nr));
        } catch (JsonProcessingException e) {
            LOG.debug("Unable to print numeric test calculation");
        }
    }

    @Test
    @DLMF("4.21.2")
    @AssumeMathematicaAvailability
    void numericComputationEquivalenceMathematicaTest() {
        NumericResult nr = enhancer.computeNumerically(
                "\\sin@{x+y} = \\sin@{x}\\cos@{y} + \\cos@{x}\\sin{y}",
                Keys.KEY_MATHEMATICA
        );
        assertNotNull( nr );
        assertTrue( nr.isSuccessful() );
        assertTrue( nr.getNumberOfTotalTests() > 0 );
        assertEquals( nr.getNumberOfTotalTests(), nr.getNumberOfSuccessfulTests() );
        assertEquals( 0, nr.getNumberOfFailedTests() );
        assertTrue( nr.getTestCalculations().isEmpty() );
    }
}
