package gov.nist.drmf.interpreter.cas.blueprints;

import gov.nist.drmf.interpreter.cas.common.ForwardTranslationProcessConfig;
import gov.nist.drmf.interpreter.cas.translation.SemanticLatexTranslator;
import gov.nist.drmf.interpreter.common.constants.GlobalPaths;
import gov.nist.drmf.interpreter.common.constants.Keys;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * @author Andre Greiner-Petter
 */
public class LimitBlueprintTest {

    private static BlueprintMaster btmaster;

    @BeforeAll
    public static void setup() throws IOException {
        SemanticLatexTranslator slt = new SemanticLatexTranslator(Keys.KEY_MAPLE);
        slt.init( GlobalPaths.PATH_REFERENCE_DATA );
        btmaster = slt.getBlueprintMaster();
    }

    @Test
    public void simpleEquationTest() {
        String str = "a = 1";
        Limits limit = btmaster.findMatchingLimit(str);
        assertEquals("a", limit.getVars().get(0));
        assertEquals("1", limit.getLower().get(0));
        assertEquals("infinity", limit.getUpper().get(0));

        assertEquals(1, limit.getVars().size());
        assertEquals(1, limit.getLower().size());
        assertEquals(1, limit.getUpper().size());
    }

    @Test
    public void simpleEquationLongerTest() {
        String str = "n = -\\infty";
        Limits limit = btmaster.findMatchingLimit(str);
        assertEquals("n", limit.getVars().get(0));
        assertEquals("- infinity", limit.getLower().get(0));
        assertEquals("infinity", limit.getUpper().get(0));

        assertEquals(1, limit.getVars().size());
        assertEquals(1, limit.getLower().size());
        assertEquals(1, limit.getUpper().size());
    }

    @Test
    public void multiEquationTest() {
        String str = "a, b, c = 1";
        Limits limit = btmaster.findMatchingLimit(str);
        assertEquals("a", limit.getVars().get(0));
        assertEquals("1", limit.getLower().get(0));
        assertEquals("infinity", limit.getUpper().get(0));

        assertEquals("b", limit.getVars().get(1));
        assertEquals("1", limit.getLower().get(1));
        assertEquals("infinity", limit.getUpper().get(1));

        assertEquals("c", limit.getVars().get(2));
        assertEquals("1", limit.getLower().get(2));
        assertEquals("infinity", limit.getUpper().get(2));

        assertEquals(3, limit.getVars().size());
        assertEquals(3, limit.getLower().size());
        assertEquals(3, limit.getUpper().size());
    }

    @Test
    public void simpleRelationTest() {
        String str = "1 \\leq n \\leq 10";
        Limits limit = btmaster.findMatchingLimit(str);
        assertEquals("n", limit.getVars().get(0));
        assertEquals("1", limit.getLower().get(0));
        assertEquals("10", limit.getUpper().get(0));

        assertEquals(1, limit.getVars().size());
        assertEquals(1, limit.getLower().size());
        assertEquals(1, limit.getUpper().size());
    }

    @Test
    public void multiRelationTest() {
        String str = "1 \\le n, k \\leq 10";
        Limits limit = btmaster.findMatchingLimit(str);
        assertEquals("n", limit.getVars().get(0));
        assertEquals("1", limit.getLower().get(0));
        assertEquals("10", limit.getUpper().get(0));

        assertEquals("k", limit.getVars().get(1));
        assertEquals("1", limit.getLower().get(1));
        assertEquals("10", limit.getUpper().get(1));

        assertEquals(2, limit.getVars().size());
        assertEquals(2, limit.getLower().size());
        assertEquals(2, limit.getUpper().size());
    }

    @Test
    public void multiRelationHardTest() {
        String str = "1 \\le j < k \\le n";
        Limits limit = btmaster.findMatchingLimit(str);
        assertEquals("j", limit.getVars().get(0));
        assertEquals("1", limit.getLower().get(0));
        assertEquals("k - 1", limit.getUpper().get(0));

        assertEquals("k", limit.getVars().get(1));
        assertEquals("j + 1", limit.getLower().get(1));
        assertEquals("n", limit.getUpper().get(1));

        assertEquals(2, limit.getVars().size());
        assertEquals(2, limit.getLower().size());
        assertEquals(2, limit.getUpper().size());
    }

    @Test
    public void infinityTest() {
        String str = "-\\infty < n < \\infty";
        Limits limit = btmaster.findMatchingLimit(str);
        assertEquals("n", limit.getVars().get(0));
        assertEquals("- infinity", limit.getLower().get(0));
        assertEquals("infinity", limit.getUpper().get(0));

        assertEquals(1, limit.getVars().size());
        assertEquals(1, limit.getLower().size());
        assertEquals(1, limit.getUpper().size());
    }

    @Test
    public void subscriptTest() {
        String str = "n_k = 1";
        Limits limit = btmaster.findMatchingLimit(str);
        assertEquals("n[k]", limit.getVars().get(0));
        assertEquals("1", limit.getLower().get(0));
        assertEquals("infinity", limit.getUpper().get(0));

        assertEquals(1, limit.getVars().size());
        assertEquals(1, limit.getLower().size());
        assertEquals(1, limit.getUpper().size());
    }

    @Test
    public void superscriptTest() {
        String str = "p^m \\leq x";
        Limits limit = btmaster.findMatchingLimit(str);
        assertEquals("(p)^(m)", limit.getVars().get(0));
        assertEquals("- infinity", limit.getLower().get(0));
        assertEquals("x", limit.getUpper().get(0));

        assertEquals(1, limit.getVars().size());
        assertEquals(1, limit.getLower().size());
        assertEquals(1, limit.getUpper().size());
    }

    @Test
    public void setSumTest() {
        String str = "x \\in \\Omega_n";
        Limits limit = btmaster.findMatchingLimit(str);
        assertEquals("x", limit.getVars().get(0));
        assertEquals("Omega[n]", limit.getLower().get(0));
        assertTrue(limit.isLimitOverSet());

        assertEquals(1, limit.getVars().size());
        assertEquals(1, limit.getLower().size());
    }

    @Test
    public void hideRelTest() {
        String str = "n \\hiderel{=} 1";
        Limits limit = btmaster.findMatchingLimit(str);
        assertEquals("n", limit.getVars().get(0));
        assertEquals("1", limit.getLower().get(0));
        assertFalse(limit.isLimitOverSet());

        assertEquals(1, limit.getVars().size());
        assertEquals(1, limit.getLower().size());
    }

    @Test
    public void longVarNameTest() {
        String str = "\\ell = 0";
        Limits limit = btmaster.findMatchingLimit(str);
        assertEquals("ell", limit.getVars().get(0));
        assertEquals("0", limit.getLower().get(0));
        assertFalse(limit.isLimitOverSet());

        assertEquals(1, limit.getVars().size());
        assertEquals(1, limit.getLower().size());
    }
}
