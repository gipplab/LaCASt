package gov.nist.drmf.interpreter.generic.mlp;

import com.fasterxml.jackson.core.JsonProcessingException;
import gov.nist.drmf.interpreter.common.constants.Keys;
import gov.nist.drmf.interpreter.common.meta.DLMF;
import gov.nist.drmf.interpreter.common.pojo.NumericResult;
import gov.nist.drmf.interpreter.common.pojo.SymbolicResult;
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
            String representation = SemanticEnhancedDocument.getMapper().writeValueAsString(nr);
            assertFalse( representation.matches(".*[Ee](rror|RROR).*") );
            LOG.debug(representation);
        } catch (JsonProcessingException e) {
            LOG.debug("Unable to print numeric test calculation");
        }
    }

    @Test
    @AssumeMathematicaAvailability
    void symbolicComputationMathematicaTest() {
        SymbolicResult sr = enhancer.computeSymbolically("x = y", Keys.KEY_MATHEMATICA);
        assertNotNull( sr );
        assertFalse( sr.isSuccessful() );
        assertTrue( sr.getNumberOfTests() > 0 );
        assertTrue( sr.getTestCalculations().size() > 0 );
        try {
            String representation = SemanticEnhancedDocument.getMapper().writeValueAsString(sr);
            assertFalse( representation.matches(".*[Ee](rror|RROR).*") );
            LOG.debug(representation);
        } catch (JsonProcessingException e) {
            LOG.debug("Unable to print numeric test calculation");
        }
    }

    @Test
    @AssumeMathematicaAvailability
    void numericRelationComputationMathematicaTest() {
        NumericResult nr = enhancer.computeNumerically("x < x^2", Keys.KEY_MATHEMATICA);
        assertNotNull( nr );
        assertFalse( nr.isSuccessful() );
        assertTrue( nr.getNumberOfTotalTests() > 0 );
        assertTrue( nr.getNumberOfFailedTests() > 0 );
        assertTrue( nr.getTestCalculations().size() > 0 );
        try {
            String representation = SemanticEnhancedDocument.getMapper().writeValueAsString(nr);
            assertFalse( representation.matches(".*[Ee](rror|RROR).*") );
            LOG.debug(representation);
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
            String representation = SemanticEnhancedDocument.getMapper().writeValueAsString(nr);
            assertFalse( representation.matches(".*[Ee](rror|RROR).*") );
            LOG.debug(representation);
        } catch (JsonProcessingException e) {
            LOG.debug("Unable to print numeric test calculation");
        }
    }

    @Test
    @AssumeMapleAvailability
    void symbolicComputationMapleTest() {
        SymbolicResult sr = enhancer.computeSymbolically("x = y", Keys.KEY_MAPLE);
        assertNotNull( sr );
        assertFalse( sr.isSuccessful() );
        assertTrue( sr.getNumberOfTests() > 0 );
        assertTrue( sr.getTestCalculations().size() > 0 );
        try {
            String representation = SemanticEnhancedDocument.getMapper().writeValueAsString(sr);
            assertFalse( representation.matches(".*[Ee](rror|RROR).*") );
            LOG.debug(representation);
        } catch (JsonProcessingException e) {
            LOG.debug("Unable to print numeric test calculation");
        }
    }

    @Test
    @AssumeMapleAvailability
    void numericRelationComputationMapleTest() {
        NumericResult nr = enhancer.computeNumerically("x < x^2", Keys.KEY_MAPLE);
        assertNotNull( nr );
        assertFalse( nr.isSuccessful() );
        assertTrue( nr.getNumberOfTotalTests() > 0 );
        assertTrue( nr.getNumberOfFailedTests() > 0 );
        assertTrue( nr.getTestCalculations().size() > 0 );
        try {
            String representation = SemanticEnhancedDocument.getMapper().writeValueAsString(nr);
            assertFalse( representation.matches(".*[Ee](rror|RROR).*") );
            LOG.debug(representation);
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
        try {
            String representation = SemanticEnhancedDocument.getMapper().writeValueAsString(nr);
            assertFalse( representation.matches(".*[Ee](rror|RROR).*") );
            LOG.debug(representation);
        } catch (JsonProcessingException e) {
            LOG.debug("Unable to print numeric test calculation");
        }
    }

    @Test
    @DLMF("4.21.2")
    @AssumeMapleAvailability
    void numericComputationEquivalenceMapleTest() {
        NumericResult nr = enhancer.computeNumerically(
                "\\sin@{x+y} = \\sin@{x}\\cos@{y} + \\cos@{x}\\sin{y}",
                Keys.KEY_MAPLE
        );
        assertNotNull( nr );
        assertTrue( nr.isSuccessful() );
        assertTrue( nr.getNumberOfTotalTests() > 0 );
        assertEquals( nr.getNumberOfTotalTests(), nr.getNumberOfSuccessfulTests() );
        assertEquals( 0, nr.getNumberOfFailedTests() );
        assertTrue( nr.getTestCalculations().isEmpty() );
        try {
            String representation = SemanticEnhancedDocument.getMapper().writeValueAsString(nr);
            assertFalse( representation.matches(".*[Ee](rror|RROR).*") );
            LOG.debug(representation);
        } catch (JsonProcessingException e) {
            LOG.debug("Unable to print numeric test calculation");
        }
    }

    @Test
    @DLMF("4.21.2")
    @AssumeMathematicaAvailability
    void symbolicComputationEquivalenceMathematicaTest() {
        SymbolicResult sr = enhancer.computeSymbolically(
                "\\sin@{x+y} = \\sin@{x}\\cos@{y} + \\cos@{x}\\sin{y}",
                Keys.KEY_MATHEMATICA
        );
        assertNotNull( sr );
        assertTrue( sr.isSuccessful() );
        assertTrue( sr.getNumberOfTests() > 0 );
        assertTrue( sr.getTestCalculations().size() > 0 );
        try {
            String representation = SemanticEnhancedDocument.getMapper().writeValueAsString(sr);
            assertFalse( representation.matches(".*[Ee](rror|RROR).*") );
            LOG.debug(representation);
        } catch (JsonProcessingException e) {
            LOG.debug("Unable to print numeric test calculation");
        }
    }

    @Test
    @DLMF("4.21.2")
    @AssumeMapleAvailability
    void symbolicComputationEquivalenceMapleTest() {
        SymbolicResult sr = enhancer.computeSymbolically(
                "\\sin@{x+y} = \\sin@{x}\\cos@{y} + \\cos@{x}\\sin{y}",
                Keys.KEY_MAPLE
        );
        assertNotNull( sr );
        assertTrue( sr.isSuccessful() );
        assertTrue( sr.getNumberOfTests() > 0 );
        assertTrue( sr.getTestCalculations().size() > 0 );
        try {
            String representation = SemanticEnhancedDocument.getMapper().writeValueAsString(sr);
            assertFalse( representation.matches(".*[Ee](rror|RROR).*") );
            LOG.debug(representation);
        } catch (JsonProcessingException e) {
            LOG.debug("Unable to print numeric test calculation");
        }
    }
}
