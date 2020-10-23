package gov.nist.drmf.interpreter.mlp.moi;

import gov.nist.drmf.interpreter.common.meta.AssumeMLPAvailability;
import mlp.ParseException;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Andre Greiner-Petter
 */
@AssumeMLPAvailability
public class MathematicalObjectOfInterestTests {

    @Test
    public void simpleMOITest() throws ParseException {
        MathematicalObjectOfInterest moi = new MathematicalObjectOfInterest("a + b");
        assertEquals("var0 + var1", moi.getPattern());

        assertTrue(moi.getIdentifiers().contains("a"));
        assertTrue(moi.getIdentifiers().contains("b"));
        assertEquals(2, moi.getIdentifiers().size());

        assertTrue(moi.getPotentialPrimaryIdentifierWildcardMapping().containsValue("a"));
        assertTrue(moi.getPotentialPrimaryIdentifierWildcardMapping().containsValue("b"));
        assertEquals(2, moi.getPotentialPrimaryIdentifierWildcardMapping().keySet().size());

        Map<String, String> wildCardMapping = moi.getWildcardIdentifierMapping();
        assertEquals("a", wildCardMapping.get("var0"));
        assertEquals("b", wildCardMapping.get("var1"));
        assertEquals(2, wildCardMapping.keySet().size());
    }

    @Test
    public void jacobiPolyTest() throws ParseException {
        MathematicalObjectOfInterest moi = new MathematicalObjectOfInterest("P_{n}^{(\\alpha,\\beta)} (x)");
        assertEquals("var0_{var1}^{(var2 , var3)} (var4)", moi.getPattern());

        Map<String, String> wildCardMapping = moi.getWildcardIdentifierMapping();
        assertTrue(wildCardMapping.containsValue("P"));
        assertTrue(wildCardMapping.containsValue("n"));
        assertTrue(wildCardMapping.containsValue("\\alpha"));
        assertTrue(wildCardMapping.containsValue("\\beta"));
        assertTrue(wildCardMapping.containsValue("x"));
        assertEquals(5, wildCardMapping.size());

        assertTrue(moi.getIdentifiers().contains("P"));
        assertTrue(moi.getIdentifiers().contains("n"));
        assertTrue(moi.getIdentifiers().contains("\\alpha"));
        assertTrue(moi.getIdentifiers().contains("\\beta"));
        assertTrue(moi.getIdentifiers().contains("x"));
        assertEquals(5, moi.getIdentifiers().size());

        Map<String, String> primaryIdentifier = moi.getPotentialPrimaryIdentifierWildcardMapping();
        assertTrue(primaryIdentifier.containsValue("P"));
        assertTrue(primaryIdentifier.containsValue("n"));
        assertTrue(primaryIdentifier.containsValue("\\alpha"));
        assertTrue(primaryIdentifier.containsValue("\\beta"));
        assertEquals(4, primaryIdentifier.size());
    }

    @Test
    public void moiNoMatchTest() throws ParseException {
        MathematicalObjectOfInterest jacobi = new MathematicalObjectOfInterest("P_{n}^{(\\alpha,\\beta)} (x)");
        MathematicalObjectOfInterest gamma = new MathematicalObjectOfInterest("\\Gamma(z)");

        assertNull(jacobi.match(gamma));
        assertNull(gamma.match(jacobi));
    }

    @Test
    public void moiSimpleMatchTest() throws ParseException {
        MathematicalObjectOfInterest jacobiX = new MathematicalObjectOfInterest("P_{n}^{(\\alpha,\\beta)} (x)");
        MathematicalObjectOfInterest jacobiZ = new MathematicalObjectOfInterest("P_{n}^{(\\alpha,\\beta)} (z)");

        DependencyPattern match = jacobiX.match(jacobiZ);
        assertNotNull(match);
        assertTrue(match.exactMatch());

        match = jacobiZ.match(jacobiX);
        assertNotNull(match);
        assertTrue(match.exactMatch());
    }

    @Test
    public void oneWayMatchTest() throws ParseException {
        MathematicalObjectOfInterest jacobiX = new MathematicalObjectOfInterest("P_{n}^{(\\alpha,\\beta)} (x)");
        MathematicalObjectOfInterest jacobiWithoutArg = new MathematicalObjectOfInterest("P_{n}^{(\\alpha,\\beta)}");

        DependencyPattern match = jacobiX.match(jacobiWithoutArg);
        assertNull(match);

        match = jacobiWithoutArg.match(jacobiX);
        assertNotNull(match);
        assertFalse(match.exactMatch());
    }

    @Test
    public void wikiJacobiTest() throws ParseException {
        MathematicalObjectOfInterest jacobi = new MathematicalObjectOfInterest("P_{n}^{(\\alpha,\\beta)} (x)");
        MathematicalObjectOfInterest pochhammerSymbol = new MathematicalObjectOfInterest("(\\alpha+1)_n");
        MathematicalObjectOfInterest hypergeoF = new MathematicalObjectOfInterest("{}_{2}F_1(a, b; c; x)");

        MathematicalObjectOfInterest definition = new MathematicalObjectOfInterest(
                "P_{n}^{(\\alpha,\\beta)} (z) = " +
                        "\\frac{(\\alpha+1)_n}{n!} {}_2 F_1 (-n, 1 + \\alpha + \\beta + n; \\alpha + 1; \\frac{1}{2}(1-z))"
        );

        DependencyPattern jacobiDep = jacobi.match(definition);
        assertNotNull( jacobiDep );
        assertFalse( jacobiDep.exactMatch() );
        Map<String, String> jGroups = jacobiDep.getMatchedGroups().getCapturedGroupStrings();
        assertEquals("P", jGroups.get("var0"));
        assertEquals("n", jGroups.get("var1"));
        assertEquals("\\alpha", jGroups.get("var2"));
        assertEquals("\\beta", jGroups.get("var3"));
        assertEquals("z", jGroups.get("var4"));

        DependencyPattern pochhammerSymbolDep = pochhammerSymbol.match(definition);
        assertNotNull( pochhammerSymbolDep );
        assertFalse( pochhammerSymbolDep.exactMatch() );
        Map<String, String> pSGroups = pochhammerSymbolDep.getMatchedGroups().getCapturedGroupStrings();
        assertEquals("\\alpha", pSGroups.get("var0"));
        assertEquals("n", pSGroups.get("var1"));

        DependencyPattern hypergeoFDep = hypergeoF.match(definition);
        assertNotNull( hypergeoFDep );
        assertFalse( hypergeoFDep.exactMatch() );
        Map<String, String> hGroups = hypergeoFDep.getMatchedGroups().getCapturedGroupStrings();
        assertEquals("F", hGroups.get("var0"));
        assertEquals("- n", hGroups.get("var1"));
        assertEquals("1 + \\alpha + \\beta + n", hGroups.get("var2"));
        assertEquals("\\alpha + 1", hGroups.get("var3"));
        assertEquals("\\frac{1}{2} (1 - z)", hGroups.get("var4"));

        assertNull(definition.match(jacobi));
        assertNull(definition.match(pochhammerSymbol));
        assertNull(definition.match(hypergeoF));
    }
}
