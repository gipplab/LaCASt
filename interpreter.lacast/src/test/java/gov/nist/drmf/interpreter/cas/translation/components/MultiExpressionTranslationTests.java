package gov.nist.drmf.interpreter.cas.translation.components;

import gov.nist.drmf.interpreter.cas.logging.TranslatedExpression;
import gov.nist.drmf.interpreter.cas.translation.SemanticLatexTranslator;
import gov.nist.drmf.interpreter.common.TranslationInformation;
import gov.nist.drmf.interpreter.common.constants.Keys;
import gov.nist.drmf.interpreter.common.exceptions.InitTranslatorException;
import gov.nist.drmf.interpreter.pom.SemanticMLPWrapper;
import gov.nist.drmf.interpreter.pom.common.meta.AssumeMLPAvailability;
import mlp.ParseException;
import mlp.PomTaggedExpression;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
        assertEquals("x = y; y = z", te.toString());
        assertEquals(te, gt.getTranslatedExpressionObject());

        List<TranslatedExpression> additionalTranslations = gt.getListOfPartialTranslations();
        assertEquals(2, additionalTranslations.size(), additionalTranslations.toString());
        assertEquals("x = y", additionalTranslations.get(0).getTranslatedExpression());
        assertEquals("y = z", additionalTranslations.get(1).getTranslatedExpression());
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
    }

    @Test
    public void totalTranslationCallTest() {
        assertEquals("x = y; y = z", slt.translate( "\\begin{align} x &= y \\\\ &= z \\end{align}" ));
        TranslationInformation ti = slt.getTranslationInformationObject();
        assertEquals(2, ti.getPartialTranslations().size());
        assertEquals("x = y", ti.getPartialTranslations().get(0).getTranslatedExpression());
        assertEquals("y = z", ti.getPartialTranslations().get(1).getTranslatedExpression());
    }
}
