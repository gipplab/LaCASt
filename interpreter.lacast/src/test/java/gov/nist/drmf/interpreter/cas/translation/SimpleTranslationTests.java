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
class SimpleTranslationTests {

    private static SemanticLatexTranslator slt;

    @BeforeAll
    static void setup() throws IOException {
        slt = new SemanticLatexTranslator(Keys.KEY_MAPLE);
        slt.init(GlobalPaths.PATH_REFERENCE_DATA);
    }

    @Test
    void singleSymbolTest() {
        String in = "\\cpi";
        String eout = "Pi";
        String out = slt.translate(in);
        assertEquals(eout, out);
    }

    @Test
    void negativeTest() {
        String in = "-\\cpi";
        String eout = "- Pi";
        String out = slt.translate(in);
        assertEquals(eout, out);
    }

    @Test
    void sequenceTest() {
        String in = "a+b";
        String eout = "a + b";
        String out = slt.translate(in);
        assertEquals(eout, out);
    }

    @Test
    void functionTest() {
        String in = "\\cos(x)";
        String eout = "cos(x)";
        String out = slt.translate(in);
        assertEquals(eout, out);
    }

    @Test
    void macroTest() {
        String in = "\\JacobiP{\\alpha}{\\beta}{n}@{\\cos@{a\\Theta}}";
        String eout = "JacobiP(n, alpha, beta, cos(a*Theta))";
        String out = slt.translate(in);
        assertEquals(eout, out);
    }

    @Test
    void simpleLong() {
        String in = "\\sqrt{\\frac{1}{\\iunit}}";
        String eout = "sqrt((1)/(I))";
        String out = slt.translate(in);
        assertEquals(eout, out);
    }

    @Test
    void sinPower() {
        String in = "\\sin{x}^3";
        String eout = "(sin(x))^(3)";
        String out = slt.translate(in);
        assertEquals(eout, out);
    }

    @Test
    void cosPowerArgument() {
        String in = "\\cos{x^3}";
        String eout = "cos((x)^(3))";
        String out = slt.translate(in);
        assertEquals(eout, out);
    }

    @Test
    void cos() {
        String in = "\\cos^2{x}";
        String eout = "(cos(x))^(2)";
        String out = slt.translate(in);
        assertEquals(eout, out);
    }

    @Test
    void trickyMultiply() {
        String in = "\\pi (t - (n+\\frac{1}{2}) \\tau)";
        String eout = "pi*(t -(n +(1)/(2))*tau)";
        String out = slt.translate(in);
        assertEquals(eout, out);
    }

    @Test
    void fracMultiply() {
        String in = "(\\frac{x}{y})+1";
        String eout = "((x)/(y))+ 1";
        String out = slt.translate(in);
        assertEquals(eout, out);
    }

    @Test
    void fracMultiply2() {
        String in = "(\\frac{x}{y})x";
        String eout = "((x)/(y))* x";
        String out = slt.translate(in);
        assertEquals(eout, out);
    }

    @Test
    void paraMultiplyTest() {
        String in = "(x+y)(x-y)";
        String eout = "(x + y)*(x - y)";
        String out = slt.translate(in);
        assertEquals(eout, out);
    }

    @Test
    void plusMinusMultiplyTest() {
        String in = "(t+\\frac{1}{2}-(n+1))";
        String eout = "(t +(1)/(2)-(n + 1))";
        String out = slt.translate(in);
        assertEquals(eout, out);
    }

    @Test
    void derivTest() {
        String in = "\\deriv[2]{w}{z}";
        String eout = "diff(w, [z$(2)])";
        String out = slt.translate(in);
        assertEquals(eout, out);
    }

    @Test
    void modTest() {
        String in = "(k-1) \\mod m";
        String eout = "`modp`(k - 1,m)";
        String out = slt.translate(in);
        assertEquals(eout, out);
    }

    @Test
    void subscriptMultiplyTest() {
        String in = "x_t x";
        String eout = "x[t]*x";
        String out = slt.translate(in);
        assertEquals(eout, out);
    }

    @Test
    void absoluteValueTest() {
        String in = "\\left| \\frac{z_1}{z_2} \\right| = \\frac{|z_1|}{|z_2|}";
        String eout = "abs((z[1])/(z[2]))=(abs(z[1]))/(abs(z[2]))";
        String out = slt.translate(in);
        assertEquals(eout, out);
    }

    @Test
    void absoluteValueInvalidTest() {
        String in = "\\left| x |";
        assertThrows(TranslationException.class, () -> slt.translate(in));
    }

    @Test
    void emptyDerivTest() {
        String in = "\\deriv{}{z} z^a = az^{a-1}";
        String eout = "diff((z)^(a), z)= a*(z)^(a - 1)";
        String out = slt.translate(in);
        assertEquals(eout, out);
        //\tfrac{1}{4} |z|
    }

    @Test
    void multiplyBeforeBarTest() {
        String in = "\\tfrac{1}{4} |z|";
        String eout = "(1)/(4)*abs(z)";
        String out = slt.translate(in);
        assertEquals(eout, out);
    }

    @Test
    void multiplyTrickyBarTest() {
        String in = "(\\tfrac{1}{4} + |z|)n";
        String eout = "((1)/(4)+abs(z))* n";
        String out = slt.translate(in);
        assertEquals(eout, out);
    }

    @Test
    void multiplyTrickyBar2Test() {
        String in = "|z^a|";
        String eout = "abs((z)^(a))";
        String out = slt.translate(in);
        assertEquals(eout, out);
    }

    @Test
    void multiplyTrickyBar3Test() {
        String in = "(\\tfrac{1}{4} + \\left|z \\right|)n";
        String eout = "((1)/(4)+abs(z))* n";
        String out = slt.translate(in);
        assertEquals(eout, out);
    }

    @Test
    void generalBracketTest() {
        String in = "\\left[ x \\right] + \\left( y \\right) + \\left| z \\right|";
        String eout = "[x]+(y)+abs(z)";
        String out = slt.translate(in);
        assertEquals(eout, out);
    }

    @Test
    void superSubScriptTest() {
        String in = "x_2^4";
        String inRev = "x^4_2";

        String eout = "(x[2])^(4)";
        String out = slt.translate(in);
        String outRev = slt.translate(inRev);
        assertEquals(eout, out);
        assertEquals(eout, outRev);
    }

    @Test
    void multiplyIunitTest() {
        String in = "\\sqrt{2}+\\sqrt{2} \\iunit";
        String out = "sqrt(2)+sqrt(2)*I";

        assertEquals(out, slt.translate(in));
    }

    @Test
    void wronksianTest() {
        String in = "\\Wronskian\\left\\{\\FerrersP[-\\mu]{\\nu}@{x},\\FerrersP[-\\mu]{\\nu}@{-x}\\right\\}";
        String expect = "(LegendreP(nu, - mu, x))*diff(LegendreP(nu, - mu, - x), x)-diff(LegendreP(nu, - mu, x), x)*(LegendreP(nu, - mu, - x))";
        String out = slt.translate(in);
        assertEquals(expect, out);
    }

    @Test
    void overlineTest() {
        assertThrows(TranslationException.class, () -> slt.translate("\\overline{z}"));
        assertThrows(TranslationException.class, () -> slt.translate("\\overline{z+1}"));
    }

    @Test
    void replaceTranslationTest() {
        String in = "e^{z}=(\\exp@@{z})\\exp@{2kz\\pi\\iunit}";
        String label = "4.2.E33";
        String res = slt.translate(in, label);
        System.out.println(res);
        assertEquals("exp(z)=(exp(z))* exp(2*k*z*Pi*I)", res);
    }
}
