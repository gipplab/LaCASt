package gov.nist.drmf.interpreter.cas.translation.components;

import gov.nist.drmf.interpreter.cas.logging.TranslatedExpression;
import gov.nist.drmf.interpreter.cas.translation.SemanticLatexTranslator;
import gov.nist.drmf.interpreter.common.latex.FreeVariables;
import gov.nist.drmf.interpreter.common.TranslationInformation;
import gov.nist.drmf.interpreter.common.constants.Keys;
import gov.nist.drmf.interpreter.common.exceptions.InitTranslatorException;
import gov.nist.drmf.interpreter.common.tests.Resource;
import gov.nist.drmf.interpreter.pom.SemanticMLPWrapper;
import gov.nist.drmf.interpreter.pom.common.meta.AssumeMLPAvailability;
import mlp.ParseException;
import mlp.PomTaggedExpression;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Andre Greiner-Petter
 */
@AssumeMLPAvailability
public class MultiExpressionTranslationTests {
    private static SemanticLatexTranslator slt;
    private static SemanticMLPWrapper mlp;

    @BeforeAll
    static void setup() throws InitTranslatorException {
        slt = new SemanticLatexTranslator(Keys.KEY_MAPLE);
        mlp = SemanticMLPWrapper.getStandardInstance();
    }

    @BeforeEach
    void resetTranslator() {
        slt.reset();
    }

    @Test
    public void alignEnvironmentTest() throws ParseException {
        MultiExpressionTranslator gt = new MultiExpressionTranslator(slt);
        PomTaggedExpression pte = mlp.parse("\\begin{align} x &= y \\\\ x &= z \\end{align}");
        TranslatedExpression te = gt.translate(pte);
        assertEquals("x = y; x = z", te.toString());
        assertEquals(te, gt.getTranslatedExpressionObject());

        List<TranslatedExpression> partialTranslations = gt.getListOfPartialTranslations();
        assertEquals(2, partialTranslations.size());
        assertEquals("x = y", partialTranslations.get(0).getTranslatedExpression());
        assertEquals("x = z", partialTranslations.get(1).getTranslatedExpression());
    }

    @Test
    public void alignEmptyEnvironmentTest() throws ParseException {
        MultiExpressionTranslator gt = new MultiExpressionTranslator(slt);
        PomTaggedExpression pte = mlp.parse("\\begin{align} x &= y \\\\ &= z \\end{align}");
        TranslatedExpression te = gt.translate(pte);
        assertEquals("x = y = z", te.toString());
        assertEquals(te, gt.getTranslatedExpressionObject());

        List<TranslatedExpression> additionalTranslations = gt.getListOfPartialTranslations();
        assertEquals(0, additionalTranslations.size(), additionalTranslations.toString());
    }

    @Test
    public void equationEmptyEnvironmentTest() throws ParseException {
        MultiExpressionTranslator gt = new MultiExpressionTranslator(slt);
        PomTaggedExpression pte = mlp.parse("\\begin{eqnarray} x &=& y \\\\ a &=& z \\end{eqnarray}");
        TranslatedExpression te = gt.translate(pte);
        assertEquals("x = y; a = z", te.toString());
        assertEquals(te, gt.getTranslatedExpressionObject());

        List<TranslatedExpression> additionalTranslations = gt.getListOfPartialTranslations();
        assertEquals(2, additionalTranslations.size(), additionalTranslations.toString());
        assertEquals("x = y", additionalTranslations.get(0).getTranslatedExpression());
        assertEquals("a = z", additionalTranslations.get(1).getTranslatedExpression());

        FreeVariables freeVar1 = additionalTranslations.get(0).getFreeVariables();
        assertEquals(2, freeVar1.getFreeVariables().size());
        assertTrue(freeVar1.getFreeVariables().contains("x"));
        assertTrue(freeVar1.getFreeVariables().contains("y"));

        FreeVariables freeVar2 = additionalTranslations.get(1).getFreeVariables();
        assertEquals(2, freeVar2.getFreeVariables().size());
        assertTrue(freeVar2.getFreeVariables().contains("a"));
        assertTrue(freeVar2.getFreeVariables().contains("z"));
    }

    @Test
    public void totalTranslationCallTest() {
        assertEquals("x = y = z", slt.translate( "\\begin{align} x &= y \\\\ &= z \\end{align}" ));
        TranslationInformation ti = slt.getTranslationInformationObject();
        assertEquals(0, ti.getPartialTranslations().size());
    }

    @Test
    public void eqArrayWithConstraintsTest() {
        String test = "\\begin{align} x &= y, \\quad x > z\\\\ x &= z, \\quad x < z\\end{align}";
        TranslationInformation ti = slt.translateToObject(test);
        assertEquals("x = y ; x = z ", ti.getTranslatedExpression());
        assertEquals(2, ti.getTranslatedConstraints().size(), ti.getTranslatedConstraints().toString());

        List<TranslationInformation> parts = ti.getPartialTranslations();
        assertEquals(2, parts.size(), parts.toString());

        TranslationInformation firstPart = parts.get(0);
        assertEquals("x = y ", firstPart.getTranslatedExpression());
        assertEquals(1, firstPart.getTranslatedConstraints().size());
        assertEquals("x > z", firstPart.getTranslatedConstraints().get(0));
        FreeVariables freeVar1 = firstPart.getFreeVariables();
        assertEquals(3, freeVar1.getFreeVariables().size());
        assertTrue(freeVar1.getFreeVariables().contains("x"));
        assertTrue(freeVar1.getFreeVariables().contains("y"));
        assertTrue(freeVar1.getFreeVariables().contains("z"));

        TranslationInformation secondPart = parts.get(1);
        assertEquals("x = z ", secondPart.getTranslatedExpression());
        assertEquals(1, secondPart.getTranslatedConstraints().size());
        assertEquals("x < z", secondPart.getTranslatedConstraints().get(0));
        FreeVariables freeVar2 = secondPart.getFreeVariables();
        assertEquals(2, freeVar2.getFreeVariables().size());
        assertTrue(freeVar2.getFreeVariables().contains("x"));
        assertTrue(freeVar2.getFreeVariables().contains("z"));
    }

    @Test
    public void partialTranslationTest() {
        assertEquals("x = y; x = z", slt.translate( "\\begin{align} x &= y \\\\ x &= z \\end{align}" ));
        TranslationInformation ti = slt.getTranslationInformationObject();
        assertEquals(2, ti.getPartialTranslations().size());

        FreeVariables freeVar1 = ti.getPartialTranslations().get(0).getFreeVariables();
        assertEquals(2, freeVar1.getFreeVariables().size());
        assertTrue(freeVar1.getFreeVariables().contains("x"));
        assertTrue(freeVar1.getFreeVariables().contains("y"));

        FreeVariables freeVar2 = ti.getPartialTranslations().get(1).getFreeVariables();
        assertEquals(2, freeVar2.getFreeVariables().size());
        assertTrue(freeVar2.getFreeVariables().contains("x"));
        assertTrue(freeVar2.getFreeVariables().contains("z"));
    }

    @Test
    public void complexRealTest() {
        String input = "\\begin{align}&2n (n + \\alpha + \\beta) \\\\&= \\JacobipolyP{\\alpha}{\\beta}{n-1}@{z} - 2 (n+\\alpha - 1),\\end{align}";
        String translation = slt.translate( input );
        assertEquals("2*n*(n + alpha + beta) = JacobiP(n - 1, alpha, beta, z)- 2*(n + alpha - 1)", translation);
        TranslationInformation ti = slt.getTranslationInformationObject();
        assertEquals(0, ti.getPartialTranslations().size());

        FreeVariables vars = ti.getFreeVariables();
        Set<String> varsStr = vars.getFreeVariables();
        assertTrue(varsStr.contains("n"));
        assertTrue(varsStr.contains("alpha"));
        assertTrue(varsStr.contains("beta"));
        assertTrue(varsStr.contains("z"));
        assertEquals(4, varsStr.size());

        assertEquals(input, ti.getExpression());
        assertEquals(slt.getInfoLogger().toString(), ti.getTranslationInformation().toString());

        assertTrue( ti.getRequiredPackages().isEmpty() );
        assertTrue( ti.getTranslatedConstraints().isEmpty() );
    }

    @Test
    public void complexRealQuadTest() {
        String input = "\\begin{align}&2n (n + \\alpha + \\beta) \\\\ &\\qquad = \\JacobipolyP{\\alpha}{\\beta}{n-1}@{z} - 2 (n+\\alpha - 1),\\end{align}";
        String translation = slt.translate( input );
        assertEquals("2*n*(n + alpha + beta) = JacobiP(n - 1, alpha, beta, z)- 2*(n + alpha - 1)", translation);
        TranslationInformation ti = slt.getTranslationInformationObject();
        assertEquals(0, ti.getPartialTranslations().size());

        FreeVariables vars = ti.getFreeVariables();
        Set<String> varsStr = vars.getFreeVariables();
        assertTrue(varsStr.contains("n"));
        assertTrue(varsStr.contains("alpha"));
        assertTrue(varsStr.contains("beta"));
        assertTrue(varsStr.contains("z"));
        assertEquals(4, varsStr.size());

        assertEquals(input, ti.getExpression());
        assertEquals(slt.getInfoLogger().toString(), ti.getTranslationInformation().toString());

        assertTrue( ti.getRequiredPackages().isEmpty() );
        assertTrue( ti.getTranslatedConstraints().isEmpty() );
    }

    @Resource({"MultiEquationArray.tex", "MultiEquationArrayMapleTranslation.txt"})
    public void massiveMultilineEquationTest(String source, String solution) {
        String translation = slt.translate(source);
        assertEquals(solution, translation);
    }
}
