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

    @Test
    public void simpleLong() {
        String in = "\\sqrt{\\frac{1}{\\iunit}}";
        String eout = "sqrt((1)/(I))";
        String out = slt.translate(in);
        assertEquals(eout, out);
    }

    @Test
    public void log() {
        String in = "\\log{x}^3";
        String eout = "log(x)^3";
        String out = slt.translate(in);
        assertEquals(eout, out);
    }

    @Test
    public void cos() {
        String in = "\\cos^2{x}";
        String eout = "(cos(x))^(2)";
        String out = slt.translate(in);
        assertEquals(eout, out);
    }

    @Test
    public void trickyMultiply() {
        String in = "\\pi (t - (n+\\frac{1}{2}) \\tau)";
        String eout = "(pi)/(sin(pi*(t -(n +(1)/(2))tau)))";
        String out = slt.translate(in);
        assertEquals(eout, out);
    }

    @Test
    public void fracMultiply() {
        String in = "(\\frac{x}{y})+1";
        String eout = "((x)/(y))+ 1";
        String out = slt.translate(in);
        assertEquals(eout, out);
    }
}
