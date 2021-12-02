package gov.nist.drmf.interpreter.mathematica.extension;

import gov.nist.drmf.interpreter.common.eval.*;
import gov.nist.drmf.interpreter.common.exceptions.ComputerAlgebraSystemEngineException;
import gov.nist.drmf.interpreter.common.meta.DLMF;
import gov.nist.drmf.interpreter.mathematica.MathematicaConnector;
import gov.nist.drmf.interpreter.mathematica.common.AssumeMathematicaAvailability;
import gov.nist.drmf.interpreter.mathematica.common.SymbolicMathematicaEvaluatorTypes;
import gov.nist.drmf.interpreter.mathematica.wrapper.Expr;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Andre Greiner-Petter
 */
@AssumeMathematicaAvailability
public class MathematicaSimplifierTest {

    private static MathematicaSimplifier simplifier;
    private static Path symbolicConfigPath;

    @BeforeAll
    static void setup() {
        MathematicaConnector connector = new MathematicaConnector();
        simplifier = (MathematicaSimplifier) connector.getSymbolicEvaluator();
        symbolicConfigPath = Paths.get(MathematicaSimplifierTest.class.getResource("symbolic_tests.properties").getPath());
    }

    @Test
    void timeoutTest() throws ComputerAlgebraSystemEngineException {
        double timeout = 0.05;
        simplifier.setTimeout(timeout);
        Instant start = Instant.now();
        Expr exp = simplifier.simplify("Integrate[x^a, {x, 0, 1}]", new HashSet<>());
        Instant stop = Instant.now();
        assertTrue(simplifier.wasAborted(exp));
        simplifier.disableTimeout();
        System.out.println("Successfully aborted computation.\n" +
                "Configured timeout after " + timeout*1_000 + "ms\n" +
                "Real elapsed time: " + Duration.between(start, stop).toMillis() + "ms" );
    }

    @Test
    void noTimeoutTest() throws ComputerAlgebraSystemEngineException {
        simplifier.disableTimeout();
        Instant start = Instant.now();
        Expr exp = simplifier.simplify("Integrate[x^a, {x, 0, 1}]", new HashSet<>());
        Instant stop = Instant.now();
        assertFalse(simplifier.wasAborted(exp));
        System.out.println("Configured no timeout.\n" +
                "Elapsed time: " + Duration.between(start, stop).toMillis() + "ms\n" +
                "Result: " + exp.toString());
    }

    @Test
    @DLMF("1.2.E7")
    void simplifyEquivalenceTest() throws ComputerAlgebraSystemEngineException {
        String testExpression = "Binomial[z + 1,k] - (Binomial[z,k]+Binomial[z,k - 1])";
        Expr exp = simplifier.simplify(testExpression, new HashSet<>());
        assertFalse(simplifier.wasAborted(exp));
        assertFalse(simplifier.isTrue(exp));
        assertFalse(simplifier.isConditionallyExpected(exp, 0), "This simple test should not return conditional results");
        assertTrue(simplifier.isAsExpected(exp, 0));

        String expectedTest = "FullSimplify[" + testExpression + "]";
        assertEquals(expectedTest, simplifier.getLatestTestExpression(),
                "The latest tested expression did not match the actual latest input.");
    }

    @Test
    @DLMF("1.2.E7")
    void globalAssumptionsTest() throws ComputerAlgebraSystemEngineException {
        String testExpression = "Binomial[z + 1,k] - (Binomial[z,k]+Binomial[z,k - 1])";
        List<String> assumptions = new LinkedList<>();
        assumptions.add("z == -1");
        assumptions.add("x \\[Element] Integers");
        simplifier.setGlobalSymbolicAssumptions(assumptions);
        Expr exp = simplifier.simplify(testExpression, new HashSet<>());
        assertFalse(simplifier.wasAborted(exp));
        assertFalse(simplifier.isTrue(exp));
        assertFalse(simplifier.isConditionallyExpected(exp, 0), "This simple test should not return conditional results");
        assertFalse(simplifier.isAsExpected(exp, 0), "Under the global assumption of z=-1, this equation is incorrect.");
        simplifier.resetGlobalAssumptions();
    }

    @Test
    @DLMF("1.2.E1")
    void symbolicVerificationOfBinomialsTest() {
        String lhs = "Binomial[n,k]";
        String rhs = "Divide[(n)!,(n - k)!*(k)!]";
        String test = "(Binomial[n,k]) - (Divide[(n)!,(n - k)!*(k)!])";

        ISymbolicTestCases[] type = SymbolicMathematicaEvaluatorTypes.values();
        SymbolicalTest symbTest = new SymbolicalTest(
                lhs, rhs, test,
                type,
                new SymbolicalConfig(type, symbolicConfigPath),
                // we do not require a forward translator for constraints because this test does not has constraints
                null
        );

        SymbolicResult result = simplifier.performSymbolicTest(symbTest);
        assertEquals(TestResultType.SUCCESS, result.overallResult());
        assertFalse(result.crashed());
        assertFalse(result.wasAborted());
        assertEquals(1, result.getTestCalculationsGroups().size());

        SymbolicCalculationGroup group = result.getTestCalculationsGroups().get(0);
        assertEquals( lhs, group.getLhs() );
        assertEquals( rhs, group.getRhs() );
        assertEquals( test, group.getTestExpression() );
        assertEquals( 1, group.getTestCalculations().size() );
        assertEquals( "0", group.getTestCalculations().get(0).getResultExpression() );
    }

    @Test
    @DLMF("4.2.E2")
    void conditionalResultTest() throws ComputerAlgebraSystemEngineException {
        String testExpression = "(Log[z])" +
                " - " +
                "(Integrate[Divide[1,t], {t, 1, z}, GenerateConditions->None])";
        Expr exp = simplifier.simplify(testExpression, new HashSet<>());
        assertFalse(simplifier.wasAborted(exp));
        assertFalse(simplifier.isTrue(exp));
        assertFalse(simplifier.isAsExpected(exp, 0));
        assertTrue(simplifier.isConditionallyExpected(exp, 0));
        String conditionalString = simplifier.getCondition(exp);
        assertNotNull(conditionalString);
        assertEquals("Or[Greater[Re[z], 0], NotElement[z, Reals]]", conditionalString);
    }
}
