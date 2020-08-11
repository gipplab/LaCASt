package gov.nist.drmf.interpreter.mlp.extensions;

import gov.nist.drmf.interpreter.common.meta.AssumeMLPAvailability;
import gov.nist.drmf.interpreter.common.meta.DLMF;
import gov.nist.drmf.interpreter.mlp.FakeMLPGenerator;
import gov.nist.drmf.interpreter.mlp.FeatureSetUtility;
import gov.nist.drmf.interpreter.mlp.MLPWrapper;
import gov.nist.drmf.interpreter.mlp.SemanticMLPWrapper;
import mlp.MathTerm;
import mlp.ParseException;
import mlp.PomTaggedExpression;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalToCompressingWhiteSpace;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Andre Greiner-Petter
 */
@AssumeMLPAvailability
public class PrintablePomTaggedExpressionTests {
    private static MLPWrapper mlp;

    @BeforeAll
    public static void setup() {
        mlp = SemanticMLPWrapper.getStandardInstance();
    }

    @Test
    public void simpleToStringTest() throws ParseException {
        String texString = "a";
        PrintablePomTaggedExpression ppte = mlp.parse(texString);
        assertEquals(texString, ppte.getTexString());
    }

    @Test
    public void simpleDepthOneTest() throws ParseException {
        String texString = "a + b";
        PrintablePomTaggedExpression ppte = mlp.parse(texString);
        assertEquals(texString, ppte.getTexString());

        List<PomTaggedExpression> components = ppte.getComponents();
        assertEquals(3, components.size());
        assertTrue(components.get(0) instanceof PrintablePomTaggedExpression);
        assertTrue(components.get(1) instanceof PrintablePomTaggedExpression);
        assertTrue(components.get(2) instanceof PrintablePomTaggedExpression);

        List<PrintablePomTaggedExpression> printComps = ppte.getPrintableComponents();
        checkList(printComps, "a", "+", "b");
    }

    @Test
    public void nestedFracTest() throws ParseException {
        String texString = "a + \\frac{a+b}{b+c}";
        PrintablePomTaggedExpression ppte = mlp.parse(texString);
        assertEquals(texString, ppte.getTexString());

        List<PomTaggedExpression> components = ppte.getComponents();
        assertEquals(3, components.size());

        List<PrintablePomTaggedExpression> fracCompy =
                ppte.getPrintableComponents().get(2).getPrintableComponents();

        assertEquals(2, fracCompy.size());
        checkList(fracCompy, "{a+b}", "{b+c}");
    }

    @Test
    public void copyConstructorTest() throws ParseException {
        String texString = "a + b";
        PrintablePomTaggedExpression ppte = mlp.parse(texString);
        PrintablePomTaggedExpression copy = new PrintablePomTaggedExpression(ppte);
        assertEquals(ppte.getTexString(), copy.getTexString());

        List<PrintablePomTaggedExpression> origChildren = ppte.getPrintableComponents();
        assertEquals(3, ppte.getPrintableComponents().size());
        assertEquals(3, copy.getPrintableComponents().size());
        origChildren.remove(0);
        assertEquals(2, ppte.getPrintableComponents().size());
        assertEquals(3, copy.getPrintableComponents().size());
    }

    @Test
    public void constructorNestedTest() throws ParseException {
        String texString = "\\frac{\\sin (x)}{\\sin (y)} - \\frac{\\sin (z)}{\\sin (q^2)}";
        PrintablePomTaggedExpression ppte = mlp.parse(texString);
        assertEquals(texString, ppte.getTexString());

        List<PrintablePomTaggedExpression> firstFracStr =
                ppte.getPrintableComponents().get(0).getPrintableComponents();
        assertEquals("{\\sin (x)}", firstFracStr.get(0).getTexString());
        assertEquals("{\\sin (y)}", firstFracStr.get(1).getTexString());

        List<PrintablePomTaggedExpression> secondFracStr =
                ppte.getPrintableComponents().get(2).getPrintableComponents();
        assertEquals("{\\sin (z)}", secondFracStr.get(0).getTexString());
        assertEquals("{\\sin (q^2)}", secondFracStr.get(1).getTexString());

        List<PrintablePomTaggedExpression> lastElement = secondFracStr.get(1).getPrintableComponents();
        assertEquals("\\sin", lastElement.get(0).getTexString());
        assertEquals("(", lastElement.get(1).getTexString());
        assertEquals("q", lastElement.get(2).getTexString());
        assertEquals("^2", lastElement.get(3).getTexString());
        assertEquals(")", lastElement.get(4).getTexString());
    }

    @Test
    public void nestedDepthTest() throws ParseException {
        String texString = "a + b^{1+x}";
        PrintablePomTaggedExpression ppte = mlp.parse(texString);
        assertEquals(texString, ppte.getTexString());

        List<PrintablePomTaggedExpression> printComps = ppte.getPrintableComponents();
        checkList(printComps, "a", "+", "b", "^{1+x}");

        List<PrintablePomTaggedExpression> innerComps = printComps.get(3).getPrintableComponents();
        checkList(innerComps, "{1+x}");

        List<PrintablePomTaggedExpression> innerInnerComps = innerComps.get(0).getPrintableComponents();
        checkList(innerInnerComps, "1", "+", "x");
    }

    @Test
    public void radicalTest() throws ParseException {
        String texString = "\\sqrt[n]{x+1}";
        PrintablePomTaggedExpression ppte = mlp.parse(texString);
        assertEquals(texString, ppte.getTexString());

        List<PrintablePomTaggedExpression> printComps = ppte.getPrintableComponents();
        checkList(printComps, "[n]", "{x+1}");

        List<PrintablePomTaggedExpression> innerComps = printComps.get(1).getPrintableComponents();
        checkList(innerComps, "x", "+", "1");
    }

    @Test
    public void radicalSequenceTest() throws ParseException {
        String texString = "\\sqrt[n]{x+1}+y";
        PrintablePomTaggedExpression ppte = mlp.parse(texString);
        assertEquals(texString, ppte.getTexString());

        List<PrintablePomTaggedExpression> printComps = ppte.getPrintableComponents();
        checkList(printComps, "\\sqrt[n]{x+1}", "+", "y");

        List<PrintablePomTaggedExpression> innerComps = printComps.get(0).getPrintableComponents();
        checkList(innerComps, "[n]", "{x+1}");
    }

    @Test
    public void successiveIdenticalTokensTest() throws ParseException {
        String texString = "n n";
        PrintablePomTaggedExpression ppte = mlp.parse(texString);
        assertEquals(texString, ppte.getTexString());

        List<PrintablePomTaggedExpression> printComps = ppte.getPrintableComponents();
        checkList(printComps, "n", "n");
    }

    @Test
    public void nestedAndSuccessiveTest() throws ParseException {
        String texString = "1+\\sqrt{x+x^2}";
        PrintablePomTaggedExpression ppte = mlp.parse(texString);
        assertEquals(texString, ppte.getTexString());

        List<PrintablePomTaggedExpression> printComps = ppte.getPrintableComponents();
        checkList(printComps, "1", "+", "\\sqrt{x+x^2}");

        List<PrintablePomTaggedExpression> innerComps = printComps.get(2).getPrintableComponents();
        checkList(innerComps, "{x+x^2}");

        List<PrintablePomTaggedExpression> innerInnerComps = innerComps.get(0).getPrintableComponents();
        checkList(innerInnerComps, "x", "+", "x", "^2");
    }

    @Test
    public void semanticLaTeXTest() throws ParseException, IOException {
        String texString = "\\JacobiP{\\alpha}{\\beta}{n}@{a+\\cos@{x}}";
        SemanticMLPWrapper smlp = SemanticMLPWrapper.getStandardInstance();
        PrintablePomTaggedExpression ppte = smlp.parse(texString);
        assertEquals(texString, ppte.getTexString());

        List<PrintablePomTaggedExpression> printComps = ppte.getPrintableComponents();
        checkList(printComps, "\\JacobiP", "{\\alpha}", "{\\beta}", "{n}", "@", "{a+\\cos@{x}}");
    }

    @Test
    public void fractionSequenceTest() throws ParseException {
        String texString = "\\frac{1}{2}+\\frac{2}{3}";
        PrintablePomTaggedExpression ppte = mlp.parse(texString);
        assertEquals(texString, ppte.getTexString());

        List<PrintablePomTaggedExpression> printComps = ppte.getPrintableComponents();
        checkList(printComps, "\\frac{1}{2}", "+", "\\frac{2}{3}");

        List<PrintablePomTaggedExpression> innerComps = printComps.get(0).getPrintableComponents();
        checkList(innerComps, "{1}", "{2}");

        List<PrintablePomTaggedExpression> innerComps2 = printComps.get(2).getPrintableComponents();
        checkList(innerComps2, "{2}", "{3}");
    }

    @Test
    public void setComponentsTest() throws ParseException {
        String texString = "\\frac{x^1}{2}";
        String replace = "a^2 + b^2";

        PrintablePomTaggedExpression orig = mlp.parse(texString);
        PrintablePomTaggedExpression ref = mlp.parse(replace);

        PrintablePomTaggedExpression enumerator = orig.getPrintableComponents().get(0);
        enumerator.setComponents(ref);

        assertEquals("\\frac{a^2 + b^2}{2}", orig.getTexString());
    }

    @Test
    public void constructSetComponentsTest() throws ParseException {
        String replace = "a^2 + b^2";

        PrintablePomTaggedExpression orig = FakeMLPGenerator.generateEmptySequencePPTE();
        PrintablePomTaggedExpression ref = mlp.parse(replace);

        List<PomTaggedExpression> content = ref.getComponents();
        orig.setComponents(content);

        assertEquals("a^2 + b^2", orig.getTexString());
    }

    @Test
    public void subSuperScriptTest() throws ParseException {
        String texString = "y \\cdot y_b^a";
        PrintablePomTaggedExpression ppte = mlp.parse(texString);
        assertEquals(texString, ppte.getTexString());

        List<PrintablePomTaggedExpression> printComps = ppte.getPrintableComponents();
        checkList(printComps, "y", "\\cdot", "y", "_b^a");

        List<PrintablePomTaggedExpression> innerComps = printComps.get(3).getPrintableComponents();
        checkList(innerComps, "_b", "^a");
    }

    @Test
    public void fractionTest() throws ParseException {
        String texString = "\\frac{a}{b}";
        PrintablePomTaggedExpression ppte = mlp.parse(texString);
        assertEquals(texString, ppte.getTexString());

        List<PrintablePomTaggedExpression> printComps = ppte.getPrintableComponents();
        checkList(printComps, "{a}", "{b}");
    }

    @Test
    @DLMF("4.4.8")
    public void elementaryDLMFTest() throws ParseException {
        String texString = "e^{\\pm\\pi\\mathrm{i}/3}=\\frac{1}{2}\\pm\\mathrm{i}\\frac{\\sqrt{3}}{2}";
        PrintablePomTaggedExpression ppte = mlp.parse(texString);
        assertEquals(texString, ppte.getTexString());

        List<PrintablePomTaggedExpression> printComps = ppte.getPrintableComponents();
        checkList(printComps, "e", "^{\\pm\\pi\\mathrm{i}/3}", "=", "\\frac{1}{2}", "\\pm", "\\mathrm{i}", "\\frac{\\sqrt{3}}{2}");
    }

    @Test
    @DLMF("9.6.2")
    public void airyAiDLMFTest() throws ParseException {
        String texString = "\\operatorname{Ai}\\left(z\\right)=\\pi^{-1}\\sqrt{z/3}K_{\\pm 1/3}\\left(\\zeta\\right)";
        PrintablePomTaggedExpression ppte = mlp.parse(texString);
        assertEquals(texString, ppte.getTexString());

        List<PrintablePomTaggedExpression> printComps = ppte.getPrintableComponents();
        checkList(printComps,
                "\\operatorname",
                "{Ai}",
                "\\left(", "z", "\\right)",
                "=",
                "\\pi",
                "^{-1}",
                "\\sqrt{z/3}",
                "K", "_{\\pm 1/3}",
                "\\left(", "\\zeta", "\\right)");
    }

    @Test
    public void balancedFractionSumTest() throws ParseException {
        String texString = "\\left(\\frac{\\cos{a}+1}{\\sin{b}+2}\\right)";
        PrintablePomTaggedExpression ppte = mlp.parse(texString);
        assertEquals(texString, ppte.getTexString());

        List<PrintablePomTaggedExpression> printComps = ppte.getPrintableComponents();
        checkList(printComps,
                "\\left(",
                "\\frac{\\cos{a}+1}{\\sin{b}+2}",
                "\\right)");
    }

    @Test
    @DLMF("22.12.2")
    public void longPrintableDLMFTest() throws ParseException {
        String texString = "\\sum_{n=-\\infty}^{\\infty} \\frac{\\pi}{\\sin@{\\pi (t - (n+\\frac{1}{2}) \\tau)}} = " +
                "\\sum_{n=-\\infty}^{\\infty} \\left( " +
                    "\\sum_{m=-\\infty}^{\\infty} \\frac{(-1)^m}{t - m - (n+\\frac{1}{2}) \\tau} " +
                "\\right)";
        PrintablePomTaggedExpression ppte = mlp.parse(texString);
        assertEquals(texString, ppte.getTexString());

        List<PrintablePomTaggedExpression> printComps = ppte.getPrintableComponents();
        checkList(printComps,
                "\\sum", "_{n=-\\infty}^{\\infty}",
                "\\frac{\\pi}{\\sin@{\\pi (t - (n+\\frac{1}{2}) \\tau)}}",
                "=",
                "\\sum", "_{n=-\\infty}^{\\infty}",
                "\\left(",
                    "\\sum", "_{m=-\\infty}^{\\infty}",
                    "\\frac{(-1)^m}{t - m - (n+\\frac{1}{2}) \\tau}",
                "\\right)"
        );

        printComps = printComps.get(2).getPrintableComponents();
        checkList(printComps,
                "{\\pi}",
                "{\\sin@{\\pi (t - (n+\\frac{1}{2}) \\tau)}}"
        );

        printComps = printComps.get(1).getPrintableComponents();
        checkList(printComps,
                "\\sin", "@", "{\\pi (t - (n+\\frac{1}{2}) \\tau)}"
        );

        printComps = printComps.get(2).getPrintableComponents();
        checkList(printComps,
                "\\pi", "(", "t", "-", "(", "n", "+", "\\frac{1}{2}", ")", "\\tau", ")"
        );
    }

    @Test
    public void sameEndingFractionTest() throws ParseException {
        String texString = "\\frac{b-a_b}{b-a_b}";
        PrintablePomTaggedExpression ppte = mlp.parse(texString);
        assertEquals(texString, ppte.getTexString());

        List<PrintablePomTaggedExpression> printComps = ppte.getPrintableComponents();
        checkList(printComps, "{b-a_b}", "{b-a_b}");
    }

    @Test
    @DLMF("16.11.2")
    public void fractionSumTest() throws ParseException {
        String texString = "\\sum_{m=1}^p " +
                "\\sum_{k=0}^\\infty " +
                "\\frac{\\opminus^k}{k!} " +
                "\\EulerGamma@{a_m + k} " +
                "\\left(" +
                    "\\frac{" +
                        "\\prod_{\\ell=1}^p " +
                        "\\EulerGamma@{a_\\ell - a_m - k}" +
                    "} " +
                    "{" +
                        "\\prod_{\\ell=1}^q " +
                        "\\EulerGamma@{b_\\ell - a_m - k}" +
                    "}" +
                "\\right) " +
                "z^{-a_m - k}";
        PrintablePomTaggedExpression ppte = mlp.parse(texString);
        assertEquals(texString, ppte.getTexString());

        List<PrintablePomTaggedExpression> printComps = ppte.getPrintableComponents();
        checkList(printComps,
                "\\sum", "_{m=1}^p",
                "\\sum", "_{k=0}^\\infty",
                "\\frac{\\opminus^k}{k!}",
                "\\EulerGamma", "@", "{a_m + k}",
                "\\left(",
                    "\\frac{\\prod_{\\ell=1}^p \\EulerGamma@{a_\\ell - a_m - k}} {\\prod_{\\ell=1}^q \\EulerGamma@{b_\\ell - a_m - k}}",
                "\\right)",
                "z", "^{-a_m - k}"
        );

        printComps = printComps.get(9).getPrintableComponents();
        checkList(printComps,
                "{\\prod_{\\ell=1}^p \\EulerGamma@{a_\\ell - a_m - k}}",
                "{\\prod_{\\ell=1}^q \\EulerGamma@{b_\\ell - a_m - k}}"
        );
    }

    @Test
    public void overrideSetRootTest() throws ParseException {
        String texString = "\\sqrt[2]{x^2}";
        PomTaggedExpression pte = mlp.parse(texString);

        MathTerm mt = new MathTerm(" ");
        mt.setNamedFeature(FeatureSetUtility.LATEX_FEATURE_KEY, "\\fake");
        pte.setRoot(mt);

        PrintablePomTaggedExpression ppte = (PrintablePomTaggedExpression) pte;
        assertEquals( "\\fake[2]{x^2}", ppte.getTexString() );
    }

    @Test
    public void overrideSetRootSequenceTest() throws ParseException {
        String texString = "x+\\frac{y}{x^2}";
        PrintablePomTaggedExpression pte = mlp.parse(texString);

        MathTerm mt = new MathTerm(" ");
        mt.setNamedFeature(FeatureSetUtility.LATEX_FEATURE_KEY, "\\fake");

        List<PomTaggedExpression> comps = pte.getComponents();
        List<PomTaggedExpression> innerComps = comps.get(2).getComponents();
        PomTaggedExpression numerator = innerComps.get(0);
        numerator.setRoot(mt);

        assertThat( "x + \\frac{\\fake}{x^2}", equalToCompressingWhiteSpace(pte.getTexString()) );

        PomTaggedExpression xpte = innerComps.get(1).getComponents().get(0);
        MathTerm newMT = new MathTerm("y");
        xpte.setRoot(newMT);

        assertEquals( "x + \\frac{\\fake}{y^2}", pte.getTexString() );
    }

    @Test
    public void emptyDerivTest() throws ParseException {
        String texString = "\\pderiv{}{x}=\\cos@@{\\phi}\\pderiv{}{r}-\\frac{\\sin@@{\\phi}}{r}\\pderiv{}{\\phi}";
        PrintablePomTaggedExpression ppte = mlp.parse(texString);

        assertThat( texString, equalToCompressingWhiteSpace(ppte.getTexString()) );
    }

    @Test
    public void illegalManipulationTest() throws ParseException {
        String texString = "x+\\frac{y}{x^2}";
        PrintablePomTaggedExpression pte = mlp.parse(texString);
        PomTaggedExpression realPTE = mlp.simpleParse(texString);

        assertFalse( realPTE instanceof PrintablePomTaggedExpression );
        assertThrows( IllegalArgumentException.class, () -> pte.set(realPTE) );
        assertThrows( IllegalArgumentException.class, () -> pte.addComponent(realPTE) );
        assertThrows( IllegalArgumentException.class, () -> pte.addComponent(0, realPTE) );
        assertThrows( IllegalArgumentException.class, () -> pte.setComponents(realPTE) );
    }

    @Test
    public void validManipulationTest() throws ParseException {
        String texString = "\\frac{y}{x^2}";
        PrintablePomTaggedExpression pte = mlp.parse("x+y");
        PrintablePomTaggedExpression plusPTE = mlp.parse("+");
        PrintablePomTaggedExpression secondPTE = mlp.parse(texString);

        pte.addComponent(secondPTE);
        assertEquals("x + y \\frac{y}{x^2}", pte.getTexString());

        pte.addComponent(3, plusPTE);
        assertEquals("x + y + \\frac{y}{x^2}", pte.getTexString());

        PrintablePomTaggedExpression newPTE = mlp.parse("y+x");
        pte.set(newPTE);
        assertEquals("y + x", pte.getTexString());

        PrintablePomTaggedExpression completeNewPTE = mlp.parse("z+x+y");
        pte.setComponents(completeNewPTE.getComponents());
        assertEquals("z + x + y", pte.getTexString());
    }

    @Test
    public void emptySubscriptTest() throws ParseException{
        String test = "\\pi+{}_2F_1\\left(a,b;c;z\\right)";
        PrintablePomTaggedExpression p = mlp.parse(test);
        assertEquals(test, p.getTexString());

        List<PrintablePomTaggedExpression> printComps = p.getPrintableComponents();
        checkList(printComps,
                "\\pi", "+", "{}", "_2", "F", "_1",
                "\\left(", "a", ",", "b", ";", "c", ";", "z", "\\right)"
        );
    }

    @Test
    public void spaceTest() throws ParseException {
        String test = "\\pi \\; + \\, 2";
        String test2 = "\\pi + 2";
        PrintablePomTaggedExpression p1 = mlp.parse(test);
        PrintablePomTaggedExpression p2 = mlp.parse(test2);
        assertThat(p2.getTexString(), equalToCompressingWhiteSpace(p1.getTexString()));
    }

    @Test
    public void realWorldWikiExampleTest() throws ParseException {
        String texString = "(1 - x)^{\\alpha}(1 + x)^{\\beta}";
        mlp.parse(texString);
    }

    private void checkList( List<PrintablePomTaggedExpression> components, String... matches ) {
        assertEquals(matches.length, components.size());
        for ( int i = 0; i < matches.length; i++ ){
            assertEquals(matches[i], components.get(i).getTexString());
        }
    }
}
