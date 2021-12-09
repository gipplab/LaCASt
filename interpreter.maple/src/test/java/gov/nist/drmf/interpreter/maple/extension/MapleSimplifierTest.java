package gov.nist.drmf.interpreter.maple.extension;

import gov.nist.drmf.interpreter.common.eval.*;
import gov.nist.drmf.interpreter.common.exceptions.ComputerAlgebraSystemEngineException;
import gov.nist.drmf.interpreter.common.meta.DLMF;
import gov.nist.drmf.interpreter.maple.common.SymbolicMapleEvaluatorTypes;
import gov.nist.drmf.interpreter.maple.setup.AssumeMapleAvailability;
import gov.nist.drmf.interpreter.maple.wrapper.Algebraic;
import gov.nist.drmf.interpreter.maple.wrapper.MapleException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Andre Greiner-Petter
 */
@AssumeMapleAvailability
public class MapleSimplifierTest {

    private static MapleSimplifier simplifier;
    private static Path symbolicConfigPath;

    @BeforeAll
    static void setup() {
        simplifier = new MapleSimplifier();
        simplifier.setTimeout(2);
        symbolicConfigPath = Paths.get(MapleSimplifierTest.class.getResource("symbolic_tests.properties").getPath());
    }

    @Test
    void packageTest() throws ComputerAlgebraSystemEngineException, MapleException {
        String testExpression = "(QPochhammer(a, q, - n))-((1)/(QPochhammer(a*(q)^(- n), q, n)))";
        Set<String> reqPackages = new HashSet<>();
        reqPackages.add("QDifferenceEquations,QPochhammer");

        Algebraic algResult = simplifier.simplify(testExpression, reqPackages);
        assertTrue( MapleSimplifier.isZero(algResult), "Not Zero: " + algResult );
    }

    @Test
    void missingPackageTest() throws ComputerAlgebraSystemEngineException, MapleException {
        String testExpression = "(QPochhammer(a, q, - n))-((1)/(QPochhammer(a*(q)^(- n), q, n)))";
        Set<String> reqPackages = new HashSet<>();

        Algebraic algResult = simplifier.simplify(testExpression, reqPackages);
        assertFalse(
                MapleSimplifier.isZero(algResult),
                "Valid result even we did not loaded required packages? This cannot happen."
        );
    }

    @Test
    void eulerTotientTest() throws ComputerAlgebraSystemEngineException {
        String testExpression = "phi(44)";
        Set<String> reqPackages = new HashSet<>();
        reqPackages.add("NumberTheory,phi");

        Algebraic algResult = simplifier.simplify(testExpression, reqPackages);
        assertEquals(
                "20", algResult.toString(),
                "Valid result even we did not loaded required packages? This cannot happen."
        );
    }

    @Test
    void eulerTotientEquivalenceTest() throws ComputerAlgebraSystemEngineException, MapleException {
        String testExpression = "phi(44)*phi(79) - phi(44*79)";
        Set<String> reqPackages = new HashSet<>();
        reqPackages.add("NumberTheory,phi");

        Algebraic algResult = simplifier.simplify(testExpression, reqPackages);
        assertTrue(
                MapleSimplifier.isZero(algResult),
                "Valid result even we did not loaded required packages? This cannot happen."
        );
    }

    @Test
    @DLMF("1.2.E7")
    void simplifyEquivalenceTest() throws ComputerAlgebraSystemEngineException {
        String testExpression = "binomial(z + 1,k) - (binomial(z,k)+binomial(z,k - 1))";
        Algebraic algResult = simplifier.simplify(testExpression, new HashSet<>());
        assertFalse(simplifier.wasAborted(algResult));
        assertFalse(simplifier.isTrue(algResult));
        assertFalse(simplifier.isConditionallyExpected(algResult, 0), "This simple test should not return conditional results");

        assertFalse(simplifier.isAsExpected(algResult, 0),
                "If this test fails, it means Maple's simplify function improved! " +
                        "Update the test case accordingly. With Maple 2020, the simple simplify function was not able to verify this case.");

        String convertTest = SymbolicMapleEvaluatorTypes.EXPAND.buildCommand(testExpression);
        algResult = simplifier.simplify(convertTest, new HashSet<>());
        assertTrue(simplifier.isAsExpected(algResult, 0),
                "Maple 2020 should have been able to simplify this expression to 0 but instead returned: " + algResult.toString());

        String expectedTest = "simplify(expand(binomial(z + 1,k) - (binomial(z,k)+binomial(z,k - 1))))";
        assertEquals(expectedTest, simplifier.getLatestTestExpression(),
                "The latest tested expression did not match the actual latest input.");
    }

    @Test
    @DLMF("1.2.E7")
    void globalAssumptionsTest() throws ComputerAlgebraSystemEngineException {
        String testExpression = "binomial(z + 1,k) - binomial(z,k)+binomial(z,k - 1)";
        List<String> assumptions = new LinkedList<>();
        assumptions.add("z = -1");
        assumptions.add("x::'integer'");
        simplifier.setGlobalSymbolicAssumptions(assumptions);
        Algebraic exp = simplifier.simplify(testExpression, new HashSet<>());
        assertFalse(simplifier.wasAborted(exp));
        assertFalse(simplifier.isTrue(exp));
        assertFalse(simplifier.isConditionallyExpected(exp, 0), "This simple test should not return conditional results");
        assertFalse(simplifier.isAsExpected(exp, 0), "Under the global assumption of z=-1, this equation is incorrect.");
        Set<String> vars = new HashSet<>();
        vars.add("z");
        vars.add("x"); // I know, that's pretty annoying...
        simplifier.resetAssumptions(vars);
    }

    @Test
    @DLMF("1.2.E1")
    void symbolicVerificationOfBinomialsTest() {
        String lhs = "binomial(n,k)";
        String rhs = "(factorial(n))/(factorial(n - k)*factorial(k))";
        String test = "(binomial(n,k)) - ((factorial(n))/(factorial(n - k)*factorial(k)))";

        ISymbolicTestCases[] type = SymbolicMapleEvaluatorTypes.values();
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
        assertEquals( 6, group.getTestCalculations().size(), "Expected 5 different simplification approaches" );
        assertEquals( "0", group.getTestCalculations().get(0).getResultExpression() );
        assertEquals( "0", group.getTestCalculations().get(1).getResultExpression() );
        assertEquals( "0", group.getTestCalculations().get(2).getResultExpression() );
        assertEquals( "0", group.getTestCalculations().get(3).getResultExpression() );
        assertEquals( "0", group.getTestCalculations().get(4).getResultExpression() );
        assertEquals( "0", group.getTestCalculations().get(5).getResultExpression() );
    }

    /**
     * This test is the Maple equivalent to the MathematicaSimplifierTest with the difference that there are
     * no conditional simplification results in Maple. Hence, Maple is simply uncapable of successfully simplifying
     * this equation.
     */
    @Test
    @DLMF("4.2.E2")
    void conditionalResultTest() throws ComputerAlgebraSystemEngineException {
        String testExpression = "(ln(z))" +
                " - " +
                "(int((1)/(t), t = 1..z))";
        Algebraic exp = simplifier.simplify(testExpression, new HashSet<>());
        assertFalse(simplifier.wasAborted(exp));
        assertFalse(simplifier.isTrue(exp));
        assertFalse(simplifier.isAsExpected(exp, 0));
        assertFalse(simplifier.isConditionallyExpected(exp, 0)); // Maple does not have conditional results!
        String conditionalString = simplifier.getCondition(exp);
        assertNotNull(conditionalString);
        assertEquals("", conditionalString);
    }
}
