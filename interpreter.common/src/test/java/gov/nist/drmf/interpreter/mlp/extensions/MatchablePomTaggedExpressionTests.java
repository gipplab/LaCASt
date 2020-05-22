package gov.nist.drmf.interpreter.mlp.extensions;

import gov.nist.drmf.interpreter.common.exceptions.NotMatchableException;
import gov.nist.drmf.interpreter.common.meta.AssumeMLPAvailability;
import gov.nist.drmf.interpreter.common.meta.DLMF;
import gov.nist.drmf.interpreter.mlp.MLPWrapper;
import gov.nist.drmf.interpreter.mlp.SemanticMLPWrapper;
import mlp.ParseException;
import mlp.PomTaggedExpression;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.util.StringUtils;

import java.util.*;

import static gov.nist.drmf.interpreter.common.tests.IgnoresAllWhitespacesMatcher.ignoresAllWhitespaces;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

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
        mlp = SemanticMLPWrapper.getStandardInstance();
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

        assertTrue(blueprint.match("P^{(a,b)}_{n} ( x \\cdot (x^2 + y) )", MatcherConfig.getExactMatchConfig()));
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
                blueprint.match("( x + 1 )^{y}"),
                "Should not match because capture group var1 cannot be both x and y"
        );

        assertTrue(
                blueprint.match("( x + 1 )^{x}")
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
        MatcherConfig config = new MatcherConfig(false, true, false);
        assertTrue(blueprint.match(ppte, config));

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
        MatcherConfig config = MatcherConfig.getInPlaceMatchConfig();
        assertTrue(blueprint.match(ppte, config));

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
        assertFalse(blueprint.match(ppte));
        assertFalse(blueprint.match(ppte, new MatcherConfig(true, true, true)));
    }

    @Test
    public void noMatchEqualTest2() throws ParseException {
        MatchablePomTaggedExpression blueprint =
                new MatchablePomTaggedExpression(mlp, "var1 = var2 = 0", "var\\d+");

        String test = "(1 - x)^{\\alpha}(1 + x)^{\\beta}";
        PrintablePomTaggedExpression ppte = mlp.parse(test);
        assertFalse(blueprint.match(ppte));
        assertFalse(blueprint.match(ppte, new MatcherConfig(true, true, true)));
    }

    @Test
    public void wikiParseTest() throws ParseException {
        MatchablePomTaggedExpression blueprint =
                new MatchablePomTaggedExpression(mlp, "var3 + var2 + var1", "var\\d+");

        String test = "(1 - x)^{\\alpha}(1 + x)^{\\beta}";
        PrintablePomTaggedExpression ppte = mlp.parse(test);
        assertFalse(blueprint.match(ppte));
        assertFalse(blueprint.match(ppte, new MatcherConfig(true, true, true)));
    }

    @Test
    public void inlineBracketTest() throws ParseException {
        MatchablePomTaggedExpression blueprint =
                new MatchablePomTaggedExpression(mlp, "1-(var1)^x", "var\\d+");

        String test = "1-(f(x))^x";
        PrintablePomTaggedExpression ppte = mlp.parse(test);
        assertTrue(blueprint.match(ppte));
        assertTrue(blueprint.match(ppte, new MatcherConfig(false)));

        Map<String, String> matches = blueprint.getStringMatches();
        assertEquals("f (x)", matches.get("var1"));
    }

    @Test
    public void bracketLogicTest() throws ParseException {
        MatchablePomTaggedExpression blueprint =
                new MatchablePomTaggedExpression(mlp, "1-(var1)^x", "var\\d+");

        String test = "1-(f(x)^x";
        PrintablePomTaggedExpression ppte = mlp.parse(test);

        assertFalse(blueprint.match(ppte, new MatcherConfig(false)));

        ppte = mlp.parse(test);
        assertTrue(blueprint.match(ppte, new MatcherConfig(true)));
    }

    @Test
    public void configTest() throws ParseException {
        MatchablePomTaggedExpression blueprint =
                new MatchablePomTaggedExpression(mlp, "1 + var1 + 2", "var\\d+");

        // positive config tests
        assertTrue(
                blueprint.match("1 + x + 2", MatcherConfig.getExactMatchConfig()),
                "Exact match should return true"
        );
        assertTrue(
                blueprint.match("0 + 1 + x + 2", new MatcherConfig(true, false, false)),
                "Allow leading tokens but no following tokens does not work"
        );
        assertTrue(
                blueprint.match("1 + x + 2 + 3", new MatcherConfig(false, true, false)),
                "Not allow leading tokens but following tokens does not work"
        );
        assertTrue(
                blueprint.match("0 + 1 + x + 2 + 3", new MatcherConfig(true, true, false)),
                "Allow both, leading and following tokens, does not work"
        );

        // negative config tests
        assertFalse(
                blueprint.match("1 + x + 2 + 3", new MatcherConfig(true, false, false)),
                "Negative test for leading tokens allowed but no ending tokens failed"
        );
        assertFalse(
                blueprint.match("0 + 1 + x + 2", new MatcherConfig(false, true, false)),
                "Negative test for leading tokens not allowed but ending tokens are, failed"
        );
        assertFalse(
                blueprint.match("0 + 1 + x + 2 + 3", new MatcherConfig(false, false, false)),
                "Negative test for no in-place test failed."
        );
    }

    @Test
    public void inlineHypergeometricTest() throws ParseException {
        MatchablePomTaggedExpression blueprint =
                new MatchablePomTaggedExpression(mlp, "{}_{var1}F_{var2} (var3,var4;var5;var6)", "var\\d+");

        String test = "P_n^{(\\alpha,\\beta)}(z)=\\frac{(\\alpha+1)_n}{n!}\\,{}_2F_1\\left(-n,1+\\alpha+\\beta+n;\\alpha+1;\\tfrac{1}{2}(1-z)\\right)";
        PrintablePomTaggedExpression ppte = mlp.parse(test);
        assertTrue(blueprint.match(ppte, MatcherConfig.getInPlaceMatchConfig()));

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

    @Test
    public void deepInsideGammaTest() throws ParseException {
        MatchablePomTaggedExpression blueprint =
                new MatchablePomTaggedExpression(mlp, "\\Gamma(var1)", "(p|v)ar\\d");
        String test = "P_n^{(\\alpha,\\beta)} (z) = \\frac{\\Gamma (\\alpha+n+1)}{n!\\,\\Gamma (\\alpha+\\beta+n+1)} " +
                "\\sum_{m=0}^n {n\\choose m} \\frac{\\Gamma (\\alpha + \\beta + n + m + 1)}{\\Gamma (\\alpha + m + 1)} " +
                "\\left(\\frac{z-1}{2}\\right)^m";

        PrintablePomTaggedExpression ppte = mlp.parse(test);
        assertTrue(blueprint.match(ppte, MatcherConfig.getInPlaceMatchConfig()));
        Map<String, String> matches = blueprint.getStringMatches();
        assertEquals("\\alpha + n + 1", matches.get("var1"));
    }

    @Test
    public void multiCycleMatchingOnSingleBlueprintTest() throws ParseException {
        MatchablePomTaggedExpression blueprint =
                new MatchablePomTaggedExpression(mlp, "var1^{(var2, var3)}_{var4} (var5)", "(p|v)ar\\d");

        for ( int i = 1; i < 11; i++ ) {
            String test = "P_n^{(\\alpha,\\beta)}(x)";
            PrintablePomTaggedExpression ppte = mlp.parse(test);
            testAgain(i, blueprint, ppte);
        }
    }

    @Test
    public void multiCycleMatchingOnSingleTestExpressionTest() throws ParseException {
        MatchablePomTaggedExpression blueprint =
                new MatchablePomTaggedExpression(mlp, "var1^{(var2, var3)}_{var4} (var5)", "(p|v)ar\\d");

        String test = "P_n^{(\\alpha,\\beta)}(x)";
        PrintablePomTaggedExpression ppte = mlp.parse(test);

        for ( int i = 1; i < 11; i++ )
            testAgain(i, blueprint, ppte);
    }

    private void testAgain(int cycle, MatchablePomTaggedExpression blueprint, PrintablePomTaggedExpression ppte) {
        assertTrue(blueprint.match(ppte), "Failed in cycle: " + cycle);
        Map<String, String> matches = blueprint.getStringMatches();
        assertEquals("P", matches.get("var1"));
        assertEquals("\\alpha", matches.get("var2"));
        assertEquals("\\beta", matches.get("var3"));
        assertEquals("n", matches.get("var4"));
        assertEquals("x", matches.get("var5"));
    }

    @Test
    public void pomMatcherFindSimpleTest() throws ParseException {
        MatchablePomTaggedExpression blueprint =
                new MatchablePomTaggedExpression(mlp, "\\Gamma(var1)", "(p|v)ar\\d");

        String test = "x + \\Gamma(x)";
        PomMatcher pomMatcher = blueprint.matcher( test );
        assertFalse(pomMatcher.match());
        assertTrue(pomMatcher.find());

        Map<String, String> groups = pomMatcher.groups();
        assertEquals(1, groups.size());
        assertEquals("x", groups.get("var1"));

        // there is only one hit in the expression, so any following find should
        // return false
        for ( int i = 2; i < 4; i++ )
            assertFalse(pomMatcher.find(), "Suddenly returned true again in find() invoke number " + i);
    }

    @Test
    public void pomMatcherFindMultiHitsTest() throws ParseException {
        MatchablePomTaggedExpression blueprint =
                new MatchablePomTaggedExpression(mlp, "\\Gamma(var1)", "(p|v)ar\\d");

        String test = "x + \\Gamma(x) - \\Gamma(y)";
        PomMatcher pomMatcher = blueprint.matcher( test );
        assertFalse(pomMatcher.match());

        assertTrue(pomMatcher.find());
        Map<String, String> groups = pomMatcher.groups();
        assertEquals(1, groups.size());
        assertEquals("x", groups.get("var1"));

        assertTrue(pomMatcher.find());
        groups = pomMatcher.groups();
        assertEquals(1, groups.size());
        assertEquals("y", groups.get("var1"));

        assertFalse(pomMatcher.find());
    }

    @Test
    public void pomMatcherFindPseudoNestedHitsTest() throws ParseException {
        MatchablePomTaggedExpression blueprint =
                new MatchablePomTaggedExpression(mlp, "\\Gamma(var1)", "(p|v)ar\\d");

        String test = "\\Gamma(\\Gamma(\\Gamma(x)))";
        PomMatcher pomMatcher = blueprint.matcher( test );
        assertTrue(pomMatcher.match());
        Map<String, String> groups = pomMatcher.groups();
        assertEquals(1, groups.size());
        assertThat("\\Gamma(\\Gamma(x))", ignoresAllWhitespaces(groups.get("var1")));

        assertTrue(pomMatcher.find());
        assertEquals(1, groups.size());
        assertThat("\\Gamma(\\Gamma(x))", ignoresAllWhitespaces(groups.get("var1")));

        assertTrue(pomMatcher.find());
        groups = pomMatcher.groups();
        assertEquals(1, groups.size());
        assertThat("\\Gamma(x)", ignoresAllWhitespaces(groups.get("var1")));

        assertTrue(pomMatcher.find());
        groups = pomMatcher.groups();
        assertEquals(1, groups.size());
        assertEquals("x", groups.get("var1"));

        assertFalse(pomMatcher.find());
    }

    @Test
    public void pomMatcherFindRealNestedHitsTest() throws ParseException {
        MatchablePomTaggedExpression blueprint =
                new MatchablePomTaggedExpression(mlp, "\\Gamma{var1}", "(p|v)ar\\d");

        String test = "\\Gamma{\\Gamma{\\Gamma{x}}}";
        PomMatcher pomMatcher = blueprint.matcher( test );
        assertTrue(pomMatcher.match());
        Map<String, String> groups = pomMatcher.groups();
        assertEquals(1, groups.size());
        assertThat("\\Gamma{\\Gamma{x}}", ignoresAllWhitespaces(groups.get("var1")));

        assertTrue(pomMatcher.find());
        assertEquals(1, groups.size());
        assertThat("\\Gamma{\\Gamma{x}}", ignoresAllWhitespaces(groups.get("var1")));

        assertTrue(pomMatcher.find());
        groups = pomMatcher.groups();
        assertEquals(1, groups.size());
        assertThat("\\Gamma{x}", ignoresAllWhitespaces(groups.get("var1")));

        assertTrue(pomMatcher.find());
        groups = pomMatcher.groups();
        assertEquals(1, groups.size());
        assertEquals("x", groups.get("var1"));

        assertFalse(pomMatcher.find());
    }

    @Test
    public void pomMatcherFindDoubleArgTest() throws ParseException {
        MatchablePomTaggedExpression blueprint =
                new MatchablePomTaggedExpression(mlp, "\\sin(var1)", "(p|v)ar\\d");

        String test = "\\sin (x-z) + \\sin (x + y^2)}";
        PomMatcher pomMatcher = blueprint.matcher( test );

        assertTrue(pomMatcher.find());
        Map<String, String> groups = pomMatcher.groups();
        assertEquals("x - z", groups.get("var1"));

        assertTrue(pomMatcher.find());
        groups = pomMatcher.groups();
        assertEquals("x + y^2", groups.get("var1"));

        assertFalse(pomMatcher.find());
    }

    @Test
    public void pomMatcherFindFractionTest() throws ParseException {
        MatchablePomTaggedExpression blueprint =
                new MatchablePomTaggedExpression(mlp, "\\sin(var1)", "(p|v)ar\\d");

        String test = "\\frac{\\sin (x) + z}{a + \\sin (y) + b}";
        PomMatcher pomMatcher = blueprint.matcher( test );

        assertTrue(pomMatcher.find());
        Map<String, String> groups = pomMatcher.groups();
        assertEquals("x", groups.get("var1"));

        assertTrue(pomMatcher.find());
        groups = pomMatcher.groups();
        assertEquals("y", groups.get("var1"));

        assertFalse(pomMatcher.find());
    }

    @Test
    public void pomMatcherFindMultiFractionTest() throws ParseException {
        MatchablePomTaggedExpression blueprint =
                new MatchablePomTaggedExpression(mlp, "\\sin(var1)", "(p|v)ar\\d");

        String test = "\\frac{\\sin (x)}{\\sin (y)} - \\frac{\\sin (z)}{\\sin (q^2)}";
        PomMatcher pomMatcher = blueprint.matcher( test );
        Set<String> registeredHits = new HashSet<>();
        while ( pomMatcher.find() ) {
            // store all hits
            Map<String, String> groups = pomMatcher.groups();
            registeredHits.addAll( groups.values() );
        }

        // check if we found all:
        assertTrue( registeredHits.contains("x"), registeredHits.toString() );
        assertTrue( registeredHits.contains("y"), registeredHits.toString() );
        assertTrue( registeredHits.contains("z"), registeredHits.toString() );
        assertTrue( registeredHits.contains("q^2"), registeredHits.toString() );
        assertEquals( 4, registeredHits.size(), registeredHits.toString() );
    }

    @Test
    public void pomMatcherFindHardJacobiRealWorldExampleTest() throws ParseException {
        MatchablePomTaggedExpression blueprint =
                new MatchablePomTaggedExpression(mlp, "\\Gamma(var1)", "(p|v)ar\\d");

        String test = "P_n^{(\\alpha,\\beta)} (z) = \\frac{\\Gamma (\\alpha+n+1)}{n!\\,\\Gamma (\\alpha+\\beta+n+1)} " +
                "\\sum_{m=0}^n {n\\choose m} \\frac{\\Gamma (\\alpha + \\beta + n + m + 1)}{\\Gamma (\\alpha + m + 1)} " +
                "\\left(\\frac{z-1}{2}\\right)^m";

        // lets not care about the order of the hits, so lets store them first and check later.
        Set<String> registeredHits = new HashSet<>();

        PomMatcher pomMatcher = blueprint.matcher(test);
        while ( pomMatcher.find() ) {
            // store all hits
            Map<String, String> groups = pomMatcher.groups();
            registeredHits.addAll( groups.values() );
        }

        // check if we found all:
        assertTrue( registeredHits.contains("\\alpha + n + 1"), registeredHits.toString() );
        assertTrue( registeredHits.contains("\\alpha + \\beta + n + 1"), registeredHits.toString() );
        assertTrue( registeredHits.contains("\\alpha + \\beta + n + m + 1"), registeredHits.toString() );
        assertTrue( registeredHits.contains("\\alpha + m + 1"), registeredHits.toString() );
        assertEquals( 4, registeredHits.size(), registeredHits.toString() );
    }

    @Test
    public void pomMatcherFindStartingWildcardTest() throws ParseException {
        MatchablePomTaggedExpression blueprint =
                new MatchablePomTaggedExpression(mlp, "var1(var2)", "(p|v)ar\\d");

        String test = "f(x) + g(y)";
        PomMatcher pomMatcher = blueprint.matcher(test);

        assertTrue(pomMatcher.find());
        Map<String, String> groups = pomMatcher.groups();
        assertEquals("f", groups.get("var1"));
        assertEquals("x", groups.get("var2"));
        assertEquals(2, groups.values().size());

        assertTrue(pomMatcher.find());
        groups = pomMatcher.groups();
        assertEquals("g", groups.get("var1"));
        assertEquals("y", groups.get("var2"));
        assertEquals(2, groups.values().size());

        assertFalse(pomMatcher.find());
    }

    @Test
    public void pomMatcherFindStartingWildcardBacklogTest() throws ParseException {
        MatchablePomTaggedExpression blueprint =
                new MatchablePomTaggedExpression(mlp, "var1(var2)", "(p|v)ar\\d");

        String test = "F_1(x)";
        PomMatcher pomMatcher = blueprint.matcher(test);

        assertTrue(pomMatcher.find());
        Map<String, String> groups = pomMatcher.groups();
        assertEquals("F_1", groups.get("var1"));
        assertEquals("x", groups.get("var2"));
        assertEquals(2, groups.values().size());

        assertFalse(pomMatcher.find());
    }

    @Test
    public void pomMatcherFindIncalidMatchTest() throws ParseException {
        MatchablePomTaggedExpression blueprint =
                new MatchablePomTaggedExpression(mlp, "var1 + var2 = - ( - var2 - var1 )", "(p|v)ar\\d");

        String test = "x + y";
        PomMatcher pomMatcher = blueprint.matcher(test);

        assertFalse(pomMatcher.find());
    }

    @Test
    public void pomMatcherFindLargeNegativeTest() throws ParseException {
        MatchablePomTaggedExpression blueprint =
                new MatchablePomTaggedExpression(mlp, "P_{var5}^{({var2},{var1})}({var4})=\\frac{({var2}+1)_{var5}}{{var5}!} {}_2F_1 \\left(-{var5},1+{var2}+{var1}+{var5};{var2}+1; \\tfrac{1}{2}(1-{var4})\\right)", "(p|v)ar\\d");

        String test = "P_{n}^{(\\alpha, \\beta)}(x)";
        PomMatcher pomMatcher = blueprint.matcher(test);
        assertFalse(pomMatcher.find());
    }

    @Test
    public void pomMatcherFindVeryHardRealWorldTest() throws ParseException {
        MatchablePomTaggedExpression blueprint =
                new MatchablePomTaggedExpression(mlp, "var1(var2)", "(p|v)ar\\d");

        String test = "P_n^{(\\alpha,\\beta)} (z) = \\frac{\\Gamma (\\alpha+n+1)}{n!\\,\\Gamma (\\alpha+\\beta+n+1)} " +
                "\\sum_{m=0}^n {n\\choose m} \\frac{\\Gamma (\\alpha + \\beta + n + m + 1)}{\\Gamma (\\alpha + m + 1)} " +
                "\\left(\\frac{z-1}{2}\\right)^m";

        // lets not care about the order of the hits, so lets store them first and check later.
        Map<String, List<String>> allGroups = new HashMap<>();

        PomMatcher pomMatcher = blueprint.matcher(test);
        while ( pomMatcher.find() ) {
            // store all hits
            Map<String, String> groups = pomMatcher.groups();
            String key = groups.get("var1");
            allGroups.computeIfAbsent( key, k -> new LinkedList<>() ).add(groups.get("var2"));
        }

        checkComplexHitStructure(allGroups, "P_n^{(\\alpha,\\beta)}", "z");
        checkComplexHitStructure(allGroups, "\\Gamma",
                "\\alpha + n + 1",
                "\\alpha + \\beta + n + 1",
                "\\alpha + \\beta + n + m + 1",
                "\\alpha + m + 1"
        );

         /*
         well you won't believe it, the algorithm found the following "hit" also:
         var1 = \frac{\Gamma (\alpha + \beta + n + m + 1)}{\Gamma (\alpha + m + 1)}
         var2 = \left(\frac{z-1}{2}\right)
         which is kind of correct... but I'm not sure if we want "not allow" that.
         Not allow could include two reasons...
         1) when the pattern is f(x), should we hit f(x)^2 or not? I think we should,
         we don't know if its part of a function or not. In case of f(x)^2, (x)^2 is not a single object.
         2) we may want to fine control hits via feature sets... eg var1 (but var1 should be a function or letter) which
         would not allow "frac" as a hit. But that's a different story for later.
         */
//        assertEquals(2, allGroups.keySet().size());

        // only these two keys exists, nothing else
        assertEquals(3, allGroups.keySet().size());
    }

    private void checkComplexHitStructure( Map<String, List<String>> groups, String key, String... values ) {
        // first, check the key exists
        assertTrue( groups.containsKey(key), groups.toString() );
        // second, check all required values exist
        for ( String v : values ) {
            assertTrue( groups.get(key).contains(v), groups.get(key).toString() );
        }
        // third, check if no more values than the required values exists
        assertEquals( values.length, groups.get(key).size(), groups.get(key).toString() );
    }

    @Test
    public void pomMatcherReplaceTest() throws ParseException {
        MatchablePomTaggedExpression blueprint =
                new MatchablePomTaggedExpression(mlp, "y");

        String originalInput = "x + y + z";
        PrintablePomTaggedExpression origP = mlp.parse(originalInput);
        PomMatcher pomMatcher = blueprint.matcher(origP);
        assertTrue(pomMatcher.find());

        PrintablePomTaggedExpression ppte = mlp.parse("Q");
        LinkedList<PrintablePomTaggedExpression> tmp = new LinkedList<>();
        tmp.add(ppte);

        PrintablePomTaggedExpression p = pomMatcher.replacePreviousHit(tmp);
        assertEquals("x + Q + z", p.getTexString());
        assertEquals(originalInput, origP.getTexString());
    }

    @Test
    public void pomMatcherReplaceAllTest() throws ParseException {
        MatchablePomTaggedExpression blueprint =
                new MatchablePomTaggedExpression(mlp, "q");

        String test = "x + q + y + q + z";
        PomMatcher pomMatcher = blueprint.matcher(test);

        PrintablePomTaggedExpression ppte = mlp.parse("Q");
        LinkedList<PrintablePomTaggedExpression> tmp = new LinkedList<>();
        tmp.add(ppte);

        PrintablePomTaggedExpression result = pomMatcher.replaceAll(tmp);
        assertEquals("x + Q + y + Q + z", result.getTexString());
    }

    @Test
    public void pomMatcherReplaceAllNestedTest() throws ParseException {
        MatchablePomTaggedExpression blueprint =
                new MatchablePomTaggedExpression(mlp, "q");

        String test = "x + q + y + q + z";
        PomMatcher pomMatcher = blueprint.matcher(test);

        PrintablePomTaggedExpression ppte = mlp.parse("Q");
        LinkedList<PrintablePomTaggedExpression> tmp = new LinkedList<>();
        tmp.add(ppte);

        PrintablePomTaggedExpression result = pomMatcher.replaceAll(tmp);
        assertEquals("x + Q + y + Q + z", result.getTexString());
    }

    @Test
    public void pomMatcherReplaceAllWithWildcardTest() throws ParseException {
        MatchablePomTaggedExpression blueprint =
                new MatchablePomTaggedExpression(mlp, "1 + var1 + 2", "var1");

        String test = "f(1 + x + 2) + g(1 + y + 2)";
        PomMatcher pomMatcher = blueprint.matcher(test);

        PrintablePomTaggedExpression result = pomMatcher.replaceAll( "var1^2" );
        assertEquals("f (x^2) + g (y^2)", result.getTexString());
    }

    @Test
    public void pomMatcherReplaceAllRealWorldTest() throws ParseException {
        MatchablePomTaggedExpression blueprint =
                new MatchablePomTaggedExpression(mlp, "P^{(var1, var2)}_{var3} (var4)", "(p|v)ar\\d");

        String test = "P_n^{(\\alpha,\\beta)}(\\cos \\theta)";
        PomMatcher matcher = blueprint.matcher(test);

        PrintablePomTaggedExpression result = matcher.replaceAll( "\\JacobipolyP{var1}{var2}{var3}@{var4}" );
        assertEquals("\\JacobipolyP{\\alpha}{\\beta}{n}@{\\cos \\theta}", result.getTexString());
    }

    @Test
    public void pomMatcherReplaceAllPochhammerTest() throws ParseException {
        MatchablePomTaggedExpression pochhammerBlueprint = new MatchablePomTaggedExpression(mlp, "(var1)_{var2}", "var\\d");
        PomMatcher matcher = pochhammerBlueprint.matcher("\\frac{(\\alpha+1)_n}{n!}");
        PrintablePomTaggedExpression result = matcher.replaceAll( "\\Pochhammer{var1}{var2}" );
        assertEquals("\\frac{\\Pochhammer{\\alpha + 1}{n}}{n!}", result.getTexString());
    }

    @Test
    @DLMF("15.9.1")
    public void pomMatcherReplaceJacobiArticleTest() throws ParseException {
        // contains the jacobi polynomial, the pochhammer symbol and the hypergeometric function
        String test = "P_n^{(\\alpha,\\beta)}(z)=\\frac{(\\alpha+1)_n}{n!}\\,{}_2F_1\\left(-n,1+\\alpha+\\beta+n;\\alpha+1;\\tfrac{1}{2}(1-z)\\right)";
        String semantic = "\\JacobipolyP{\\alpha}{\\beta}{n}@{z} = \\frac{\\Pochhammersym{\\alpha + 1}{n}}{n!} \\genhyperF{2}{1}@{- n, 1 + \\alpha + \\beta + n}{\\alpha + 1}{\\tfrac{1}{2} (1 - z)}";

        MatchablePomTaggedExpression jacobiBlueprint = new MatchablePomTaggedExpression(mlp, "P^{(var1, var2)}_{var3} (var4)", "var\\d");
        MatchablePomTaggedExpression pochhammerBlueprint = new MatchablePomTaggedExpression(mlp, "(var1)_{var2}", "var\\d");
        MatchablePomTaggedExpression hypergeometricBlueprint = new MatchablePomTaggedExpression(mlp, "{}_{var1}F_{var2} (var3, var4; var5; var6)", "var\\d");

        // replace all jacobis
        PomMatcher jacobiMatcher = jacobiBlueprint.matcher(test);
        String semanticJacobi = "\\JacobipolyP{var1}{var2}{var3}@{var4}";
        PrintablePomTaggedExpression replacedJacobi = jacobiMatcher.replaceAll(semanticJacobi);

        // replace all pochhammers
        PomMatcher pochhammerMatcher = pochhammerBlueprint.matcher(replacedJacobi);
        String semanticPochhammer = "\\Pochhammersym{var1}{var2}";
        PrintablePomTaggedExpression replacedJacobiPoch = pochhammerMatcher.replaceAll(semanticPochhammer);

        // replace all hypergeometric functions
        PomMatcher hypergeometricMatcher = hypergeometricBlueprint.matcher(replacedJacobiPoch);
        String semanticHypergeometric = "\\genhyperF{var1}{var2}@{var3, var4}{var5}{var6}";
        PrintablePomTaggedExpression replacedAll = hypergeometricMatcher.replaceAll(semanticHypergeometric);

        assertEquals(semantic, replacedAll.getTexString());
    }
}
