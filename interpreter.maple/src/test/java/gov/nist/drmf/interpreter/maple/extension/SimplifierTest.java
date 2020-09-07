package gov.nist.drmf.interpreter.maple.extension;

import com.maplesoft.externalcall.MapleException;
import com.maplesoft.openmaple.Algebraic;
import gov.nist.drmf.interpreter.common.exceptions.ComputerAlgebraSystemEngineException;
import gov.nist.drmf.interpreter.maple.setup.AssumeMapleAvailability;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Andre Greiner-Petter
 */
@AssumeMapleAvailability
public class SimplifierTest {

    private static Simplifier simplifier;

    @BeforeAll
    static void setup() {
        simplifier = new Simplifier();
        simplifier.setTimeout(2);
    }

    @Test
    void packageTest() throws ComputerAlgebraSystemEngineException, MapleException {
        String testExpression = "(QPochhammer(a, q, - n))-((1)/(QPochhammer(a*(q)^(- n), q, n)))";
        Set<String> reqPackages = new HashSet<>();
        reqPackages.add("QDifferenceEquations,QPochhammer");

        Algebraic algResult = simplifier.simplify(testExpression, reqPackages);
        assertTrue( Simplifier.isZero(algResult), "Not Zero: " + algResult );
    }

    @Test
    void missingPackageTest() throws ComputerAlgebraSystemEngineException, MapleException {
        String testExpression = "(QPochhammer(a, q, - n))-((1)/(QPochhammer(a*(q)^(- n), q, n)))";
        Set<String> reqPackages = new HashSet<>();

        Algebraic algResult = simplifier.simplify(testExpression, reqPackages);
        assertFalse(
                Simplifier.isZero(algResult),
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
                Simplifier.isZero(algResult),
                "Valid result even we did not loaded required packages? This cannot happen."
        );
    }

}
