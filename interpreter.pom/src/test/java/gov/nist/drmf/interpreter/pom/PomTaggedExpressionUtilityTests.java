package gov.nist.drmf.interpreter.pom;

import gov.nist.drmf.interpreter.common.constants.Keys;
import gov.nist.drmf.interpreter.common.grammar.ExpressionTags;
import mlp.MathTerm;
import mlp.PomTaggedExpression;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Andre Greiner-Petter
 */
public class PomTaggedExpressionUtilityTests {
    @Test
    public void appropriateStringMathTermTest() {
        MathTerm mathTerm = new MathTerm("x");
        mathTerm.addNamedFeature(Keys.FEATURE_ACCENT, "overline, tilde");
        PomTaggedExpression pte = new PomTaggedExpression(mathTerm, ExpressionTags.accented.tag());
        String out = PomTaggedExpressionUtility.getAppropriateFontTex(pte);
        assertEquals("\\overline{\\tilde{x}}", out);
    }

    @Test
    public void appropriateStringMathTermAndExpressionTest() {
        MathTerm mathTerm = new MathTerm("x");
        mathTerm.addNamedFeature(Keys.FEATURE_ACCENT, "tilde");
        PomTaggedExpression pte = new PomTaggedExpression(mathTerm, ExpressionTags.accented.tag());
        pte.addNamedFeature(FeatureSetUtility.LATEX_FEATURE_KEY, "\\overline");
        String out = PomTaggedExpressionUtility.getAppropriateFontTex(pte);
        assertEquals("\\overline{\\tilde{x}}", out);
    }

    @Test
    public void appropriateDottedStringTest() {
        MathTerm mathTerm = new MathTerm("x");
        mathTerm.addNamedFeature(Keys.FEATURE_ACCENT, "1-dotted");
        PomTaggedExpression pte = new PomTaggedExpression(mathTerm, ExpressionTags.accented.tag());
        pte.addNamedFeature(FeatureSetUtility.LATEX_FEATURE_KEY, "\\overline");
        String out = PomTaggedExpressionUtility.getAppropriateFontTex(pte);
        assertEquals("\\overline{\\dot{x}}", out);
    }

    @Test
    public void appropriateStringMathTermAndExpressionAnnotationTest() {
        MathTerm mathTerm = new MathTerm("x");
        mathTerm.addNamedFeature(Keys.FEATURE_ACCENT, "overline, tilde");
        PomTaggedExpression pte = new PomTaggedExpression(mathTerm, ExpressionTags.accented.tag());
        pte.addNamedFeature(FeatureSetUtility.LATEX_FEATURE_KEY, "\\overline");
        String out = PomTaggedExpressionUtility.getAppropriateFontTex(pte);
        assertEquals("\\overline{\\tilde{x}}", out);
    }

    @Test
    public void sequenceAppropriateStringTest() {
        MathTerm mathTerm = new MathTerm("x");
        mathTerm.addNamedFeature(Keys.FEATURE_ACCENT, "tilde");
        PomTaggedExpression x = new PomTaggedExpression(mathTerm, ExpressionTags.accented.tag());
        PomTaggedExpression plus = new PomTaggedExpression(new MathTerm("+"));
        PomTaggedExpression y = new PomTaggedExpression(new MathTerm("y"));

        PomTaggedExpression pte = new PomTaggedExpression();
        pte.addComponent(x);
        pte.addComponent(plus);
        pte.addComponent(y);
        pte.setNamedFeature(Keys.FEATURE_ACCENT, "overline, overline");

        String out = PomTaggedExpressionUtility.getAppropriateFontTex(pte);
        assertEquals("\\overline{\\overline{}}", out);
    }
}
