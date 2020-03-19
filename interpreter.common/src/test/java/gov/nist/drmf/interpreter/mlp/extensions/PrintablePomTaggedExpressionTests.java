package gov.nist.drmf.interpreter.mlp.extensions;

import gov.nist.drmf.interpreter.common.meta.AssumeMLPAvailability;
import gov.nist.drmf.interpreter.common.meta.DLMF;
import gov.nist.drmf.interpreter.mlp.MLPWrapper;
import gov.nist.drmf.interpreter.mlp.SemanticMLPWrapper;
import mlp.ParseException;
import mlp.PomTaggedExpression;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Andre Greiner-Petter
 */
@AssumeMLPAvailability
public class PrintablePomTaggedExpressionTests {
    private static MLPWrapper mlp;

    @BeforeAll
    public static void setup() {
        mlp = new MLPWrapper();
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
        SemanticMLPWrapper smlp = new SemanticMLPWrapper();
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

    private void checkList( List<PrintablePomTaggedExpression> components, String... matches ) {
        assertEquals(matches.length, components.size());
        for ( int i = 0; i < matches.length; i++ ){
            assertEquals(matches[i], components.get(i).getTexString());
        }
    }
}
