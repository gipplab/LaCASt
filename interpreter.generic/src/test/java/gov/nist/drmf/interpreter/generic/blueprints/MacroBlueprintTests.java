package gov.nist.drmf.interpreter.generic.blueprints;

import gov.nist.drmf.interpreter.common.meta.AssumeMLPAvailability;
import mlp.ParseException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Andre Greiner-Petter
 */
@AssumeMLPAvailability
@Disabled
public class MacroBlueprintTests {

    private static MacroBlueprint jacobiBlueprint;

    @BeforeAll
    public static void setup() throws ParseException {
        jacobiBlueprint = new MacroBlueprint("P^{(par1, par2)}_{par3} (var1)");
    }

    @Test
    public void straightJacobiPolyBlueprintTest() {
        assertTrue(jacobiBlueprint.match("P^{(\\alpha, \\beta)}_{n} (x)"));
    }

    @Test
    public void reverseJacobiPolyBlueprintTest() {
        assertTrue(jacobiBlueprint.match("P_{n}^{(\\alpha, \\beta)} (x)"));
    }

    @Test
    public void complexArgJacobiPolyBlueprintTest() {
        assertTrue(jacobiBlueprint.match("P^{(\\alpha, \\beta)}_{n} \\left( a \\cos{x} \\right)"));
    }

    @Test
    public void wrongParametersMismatchJacobiPolyBlueprintTest() {
        assertFalse(jacobiBlueprint.match("P^{\\alpha}_{n} (x)"));
    }

    @Test
    public void qJacobiMismatchBlueprintTest() {
        // it's q-Jacobi but not Jacobi, so the match should fail
        assertFalse(jacobiBlueprint.match("P^{(\\alpha, \\beta)}_{n} \\left( x; c, d; q \\right)"));
    }

    @Test
    public void jacobiFunctionMismatchBlueprintTest() {
        // it's Jacobi function but not Jacobi polynomial, so the match should fail
        assertFalse(jacobiBlueprint.match("\\phi^{(\\alpha, \\beta)}_{n} ( x )"));
    }
}
