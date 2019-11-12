package gov.nist.drmf.interpreter.cas.translation;

import gov.nist.drmf.interpreter.common.constants.GlobalPaths;
import gov.nist.drmf.interpreter.common.constants.Keys;
import gov.nist.drmf.interpreter.common.exceptions.TranslationException;
import gov.nist.drmf.interpreter.common.tests.AssumeMLPAvailability;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Andre Greiner-Petter
 */
@AssumeMLPAvailability
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
    public void sinPower() {
        String in = "\\sin{x}^3";
        String eout = "(sin(x))^(3)";
        String out = slt.translate(in);
        assertEquals(eout, out);
    }

    @Test
    public void cosPowerArgument() {
        String in = "\\cos{x^3}";
        String eout = "cos((x)^(3))";
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
        String eout = "pi*(t -(n +(1)/(2))*tau)";
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

    @Test
    public void fracMultiply2() {
        String in = "(\\frac{x}{y})x";
        String eout = "((x)/(y))* x";
        String out = slt.translate(in);
        assertEquals(eout, out);
    }

    @Test
    public void paraMultiplyTest() {
        String in = "(x+y)(x-y)";
        String eout = "(x + y)*(x - y)";
        String out = slt.translate(in);
        assertEquals(eout, out);
    }

    @Test
    public void plusMinusMultiplyTest() {
        String in = "(t+\\frac{1}{2}-(n+1))";
        String eout = "(t +(1)/(2)-(n + 1))";
        String out = slt.translate(in);
        assertEquals(eout, out);
    }

    @Test
    public void derivTest() {
        String in = "\\deriv[2]{w}{z}";
        String eout = "diff(w, [z$(2)])";
        String out = slt.translate(in);
        assertEquals(eout, out);
    }

    @Test
    public void modTest() {
        String in = "(k-1) \\mod m";
        String eout = "`modp`(k - 1,m)";
        String out = slt.translate(in);
        assertEquals(eout, out);
    }

    @Test
    public void subscriptMultiplyTest() {
        String in = "x_t x";
        String eout = "x[t]*x";
        String out = slt.translate(in);
        assertEquals(eout, out);
    }

    @Test
    public void absoluteValueTest() {
        String in = "\\left| \\frac{z_1}{z_2} \\right| = \\frac{|z_1|}{|z_2|}";
        String eout = "abs((z[1])/(z[2]))=(abs(z[1]))/(abs(z[2]))";
        String out = slt.translate(in);
        assertEquals(eout, out);
    }

    @Test
    public void absoluteValueInvalidTest() {
        String in = "\\left| x |";
        assertThrows(TranslationException.class, () -> slt.translate(in));
    }

    @Test
    public void emptyDerivTest() {
        String in = "\\deriv{}{z} z^a = az^{a-1}";
        String eout = "diff((z)^(a), z)= a*(z)^(a - 1)";
        String out = slt.translate(in);
        assertEquals(eout, out);
        //\tfrac{1}{4} |z|
    }

    @Test
    public void multiplyBeforeBarTest() {
        String in = "\\tfrac{1}{4} |z|";
        String eout = "(1)/(4)*abs(z)";
        String out = slt.translate(in);
        assertEquals(eout, out);
    }

    @Test
    public void multiplyTrickyBarTest() {
        String in = "(\\tfrac{1}{4} + |z|)n";
        String eout = "((1)/(4)+abs(z))* n";
        String out = slt.translate(in);
        assertEquals(eout, out);
    }

    @Test
    public void multiplyTrickyBar2Test() {
        String in = "(\\tfrac{1}{4} + \\left|z \\right|)n";
        String eout = "((1)/(4)+abs(z))* n";
        String out = slt.translate(in);
        assertEquals(eout, out);
    }

    @Test
    public void generalBracketTest() {
        String in = "\\left[ x \\right] + \\left( y \\right) + \\left| z \\right|";
        String eout = "[x]+(y)+abs(z)";
        String out = slt.translate(in);
        assertEquals(eout, out);
    }

    @Test
    public void superSubScriptTest() {
        String in = "x_2^4";
        String inRev = "x^4_2";

        String eout = "(x[2])^(4)";
        String out = slt.translate(in);
        String outRev = slt.translate(inRev);
        assertEquals(eout, out);
        assertEquals(eout, outRev);
    }
}
