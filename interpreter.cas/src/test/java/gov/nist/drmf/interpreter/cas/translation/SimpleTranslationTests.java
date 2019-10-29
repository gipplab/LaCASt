package gov.nist.drmf.interpreter.cas.translation;

import gov.nist.drmf.interpreter.common.constants.GlobalPaths;
import gov.nist.drmf.interpreter.common.constants.Keys;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Andre Greiner-Petter
 */
public class SimpleTranslationTests {

    private static SemanticLatexTranslator slt;

    @BeforeAll
    public static void setup() throws IOException {
        slt = new SemanticLatexTranslator(Keys.KEY_MAPLE);
        slt.init(GlobalPaths.PATH_REFERENCE_DATA);
    }

    @Test
    public void singleSymbolTest() {
        String in = "\\cpi";
        String eout = "Pi";
        String out = slt.translate(in);
        assertEquals(eout, out);
    }

    @Test
    public void negativeTest() {
        String in = "-\\cpi";
        String eout = "- Pi";
        String out = slt.translate(in);
        assertEquals(eout, out);
    }

    @Test
    public void sequenceTest() {
        String in = "a+b";
        String eout = "a + b";
        String out = slt.translate(in);
        assertEquals(eout, out);
    }

    @Test
    public void functionTest() {
        String in = "\\cos(x)";
        String eout = "cos(x)";
        String out = slt.translate(in);
        assertEquals(eout, out);
    }

    @Test
    public void macroTest() {
        String in = "\\JacobiP{\\alpha}{\\beta}{n}@{\\cos@{a\\Theta}}";
        String eout = "JacobiP(n, alpha, beta, cos(a*Theta))";
        String out = slt.translate(in);
        assertEquals(eout, out);
    }

}
