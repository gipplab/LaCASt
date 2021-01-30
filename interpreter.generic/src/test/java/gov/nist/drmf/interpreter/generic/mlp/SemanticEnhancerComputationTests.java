package gov.nist.drmf.interpreter.generic.mlp;

import com.fasterxml.jackson.core.JsonProcessingException;
import gov.nist.drmf.interpreter.common.constants.Keys;
import gov.nist.drmf.interpreter.common.eval.TestResultType;
import gov.nist.drmf.interpreter.common.meta.DLMF;
import gov.nist.drmf.interpreter.common.eval.NumericResult;
import gov.nist.drmf.interpreter.common.eval.SymbolicResult;
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
        assertEquals(TestResultType.SKIPPED, nr.overallResult() );
        assertEquals(nr.getNumberOfTotalTests(), 0);
        assertEquals(nr.getNumberOfFailedTests(), 0);
        assertEquals(nr.getTestCalculationsGroups().size(), 0);
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
        assertEquals(TestResultType.FAILURE, sr.overallResult());
        assertTrue( sr.getNumberOfTotalTests() > 0 );
        assertTrue( sr.getTestCalculationsGroups().size() > 0 );
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
        assertEquals(TestResultType.FAILURE, nr.overallResult());
        assertTrue( nr.getNumberOfTotalTests() > 0 );
        assertTrue( nr.getNumberOfFailedTests() > 0 );
        assertTrue( nr.getTestCalculationsGroups().size() > 0 );
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
    void numericMultiRelationComputationMathematicaTest() {
        NumericResult nr = enhancer.computeNumerically("x < x^2 < x^3", Keys.KEY_MATHEMATICA);
        assertNotNull( nr );
        assertEquals(TestResultType.FAILURE, nr.overallResult());
        assertTrue( nr.getNumberOfTotalTests() > 0 );
        assertTrue( nr.getNumberOfFailedTests() > 0 );
        assertEquals( 2, nr.getTestCalculationsGroups().size() );
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
        assertEquals(TestResultType.SKIPPED, nr.overallResult());
        assertEquals(nr.getNumberOfTotalTests(), 0);
        assertEquals(nr.getNumberOfFailedTests(), 0);
        assertEquals(nr.getTestCalculationsGroups().size(), 0);
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
        assertEquals(TestResultType.FAILURE, sr.overallResult());
        assertTrue( sr.getNumberOfTotalTests() > 0 );
        assertTrue( sr.getTestCalculationsGroups().size() > 0 );
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
        assertEquals(TestResultType.FAILURE, nr.overallResult());
        assertTrue( nr.getNumberOfTotalTests() > 0 );
        assertTrue( nr.getNumberOfFailedTests() > 0 );
        assertTrue( nr.getTestCalculationsGroups().size() > 0 );
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
    void numericMultiRelationComputationMapleTest() {
        NumericResult nr = enhancer.computeNumerically("x < x^2 < x^3", Keys.KEY_MAPLE);
        assertNotNull( nr );
        assertEquals(TestResultType.FAILURE, nr.overallResult());
        assertTrue( nr.getNumberOfTotalTests() > 0 );
        assertTrue( nr.getNumberOfFailedTests() > 0 );
        assertEquals( 2, nr.getTestCalculationsGroups().size() );
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
        assertEquals(TestResultType.SUCCESS, nr.overallResult());
        assertTrue( nr.getNumberOfTotalTests() > 0 );
        assertEquals( nr.getNumberOfTotalTests(), nr.getNumberOfSuccessfulTests() );
        assertEquals( 0, nr.getNumberOfFailedTests() );
        assertFalse( nr.getTestCalculationsGroups().isEmpty() );
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
        assertEquals(TestResultType.SUCCESS, nr.overallResult());
        assertTrue( nr.getNumberOfTotalTests() > 0 );
        assertEquals( nr.getNumberOfTotalTests(), nr.getNumberOfSuccessfulTests() );
        assertEquals( 0, nr.getNumberOfFailedTests() );
        assertFalse( nr.getTestCalculationsGroups().isEmpty() );
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
    void numericComputationJacobiMapleTest() {
        NumericResult nr = enhancer.computeNumerically(
                "\\JacobipolyP{\\alpha}{\\beta}{n}@{z} = " +
                        "\\frac{\\EulerGamma@{\\alpha + n + 1}}{n! \\EulerGamma@{\\alpha + \\beta + n + 1}} " +
                        "\\sum_{m=0}^n{n\\choose m} \\frac" +
                            "{\\EulerGamma@{\\alpha + \\beta + n + m + 1}}" +
                            "{\\EulerGamma@{\\alpha + m + 1}}" +
                        "(\\frac{z-1}{2})^m",
                Keys.KEY_MAPLE
        );
        assertNotNull( nr );
        assertEquals(TestResultType.SUCCESS, nr.overallResult());
        assertTrue( nr.getNumberOfTotalTests() > 0 );
        assertEquals( nr.getNumberOfTotalTests(), nr.getNumberOfSuccessfulTests() );
        assertEquals( 0, nr.getNumberOfFailedTests() );
        assertFalse( nr.getTestCalculationsGroups().isEmpty() );
    }

    @Test
    @AssumeMapleAvailability
    void numericComputationJacobiMathematicaTest() {
        NumericResult nr = enhancer.computeNumerically(
                "\\JacobipolyP{\\alpha}{\\beta}{n}@{z} = " +
                        "\\frac{\\EulerGamma@{\\alpha + n + 1}}{n! \\EulerGamma@{\\alpha + \\beta + n + 1}} " +
                        "\\sum_{m=0}^n{n\\choose m} \\frac" +
                        "{\\EulerGamma@{\\alpha + \\beta + n + m + 1}}" +
                        "{\\EulerGamma@{\\alpha + m + 1}}" +
                        "(\\frac{z-1}{2})^m",
                Keys.KEY_MATHEMATICA
        );
        assertNotNull( nr );
        assertEquals(TestResultType.SUCCESS, nr.overallResult());
        assertTrue( nr.getNumberOfTotalTests() > 0 );
        assertEquals( nr.getNumberOfTotalTests(), nr.getNumberOfSuccessfulTests() );
        assertEquals( 0, nr.getNumberOfFailedTests() );
        assertFalse( nr.getTestCalculationsGroups().isEmpty() );
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
        assertEquals(TestResultType.SUCCESS, sr.overallResult());
        assertTrue( sr.getNumberOfTotalTests() > 0 );
        assertTrue( sr.getTestCalculationsGroups().size() > 0 );
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
        assertEquals(TestResultType.SUCCESS, sr.overallResult());
        assertTrue( sr.getNumberOfTotalTests() > 0 );
        assertTrue( sr.getTestCalculationsGroups().size() > 0 );
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
    void numericComputationMapleJacobiTest() {
        NumericResult nr = enhancer.computeNumerically(
                "\\JacobipolyP{\\alpha}{\\beta}{0}@{x} = 2",
                Keys.KEY_MAPLE
        );
        assertNotNull( nr );
        assertEquals( 1, nr.getNumberOfTotalTests() );
        assertEquals( 1, nr.getNumberOfFailedTests() );
        assertEquals( 0, nr.getNumberOfSuccessfulTests() );
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
    void scorerDefinitionMathematicaTest() {
        NumericResult nr = enhancer.computeNumerically(
                "\\ScorerGi@{x} = \\frac{1}{\\cpi} \\int_0^\\infty \\sin(\\frac{t^3}{3} + xt) \\diff{t}",
                Keys.KEY_MATHEMATICA
        );
        assertNotNull( nr );
        assertEquals(TestResultType.SKIPPED, nr.overallResult());
        assertTrue(nr.wasAborted());
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
    void lommelMapleTest() {
        NumericResult nr = enhancer.computeNumerically(
                "\\Lommels{\\mu}{\\nu}@{z} = \\frac{\\cpi}{2} [\\BesselY{\\nu}@{z} \\int_{0}^{z} x^{\\mu} \\BesselJ{\\nu}@{x} \\diff{x} - \\BesselJ{\\nu}@{z} \\int_{0}^{z} x^{\\mu} \\BesselY{\\nu}@{x} \\diff{x}]",
                Keys.KEY_MAPLE
        );
        assertNotNull( nr );
        try {
            String representation = SemanticEnhancedDocument.getMapper().writeValueAsString(nr);
            assertFalse( representation.matches(".*[Ee](rror|RROR).*") );
            LOG.debug(representation);
        } catch (JsonProcessingException e) {
            LOG.debug("Unable to print numeric test calculation");
        }

    }
}
