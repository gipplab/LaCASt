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

import java.util.Map;

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

        assertTrue(blueprint.match("P^{(a,b)}_{n} ( x \\cdot (x^2 + y) )"));
        Map<String, String> groups = blueprint.getStringMatches();
        assertEquals("a", groups.get("par1"));
        assertEquals("b", groups.get("par2"));
        assertEquals("n", groups.get("par3"));
        assertEquals("x \\cdot (x^2 + y)", groups.get("var1"));
    }

    @Test
    public void spacingMatchTest() throws ParseException {
        MatchablePomTaggedExpression blueprint =
                new MatchablePomTaggedExpression(mlp, "( var1 )", "(p|v)ar\\d");

        assertTrue(blueprint.match("( x y )"));
        Map<String, String> groups = blueprint.getStringMatches();
        assertEquals("x y", groups.get("var1"));
    }

    @Test
    public void captureIntegrityTest() throws ParseException {
        MatchablePomTaggedExpression blueprint =
                new MatchablePomTaggedExpression(mlp, "( var1 + 1 )^{var1}", "(p|v)ar\\d");

        assertFalse(
                blueprint.match("( x + 1 )^{y}"),
                "Should not match because capture group var1 cannot be both x and y"
        );

        assertTrue(
                blueprint.match("( x + 1 )^{x}")
        );

        Map<String, String> groups = blueprint.getStringMatches();
        assertEquals("x", groups.get("var1"));

        assertFalse(
                blueprint.match("( x + 1 )^{y}", false),
                "Should not match because capture group var1 cannot be both x and y"
        );

        assertTrue(
                blueprint.match("( x + 1 )^{x}", false)
        );
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

    @Test
    public void followingTokensTest() throws ParseException {
        MatchablePomTaggedExpression blueprint =
                new MatchablePomTaggedExpression(mlp, "var1^{(var2, var3)}_{var4} (var5)", "(p|v)ar\\d");

        String test = "P_n^{(\\alpha,\\beta)}(\\cos \\theta) = n^{-\\frac{1}{2}}k(\\theta)\\cos (N\\theta + \\gamma) + O \\left (n^{-\\frac{3}{2}} \\right )";
        PrintablePomTaggedExpression ppte = mlp.parse(test);
        assertTrue(blueprint.matchWithinPlace(ppte));
        Map<String, String> matches = blueprint.getStringMatches();
        assertEquals("P", matches.get("var1"));
        assertEquals("\\alpha", matches.get("var2"));
        assertEquals("\\beta", matches.get("var3"));
        assertEquals("n", matches.get("var4"));
        assertEquals("\\cos \\theta", matches.get("var5"));
    }

    @Test
    public void withinPlaceTest() throws ParseException {
        MatchablePomTaggedExpression blueprint =
                new MatchablePomTaggedExpression(mlp, "var1^{(var2, var3)}_{var4} (var5)", "(p|v)ar\\d");

        String test = "\\frac{1}{2} P_n^{(\\alpha,\\beta)}(\\cos \\theta) = n^{-\\frac{1}{2}}";
        PrintablePomTaggedExpression ppte = mlp.parse(test);
        assertTrue(blueprint.matchWithinPlace(ppte));
        Map<String, String> matches = blueprint.getStringMatches();
        assertEquals("P", matches.get("var1"));
        assertEquals("\\alpha", matches.get("var2"));
        assertEquals("\\beta", matches.get("var3"));
        assertEquals("n", matches.get("var4"));
        assertEquals("\\cos \\theta", matches.get("var5"));
    }

    @Test
    public void noMatchEqualTest() throws ParseException {
        MatchablePomTaggedExpression blueprint =
                new MatchablePomTaggedExpression(mlp, "var1 = var2 = 0", "var\\d+");

        String test = "P_{n}^{(\\alpha, \\beta)}(x)";
        PrintablePomTaggedExpression ppte = mlp.parse(test);
        assertFalse(blueprint.matchWithinPlace(ppte));
    }

    @Test
    public void noMatchEqualTest2() throws ParseException {
        MatchablePomTaggedExpression blueprint =
                new MatchablePomTaggedExpression(mlp, "var1 = var2 = 0", "var\\d+");

        String test = "(1 - x)^{\\alpha}(1 + x)^{\\beta}";
        PrintablePomTaggedExpression ppte = mlp.parse(test);
        assertFalse(blueprint.matchWithinPlace(ppte));
    }

    @Test
    public void wikiParseTest() throws ParseException {
        MatchablePomTaggedExpression blueprint =
                new MatchablePomTaggedExpression(mlp, "var3 + var2 + var1", "var\\d+");

        String test = "(1 - x)^{\\alpha}(1 + x)^{\\beta}";
        PrintablePomTaggedExpression ppte = mlp.parse(test);
        assertFalse(blueprint.matchWithinPlace(ppte));
    }

    @Test
    public void inlineBracketTest() throws ParseException {
        MatchablePomTaggedExpression blueprint =
                new MatchablePomTaggedExpression(mlp, "1-(var1)^x", "var\\d+");

        String test = "1-(f(x))^x";
        PrintablePomTaggedExpression ppte = mlp.parse(test);
        assertTrue(blueprint.matchWithinPlace(ppte));

        Map<String, String> matches = blueprint.getStringMatches();
        assertEquals("f (x)", matches.get("var1"));
    }

    @Test
    public void inlineHypergeometricTest() throws ParseException {
        MatchablePomTaggedExpression blueprint =
                new MatchablePomTaggedExpression(mlp, "{}_{var1}F_{var2} (var3,var4;var5;var6)", "var\\d+");

        String test = "P_n^{(\\alpha,\\beta)}(z)=\\frac{(\\alpha+1)_n}{n!}\\,{}_2F_1\\left(-n,1+\\alpha+\\beta+n;\\alpha+1;\\tfrac{1}{2}(1-z)\\right)";
        PrintablePomTaggedExpression ppte = mlp.parse(test);
        assertTrue(blueprint.matchWithinPlace(ppte));

        Map<String, String> matches = blueprint.getStringMatches();
        assertEquals("2", matches.get("var1"));
        assertEquals("1", matches.get("var2"));
        assertEquals("- n", matches.get("var3"));
        assertEquals("1 + \\alpha + \\beta + n", matches.get("var4"));
        assertEquals("\\alpha + 1", matches.get("var5"));
        assertEquals("\\tfrac{1}{2} (1 - z)", matches.get("var6"));
    }

    @Test
    public void partialHitTest() throws ParseException {
        MatchablePomTaggedExpression blueprint =
                new MatchablePomTaggedExpression(mlp, "var1^{(var2, var3)}_{var4} (var5)", "(p|v)ar\\d");

        String test = "P_n^{(\\alpha,\\beta)}";
        PrintablePomTaggedExpression ppte = mlp.parse(test);
        assertFalse(blueprint.match(ppte));
        Map<String, String> matches = blueprint.getStringMatches();
        assertEquals("P", matches.get("var1"));
        assertEquals("\\alpha", matches.get("var2"));
        assertEquals("\\beta", matches.get("var3"));
        assertEquals("n", matches.get("var4"));
        assertNull(matches.get("var5"));
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
