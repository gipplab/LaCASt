package gov.nist.drmf.interpreter.mlp.extensions;

import gov.nist.drmf.interpreter.common.exceptions.NotMatchableException;
import gov.nist.drmf.interpreter.common.meta.AssumeMLPAvailability;
import gov.nist.drmf.interpreter.mlp.MLPWrapper;
import mlp.MLP;
import mlp.ParseException;
import mlp.PomTaggedExpression;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * @author Andre Greiner-Petter
 */
@AssumeMLPAvailability
public class MatchablePomTaggedExpressionTests {
    private static final Logger LOG = LogManager.getLogger(MatchablePomTaggedExpressionTests.class.getName());

    private static MLPWrapper mlp;
    private static MatchablePomTaggedExpression jacobiBlueprint;

    @BeforeAll
    public static void setup() throws ParseException {
        mlp = new MLPWrapper();
        jacobiBlueprint = new MatchablePomTaggedExpression(mlp, "P^{(par1, par2)}_{par3} (var1)", "[pv]ar\\d");
    }

    @Test
    public void simpleStringConstructorTest() throws ParseException {
        MatchablePomTaggedExpression blueprint = new MatchablePomTaggedExpression(mlp, "a+WILD+c", "WILD");
        assertTrue(blueprint.getMatches().isEmpty());
    }

    @Test
    public void simpleConstructorTest() throws ParseException {
        MatchablePomTaggedExpression blueprint = new MatchablePomTaggedExpression("a+WILD+c", "WILD");
        assertTrue(blueprint.getMatches().isEmpty());
    }

    @Test
    public void simplePTEConstructorTest() throws ParseException {
        PomTaggedExpression pte = mlp.simpleParse("a+WILD+c");
        MatchablePomTaggedExpression blueprint = new MatchablePomTaggedExpression(pte, "WILD");
        assertTrue(blueprint.getMatches().isEmpty());
    }

    @Test
    public void illegalWildCardTest() throws ParseException {
        PomTaggedExpression pte = mlp.simpleParse("a^b");
        assertThrows(NotMatchableException.class, () -> new MatchablePomTaggedExpression(pte, "\\^"));
    }

    @Test
    public void illegalConsecutiveWildCardTest() throws ParseException {
        PomTaggedExpression pte = mlp.simpleParse("a+b b+c");
        assertThrows(NotMatchableException.class, () -> new MatchablePomTaggedExpression(pte, "b"));
    }

    @Test
    public void linearMatchTest() throws ParseException {
        MatchablePomTaggedExpression blueprint = new MatchablePomTaggedExpression(mlp, "a+WILD+c", "WILD");
        checkMatch( blueprint, "WILD", "a+b+c", "b" );
    }

    @Test
    public void linearTwiceMatchTest() throws ParseException {
        LOG.debug("start init");
        MatchablePomTaggedExpression blueprint = new MatchablePomTaggedExpression(mlp, "a+WILD+c", "WILD");
        LOG.debug("intermed");
        PrintablePomTaggedExpression ppte = mlp.parse("a+b+c");
        LOG.debug("finish init");

        checkMatch( blueprint, "WILD", ppte, "b" );
        checkMatch( blueprint, "WILD", ppte, "b" );
    }

    @Test
    public void patternMatchTest() throws ParseException {
        MatchablePomTaggedExpression blueprint = new MatchablePomTaggedExpression(mlp, "par1+par2^x", "par\\d");
        checkMatch( blueprint, "par1", "a+b^x", "a" );
        checkMatch( blueprint, "par2", "a+b^x", "b" );
    }

    @Test
    public void complexPatternMatchTest() throws ParseException {
        MatchablePomTaggedExpression blueprint =
                new MatchablePomTaggedExpression(mlp, "P^{(par1, par2)}_{par3} (var1)", "(p|v)ar\\d");

        checkMatch(blueprint, "var1", "P^{(a,b)}_n (x)", "x");
        checkMatch(blueprint, "var1", "P^{(a,b)}_{n} ( x+\\frac{1}{x} )", "x+\\frac{1}{x}");
        checkMatch(blueprint, "var1", "P^{(a,b)}_{n} ( x \\cdot (x^2 + y) )", "x\\cdot(x^2+y)");
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

    private void checkMatch( MatchablePomTaggedExpression test, String wildCard, String expression, String result )
            throws ParseException {
        PrintablePomTaggedExpression ppte = mlp.parse(expression);
        checkMatch(test, wildCard, ppte, result);
    }

    private void checkMatch(
            MatchablePomTaggedExpression test,
            String wildCard,
            PrintablePomTaggedExpression ppte,
            String result
    ) {
        assertTrue( test.match(ppte) );
        assertTrue( test.getMatches().containsKey(wildCard) );
        assertEquals( result, test.getStringMatches().get(wildCard) );
    }
}
