package gov.nist.drmf.interpreter.cas.translation;

import gov.nist.drmf.interpreter.common.InformationLogger;
import gov.nist.drmf.interpreter.common.TranslationInformation;
import gov.nist.drmf.interpreter.common.constants.Keys;
import gov.nist.drmf.interpreter.common.exceptions.InitTranslatorException;
import gov.nist.drmf.interpreter.common.latex.RelationalComponents;
import gov.nist.drmf.interpreter.common.meta.DLMF;
import gov.nist.drmf.interpreter.pom.common.meta.AssumeMLPAvailability;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

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
    void eqArrayCombinedTest() {
        slt.translate("\\begin{align}&2n (n + \\alpha + \\beta) \\\\&= \\JacobipolyP{\\alpha}{\\beta}{n-1}@{z} - 2 (n+\\alpha - 1),\\end{align}");
        TranslationInformation ti = slt.getTranslationInformationObject();
        test( ti,
                gen("2*n*(n + \\[Alpha]+ \\[Beta])", "JacobiP[n - 1, \\[Alpha], \\[Beta], z]- 2*(n + \\[Alpha]- 1)"),
                gen("=") );
        List<TranslationInformation> subEqs = ti.getPartialTranslations();
        assertEquals(0, subEqs.size());
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
            assertEquals(parts[i], comps.getComponents().get(i), "Expected to relation part '" + parts[i] + "' but got " + comps.getComponents().get(i));

        for ( int i = 0; i < rel.length; i++ )
            assertEquals(rel[i], comps.getRelations().get(i).getSymbol(), "Expected to relation part '" + rel[i] + "' but got " + comps.getRelations().get(i));
    }

    private static String[] gen(String... p) {
        return p;
    }
}
