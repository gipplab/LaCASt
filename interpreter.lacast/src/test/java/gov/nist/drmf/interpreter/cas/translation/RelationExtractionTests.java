package gov.nist.drmf.interpreter.cas.translation;

import gov.nist.drmf.interpreter.common.InformationLogger;
import gov.nist.drmf.interpreter.common.TranslationInformation;
import gov.nist.drmf.interpreter.common.constants.Keys;
import gov.nist.drmf.interpreter.common.exceptions.InitTranslatorException;
import gov.nist.drmf.interpreter.common.latex.RelationalComponents;
import gov.nist.drmf.interpreter.common.meta.DLMF;
import gov.nist.drmf.interpreter.common.tests.Resource;
import gov.nist.drmf.interpreter.pom.common.meta.AssumeMLPAvailability;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Andre Greiner-Petter
 */
@AssumeMLPAvailability
public class RelationExtractionTests {
    private static SemanticLatexTranslator slt;

    @BeforeAll
    public static void setup() throws InitTranslatorException {
        slt = new SemanticLatexTranslator(Keys.KEY_MATHEMATICA);
    }

    @Test
    void singleLetterTest() {
        slt.translate("a");
        test(slt.getTranslationInformation(), gen("a"), gen());
    }

    @Test
    void sequenceLetterTest() {
        slt.translate("a + b");
        test(slt.getTranslationInformation(), gen("a + b"), gen());
    }

    @Test
    void simpleInequalityTest() {
        slt.translate("3 > 2");
        test(slt.getTranslationInformation(), gen("3", "2"), gen(">"));
    }

    @Test
    void noRelationParenthesisTest() {
        slt.translate("3 < 2 >");
        test(slt.getTranslationInformation(), gen("3*(2)"), gen());
    }

    @Test
    void simpleInequalityReverseTest() {
        slt.translate("x + 2 < 3");
        test(slt.getTranslationInformation(), gen("x + 2", "3"), gen("<"));
    }

    @Test
    void multiEquationTest() {
        slt.translate("x = y < z");
        test(slt.getTranslationInformation(), gen("x", "y", "z"), gen("=", "<"));
    }

    @Test
    void multiRelationTest() {
        slt.translate("x < y < z");
        test(slt.getTranslationInformation(), gen("x", "y", "z"), gen("<", "<"));
    }

    @Test
    void sumTest() {
        slt.translate("\\sum_{n=0}^{10} n = 2 n");
        test(slt.getTranslationInformation(),
                gen("Sum[n, {n, 0, 10}, GenerateConditions->None]", "2*n"),
                gen("="));
    }

    @Test
    @DLMF("4.21.2")
    void simpleEqualityTest() {
        slt.translate("\\sin@{u + v}=\\sin@@{u}\\cos@@{v} + \\cos@@{u}\\sin@@{v}");
        test(slt.getTranslationInformation(), gen("Sin[u + v]", "Sin[u]*Cos[v]+ Cos[u]*Sin[v]"), gen("="));
    }

    @Test
    @DLMF("9.2.8")
    void equalityTest() {
        slt.translate("\\Wronskian@{\\AiryAi@{z}, \\AiryAi@{z \\expe^{-2 \\cpi \\iunit/3}}} = \\frac{\\expe^{\\cpi \\iunit /6}}{2\\cpi}");
        test(slt.getTranslationInformation(), gen(
                "Wronskian[{AiryAi[z], AiryAi[z*Exp[- 2*Pi*I/3]]}, z]",
                "Divide[Exp[Pi*I/6],2*Pi]"
                ),
                gen("="));
    }

    @Test
    void eqArrayTest() {
        slt.translate("\\begin{align} x &= y, \\quad a > b\\\\ v &= w, \\quad c < d\\end{align}");
        TranslationInformation ti = slt.getTranslationInformationObject();
        // well, how should we handle that situation anyhow?
        // i think the right way to go is to ignore top level relational components. That means
        // TI with: x = y; x = z does not have relational components. instead the subexpressions should have
        // that means: x = y has: x, y with [=], 
        // and x = z has: x, z with [=]

        test( ti, gen(), gen() );
        List<TranslationInformation> subEqs = ti.getPartialTranslations();
        assertEquals(2, subEqs.size());
        test( subEqs.get(0), gen("x", "y"), gen("=") );
        test( subEqs.get(1), gen("v", "w"), gen("=") );
    }

    @Test
    void longMultiEquationSimpleTest() {
        slt.translate("\\cos t = \\frac{\\cot t}{\\sqrt{1 + \\cot^2t}} = \\frac{-ma}{\\sqrt{m^2 a^2 + b^2}},\\quad\\quad\\sin t = \\frac{1}{\\sqrt{1 + \\cot^2t}} = \\frac{b}{\\sqrt{m^2 a^2 + b^2}}");
        test(slt.getTranslationInformation(), gen(
                "Cos[t]",
                "Divide[Cot[t],Sqrt[1 + (Cot[t])^(2)]]",
                "Divide[- m*a,Sqrt[(m)^(2)* (a)^(2)+ (b)^(2)]]"
                ),
                gen("=", "="));
    }

    @Test
    void longMultiEquationTest() {
        slt.translate("\\cos t = \\frac{\\cot t}{\\pm\\sqrt{1 + \\cot^2t}} = \\frac{-ma}{\\pm\\sqrt{m^2 a^2 + b^2}},\\quad\\quad\\sin t = \\frac{1}{\\pm\\sqrt{1 + \\cot^2t}} = \\frac{b}{\\pm\\sqrt{m^2 a^2 + b^2}}");
        test(slt.getTranslationInformation(), gen(
                "Cos[t]",
                "Divide[Cot[t],\\[PlusMinus]Sqrt[1 + (Cot[t])^(2)]]",
                "Divide[- m*a,\\[PlusMinus]Sqrt[(m)^(2)* (a)^(2)+ (b)^(2)]]"
                ),
                gen("=", "="));
        List<String> constraints = slt.getTranslationInformation().getTranslatedConstraints();
        System.out.println(constraints);
    }

    @Test
    void eqArrayCombinedTest() {
        slt.translate("\\begin{align}&2n (n + \\alpha + \\beta) \\\\&= \\JacobipolyP{\\alpha}{\\beta}{n-1}@{z} - 2 (n+\\alpha - 1),\\end{align}");
        TranslationInformation ti = slt.getTranslationInformationObject();
        test( ti,
                gen("2*n*(n + \\[Alpha]+ \\[Beta])", "JacobiP[n - 1, \\[Alpha], \\[Beta], z]- 2*(n + \\[Alpha]- 1)"),
                gen("=") );
        List<TranslationInformation> subEqs = ti.getPartialTranslations();
        assertEquals(0, subEqs.size());
    }

    @Resource("components/MultiEquationArray.tex")
    public void massiveMultilineEquationTest(String latex) {
        TranslationInformation ti = slt.translateToObject(latex);
        test( ti, gen(
                "(z - 1)*Divide[d,d*z]*JacobiP[\\[Beta], n, \\[Alpha], z]",
                "Divide[1,2]*(z - 1)*(1 + \\[Alpha]+ \\[Beta]+ n)*JacobiP[\\[Beta]+ 1, n - 1, \\[Alpha]+ 1, z]",
                "n*JacobiP[\\[Beta], n, \\[Alpha], z]-(\\[Alpha]+ n)*JacobiP[\\[Beta]+ 1, n - 1, \\[Alpha], z]",
                "(1 + \\[Alpha]+ \\[Beta]+ n)*(JacobiP[\\[Beta]+ 1, n, \\[Alpha], z]- JacobiP[\\[Beta], n, \\[Alpha], z])" ,
                "(\\[Alpha]+ n)*JacobiP[\\[Beta]+ 1, n, \\[Alpha]- 1, z]- \\[Alpha]*JacobiP[\\[Beta], n, \\[Alpha], z]",
                "Divide[2*(n + 1)*JacobiP[\\[Beta]- 1, n + 1, \\[Alpha], z]-(z*(1 + \\[Alpha]+ \\[Beta]+ n)+ \\[Alpha]+ 1 + n - \\[Beta])*JacobiP[\\[Beta], n, \\[Alpha], z],1 + z]",
                "Divide[(2*\\[Beta]+ n + n*z)*JacobiP[\\[Beta], n, \\[Alpha], z]- 2*(\\[Beta]+ n)*JacobiP[\\[Beta]- 1, n, \\[Alpha], z],1 + z]",
                "Divide[1 - z,1 + z]*(\\[Beta]*JacobiP[\\[Beta], n, \\[Alpha], z]-(\\[Beta]+ n)*JacobiP[\\[Beta]- 1, n, \\[Alpha]+ 1, z])"
        ), gen(
                "=", "=", "=", "=", "=", "=", "="
        ) );
    }

    private static void test(TranslationInformation info, String[] parts, String[] rel) {
        RelationalComponents comps = info.getRelationalComponents();
        assertNotNull(comps);
        if ( parts.length == 0 ) return;
        assertTrue(comps.getComponents().size() > 0, "No components extracted at all is not valid");
        assertEquals(comps.getComponents().size() - 1, comps.getRelations().size(),
                "Number of components does not match number of relations: " + comps.getComponents() + " vs " + comps.getRelations());
        assertEquals(parts.length, comps.getComponents().size(), "Expected " + Arrays.toString(parts) + " but got " + comps.getComponents());
        assertEquals(rel.length, comps.getRelations().size(), "Expected " + Arrays.toString(rel) + " but got " + comps.getRelations());

        for ( int i = 0; i < parts.length; i++ )
            assertEquals(parts[i], comps.getComponents().get(i), "Expected the relation part '" + parts[i] + "' but got " + comps.getComponents().get(i));

        for ( int i = 0; i < rel.length; i++ )
            assertEquals(rel[i], comps.getRelations().get(i).getSymbol(), "Expected the relation part '" + rel[i] + "' but got " + comps.getRelations().get(i));
    }

    private static String[] gen(String... p) {
        return p;
    }
}
