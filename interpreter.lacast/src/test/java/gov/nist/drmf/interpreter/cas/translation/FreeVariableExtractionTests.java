package gov.nist.drmf.interpreter.cas.translation;

import gov.nist.drmf.interpreter.common.FreeVariables;
import gov.nist.drmf.interpreter.common.InformationLogger;
import gov.nist.drmf.interpreter.common.constants.Keys;
import gov.nist.drmf.interpreter.common.exceptions.InitTranslatorException;
import gov.nist.drmf.interpreter.common.meta.AssumeMLPAvailability;
import gov.nist.drmf.interpreter.common.meta.DLMF;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Andre Greiner-Petter
 */
@AssumeMLPAvailability
public class FreeVariableExtractionTests {
    private static SemanticLatexTranslator slt;

    @BeforeAll
    public static void setup() throws InitTranslatorException {
        slt = new SemanticLatexTranslator(Keys.KEY_MATHEMATICA);
    }

    @Test
    void singleLetterTest() {
        slt.translate("a");
        test(slt.getInfoLogger(), "a");
    }

    @Test
    void twoLetterTest() {
        slt.translate("a + b");
        test(slt.getInfoLogger(), "a", "b");
    }

    @Test
    void alphanumericTest() {
        slt.translate("abc");
        test(slt.getInfoLogger(), "a", "b", "c");
    }

    @Test
    void functionVarTest() {
        slt.translate("\\cos{x}");
        test(slt.getInfoLogger(), "x");
    }

    @Test
    void greekLetterTest() {
        slt.translate("\\alpha");
        test(slt.getInfoLogger(), "\\[Alpha]");
    }

    @Test
    void constantTest() {
        slt.translate("\\iunit");
        test(slt.getInfoLogger());
    }

    @Test
    void greekAndConstantTest() {
        slt.translate("\\alpha + \\iunit");
        test(slt.getInfoLogger(), "\\[Alpha]");
    }

    @Test
    void jacobiTest() {
        slt.translate("\\JacobipolyP{n}{\\alpha}{\\beta}@{a \\cos{x}}");
        test(slt.getInfoLogger(), "n", "\\[Alpha]", "\\[Beta]", "a", "x");
    }

    @Test
    void sumTest() {
        slt.translate("\\sum_{k=0}^{n} \\binom{n}{k}");
        test(slt.getInfoLogger(), "n");
    }

    @Test
    void complexSumProdTest() {
        slt.translate("\\sum_{k=0}^{n} \\binom{n}{k} = \\sum_{k=0}^{n} \\frac{\\prod_{m=1}^{n} m}{\\prod_{m=1}^{k} m \\prod_{m=1}^{n-k} m}");
        test(slt.getInfoLogger(), "n");
    }

    @Test
    void intTest() {
        slt.translate("\\int_{-\\infty}^{\\infty} \\frac{x}{t^2} \\diff{t}");
        test(slt.getInfoLogger(), "x");
    }

    @Test
    void multiIntTest() {
        slt.translate("\\int_{-\\infty}^{\\infty} \\int_{-\\infty}^{\\infty} \\int_{-\\infty}^{\\infty} \\frac{x^y}{t^2} \\diff{t} \\diff{x} \\diff{y}");
        test(slt.getInfoLogger());
    }

    @Test
    void subscriptTest() {
        slt.translate("a_n");
        test(slt.getInfoLogger(), "Subscript[a, n]", "n");
    }

    @Test
    void multiIntSingleVarTest() {
        slt.translate("\\int_{-\\infty}^{\\infty} \\int_{-\\infty}^{\\infty} \\int_{-\\infty}^{\\infty} \\frac{x^y+n}{t^2} \\diff{t} \\diff{x} \\diff{y}");
        test(slt.getInfoLogger(), "n");
    }

    @Test
    @DLMF("4.5.E5")
    void dlmfEFLnTest() {
        slt.translate("\\ln@@{x}\\leq a(x^{1/a}-1)", "4.5.E5");
        test(slt.getInfoLogger(), "x", "a");
    }

    @Test
    @DLMF("1.9.E10")
    void dlmfExpTest() {
        slt.translate("e^{i\\theta}=\\cos@@{\\theta}+i\\sin@@{\\theta}", "1.9.E10");
        test(slt.getInfoLogger(), "\\[Theta]");
    }

    @Test
    @DLMF("1.9.E14")
    void dlmfSubscriptTest() {
        slt.translate("z_{1}\\pm z_{2}=x_{1}\\pm x_{2}+\\iunit(y_{1}\\pm y_{2})", "1.9.E14");
        test(slt.getInfoLogger(),
                "Subscript[z, 1]", "Subscript[z, 2]",
                "Subscript[x, 1]", "Subscript[x, 2]",
                "Subscript[y, 1]", "Subscript[y, 2]"
        );
    }

    @Test
    @DLMF("1.9.E71")
    void dlmfSumIntTest() {
        slt.translate("\\int^{b}_{a}\\sum^{\\infty}_{n=0}f_{n}(t)\\diff{t}=\\sum^{\\infty}_{n=0}\\int^{b}_{a}f_{n}(t)\\diff{t}", "1.9.E71");
        test(slt.getInfoLogger(), "a", "b", "Subscript[f, n]");
    }

    @Test
    @DLMF("9.13.E20")
    void dlmfComplexMacroTest() {
        slt.translate("\\frac{1}{(\\alpha+2)^{1/(\\alpha+2)}}\\*\\EulerGamma@{\\frac{\\alpha+1}{\\alpha+2}}x^{1/2}\\BesselJ{-1/(\\alpha+2)}@{\\frac{2}{\\alpha+2}x^{(\\alpha+2)/2}}", "9.13.E20");
        test(slt.getInfoLogger(), "\\[Alpha]", "x");
    }

    @Test
    @DLMF("10.20.E2")
    void dlmfMultiEqualTest() {
        slt.translate("\\frac{2}{3}\\zeta^{\\frac{3}{2}}=\\int_{z}^{1}\\frac{\\sqrt{1-t^{2}}}{t}\\diff{t}=\\ln@{\\frac{1+\\sqrt{1-z^{2}}}{z}}-\\sqrt{1-z^{2}}", "10.20.E2");
        test(slt.getInfoLogger(), "\\[Zeta]", "z");
    }

    @Test
    @DLMF("17.2.E4")
    void qPochhammerTest() {
        slt.translate("\\qPochhammer{a}{q}{\\infty}=\\prod_{j=0}^{\\infty}(1-aq^{j})", "17.2.E4");
        test(slt.getInfoLogger(), "a", "q");
    }

    @Test
    @DLMF("17.5.E5")
    void qGenhyperPhiTest() {
        slt.translate("\\qgenhyperphi{1}{1}@@{a}{c}{q}{c/a}=\\frac{\\qPochhammer{c/a}{q}{\\infty}}{\\qPochhammer{c}{q}{\\infty}}", "17.5.E5");
        test(slt.getInfoLogger(), "a", "c", "q");
    }

    @Test
    @DLMF("24.5.E4")
    void sumEulerNumberTest() {
        slt.translate("\\sum_{k=0}^{n}{2n\\choose 2k}\\EulernumberE{2k}=0", "24.5.E4");
        test(slt.getInfoLogger(), "n");
    }

    @Test
    @DLMF("24.5.E9")
    void subscriptSumTest() {
        slt.translate("a_{n}=\\sum_{k=0}^{n}{n\\choose k}\\frac{b_{n-k}}{k+1}", "24.5.E9");
        test(slt.getInfoLogger(), "Subscript[a, n]", "n", "Subscript[b, n - k]");
    }

    private static void test(InformationLogger info, String... vars) {
        FreeVariables variables = info.getFreeVariables();
        assertNotNull(variables);
        assertEquals(vars.length, variables.getFreeVariables().size(), "Expected " + Arrays.toString(vars) + " but got " + variables.getFreeVariables());
        for ( String var : vars )
            assertTrue(variables.getFreeVariables().contains(var), "Expected to find variable '" + var + "' in " + variables.getFreeVariables());
    }


}
