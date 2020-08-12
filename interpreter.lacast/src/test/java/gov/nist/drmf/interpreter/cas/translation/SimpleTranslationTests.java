package gov.nist.drmf.interpreter.cas.translation;

import gov.nist.drmf.interpreter.cas.common.ForwardTranslationProcessConfig;
import gov.nist.drmf.interpreter.cas.logging.TranslatedExpression;
import gov.nist.drmf.interpreter.cas.translation.components.MacroTranslator;
import gov.nist.drmf.interpreter.common.constants.GlobalPaths;
import gov.nist.drmf.interpreter.common.constants.Keys;
import gov.nist.drmf.interpreter.common.exceptions.InitTranslatorException;
import gov.nist.drmf.interpreter.common.exceptions.TranslationException;
import gov.nist.drmf.interpreter.common.meta.AssumeMLPAvailability;
import gov.nist.drmf.interpreter.common.meta.DLMF;
import gov.nist.drmf.interpreter.mlp.MLPWrapper;
import gov.nist.drmf.interpreter.mlp.SemanticMLPWrapper;
import mlp.ParseException;
import mlp.PomTaggedExpression;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.LinkedList;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Andre Greiner-Petter
 */
@AssumeMLPAvailability
class SimpleTranslationTests {

    private static SemanticLatexTranslator slt;
    private static SemanticLatexTranslator sltMathematica;

    @BeforeAll
    static void setup() throws InitTranslatorException {
        slt = new SemanticLatexTranslator(Keys.KEY_MAPLE);
        sltMathematica = new SemanticLatexTranslator(Keys.KEY_MATHEMATICA);
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
        String eout = "abs((z[1])/(z[2])) = (abs(z[1]))/(abs(z[2]))";
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
        String eout = "diff((z)^(a), z) = a*(z)^(a - 1)";
        String out = slt.translate(in);
        assertEquals(eout, out);
        //\tfrac{1}{4} |z|
    }

    @Test
    void plusMinusTest() {
        String in = "\\pm 1";
        String eout = "&+- 1";
        String eout2 = "\\[PlusMinus]1";
        String out = slt.translate(in);
        String out2 = sltMathematica.translate(in);
        assertEquals(eout, out);
        assertEquals(eout2, out2);
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
    @DLMF("14.2.3")
    void wronksianTest() {
        String in = "\\Wronskian\\left\\{\\FerrersP[-\\mu]{\\nu}@{x},\\FerrersP[-\\mu]{\\nu}@{-x}\\right\\}";
        String expect = "(LegendreP(nu, - mu, x))*diff(LegendreP(nu, - mu, - x), x)-diff(LegendreP(nu, - mu, x), x)*(LegendreP(nu, - mu, - x))";
        String out = slt.translate(in);
        assertEquals(expect, out);
    }

    @Test
    @DLMF("9.2.9")
    void wronksianComplexArgumentTest() {
        String in = "\\Wronskian\\left\\{\\AiryAi@{z e^{-2\\pi \\tfrac{i}{3}}}, \\AiryAi@{z e^{2\\pi \\tfrac{i}{3}}}\\right\\}";
        String expect = "(AiryAi(z*exp(- 2*Pi*(I)/(3))))*diff(AiryAi(z*exp(2*Pi*(I)/(3))), z)-diff(AiryAi(z*exp(- 2*Pi*(I)/(3))), z)*(AiryAi(z*exp(2*Pi*(I)/(3))))";
        String out = slt.translate(in, "9.2.9");
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
        assertEquals("exp(z) = (exp(z))* exp(2*k*z*Pi*I)", res);
    }

    @Test
    void replaceTranslationTest2() {
        String in = "\\compellintPik@{k^{2}}{k}=\\compellintEk@{k}/{k^{\\prime}}^{2}";
        String label = "19.6.2";
        String res = slt.translate(in, label);
        System.out.println(res);
        assertEquals("EllipticPi((k)^(2), k) = EllipticE(k)/(1 - (k)^(2))", res);
    }

    @Test
    public void chooseTranslation() {
        String input = "1 + {n+1 \\choose k}^2";
        String expect = "1 +(binomial(n + 1,k))^(2)";
        String actual = slt.translate(input);
        assertEquals(expect, actual);
    }

    @Test
    public void chooseTranslationInSum() {
        String input = "\\sum_{k=0}^{n}{n+1 \\choose k}";
        String expect = "sum(binomial(n + 1,k), k = 0..n)";
        String actual = slt.translate(input);
        assertEquals(expect, actual);
    }

    @Test
    public void unknownFunctionTranslator() throws ParseException, IOException {
        String input = "\\cos(x)";
        PomTaggedExpression pte = stripOfDLMFInfo(input);
        TranslatedExpression trans = slt.translate(pte);
        assertEquals("cos(x)", trans.toString());
    }

    @Test
    public void macroPackageTranslatorConfigOnOffTest() {
        ForwardTranslationProcessConfig config = slt.getConfig();
        String testExpression = "\\qGamma{q}@{\\qfactorial{n}{q}}";
        String result = slt.translate(testExpression);

        String expectedTranslation = "QGAMMA(q, QFactorial(n, q))";
        String translationWithPackages = "with(QDifferenceEquations,QFactorial):with(QDifferenceEquations,QGAMMA): "
                + expectedTranslation
                + "; unwith(QDifferenceEquations,QFactorial):unwith(QDifferenceEquations,QGAMMA):";

        assertEquals("QGAMMA(q, QFactorial(n, q))", result,
                "The default setting should not show the required packages inline.");
        String info = slt.getInfoLogger().toString();
        System.out.println(info);
        assertTrue(info.contains("QDifferenceEquations,QFactorial"));
        assertTrue(info.contains("QDifferenceEquations,QGAMMA"));

        config.setInlinePackageMode(true);
        result = slt.translate(testExpression);

        assertEquals(translationWithPackages, result);
    }

    /**
     * Rips of the DLMF info from the first element in the parse tree.
     * Input example is "\cos{x}" or something similar.
     * @param input start with a DLMF macro, e.g., "\cos"
     * @return parse tree without dlmf info
     * @throws ParseException
     */
    private PomTaggedExpression stripOfDLMFInfo(String input) throws ParseException, IOException {
        MLPWrapper mlp = SemanticMLPWrapper.getStandardInstance();
        PomTaggedExpression pte = mlp.parse(input);
        PomTaggedExpression cosPte = pte.getComponents().get(0);
        cosPte.getRoot().setAlternativeFeatureSets(new LinkedList<>());
        cosPte.getRoot().setTag("function");
        return pte;
    }
}
