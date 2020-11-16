package gov.nist.drmf.interpreter.cas.translation;

import gov.nist.drmf.interpreter.cas.logging.TranslatedExpression;
import gov.nist.drmf.interpreter.cas.translation.components.MathTermTranslator;
import gov.nist.drmf.interpreter.common.constants.Keys;
import gov.nist.drmf.interpreter.common.exceptions.InitTranslatorException;
import gov.nist.drmf.interpreter.common.exceptions.TranslationException;
import gov.nist.drmf.interpreter.pom.common.grammar.MathTermTags;
import gov.nist.drmf.interpreter.pom.MLPWrapper;
import gov.nist.drmf.interpreter.pom.common.FakeMLPGenerator;
import gov.nist.drmf.interpreter.pom.SemanticMLPWrapper;
import mlp.ParseException;
import mlp.PomTaggedExpression;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Andre Greiner-Petter
 */
public class MathTermTranslatorTests {

    private static SemanticLatexTranslator mapleTranslator;
    private MathTermTranslator mathTermTranslator;

    @BeforeAll
    public static void setup() throws InitTranslatorException {
        mapleTranslator = new SemanticLatexTranslator(Keys.KEY_MAPLE);
    }

    @BeforeEach
    public void setupTranslator() {
        mathTermTranslator = new MathTermTranslator(mapleTranslator);
    }

    @Test
    public void expectWrongTranslatorFuncTest() {
        PomTaggedExpression funcPTE = FakeMLPGenerator.generateMathTermEmptyPPTE(
                MathTermTags.function, "cos"
        );
        assertThrows(TranslationException.class, () -> mathTermTranslator.translate(funcPTE));
    }

    @Test
    public void expectWrongTranslatorDelimiterTest() {
        PomTaggedExpression delimiterPTE = FakeMLPGenerator.generateMathTermEmptyPPTE(
                MathTermTags.left_delimiter, "\\left("
        );
        assertThrows(TranslationException.class, () -> mathTermTranslator.translate(delimiterPTE));
    }

    @Test
    public void expectWrongTranslatorAbbreviationTest() {
        PomTaggedExpression pte = FakeMLPGenerator.generateMathTermEmptyPPTE(
                MathTermTags.abbreviation, "etc."
        );
        assertThrows(TranslationException.class, () -> mathTermTranslator.translate(pte));
    }

    @Test
    public void expectWarningTranslatorAbbreviationTest() {
        PomTaggedExpression pte = FakeMLPGenerator.generateMathTermEmptyPPTE(
                MathTermTags.abbreviation, "etc"
        );
        TranslatedExpression te = mathTermTranslator.translate(pte);
        assertEquals("e*t*c", te.toString());
        assertFalse(mathTermTranslator.getInfoLogger().isEmpty());
        assertTrue(mathTermTranslator.getInfoLogger().toString().contains("etc"));
    }

    @Test
    public void expectWrongTranslatorMacroTest() {
        PomTaggedExpression pte = FakeMLPGenerator.generateMathTermEmptyPPTE(
                MathTermTags.macro, "\\customMacro"
        );
        assertThrows(TranslationException.class, () -> mathTermTranslator.translate(pte));
    }

    @Test
    public void expectWrongTranslatorEmptyTagTest() {
        PomTaggedExpression emptyPTE = FakeMLPGenerator.generateEmptySequencePPTE();
        assertThrows(TranslationException.class, () -> mathTermTranslator.translate(emptyPTE));
    }

    @Test
    public void translateRelationTest() {
        PomTaggedExpression pte = FakeMLPGenerator.generateMathTermEmptyPPTE(
                MathTermTags.relation, "\\to"
        );
        TranslatedExpression teDirect = mathTermTranslator.translate(pte);
        TranslatedExpression teRetrieved = mathTermTranslator.getTranslatedExpressionObject();
        assertEquals(teDirect, teRetrieved);
        assertEquals("=", teDirect.toString());
    }

    /**
     * The different MLP versions are not consistent on how to parse fences.
     * In the current version, "fence" is never a primary tag of a MathTerm object.
     * However, it was often a primary tag before the current version.
     *
     * Hence this test, fakes an older version of MLP by replacing the primary tag.
     * The translation of the current and older version of MLP should not affect the
     * translation.
     *
     * @throws ParseException
     */
    @Test
    public void fakeFenceTest() throws ParseException {
        String fencesTestExpr = "|b-c|";
        String expectedResult = mapleTranslator.translate(fencesTestExpr);
        mapleTranslator.reset();

        MLPWrapper mlp = SemanticMLPWrapper.getStandardInstance();
        PomTaggedExpression sequencePTE = mlp.parse(fencesTestExpr);
        List<PomTaggedExpression> listPTE = sequencePTE.getComponents();

        PomTaggedExpression first = listPTE.get(0);
        first.getRoot().setTag(MathTermTags.fence.tag());

        PomTaggedExpression last = listPTE.get(listPTE.size()-1);
        last.getRoot().setTag(MathTermTags.fence.tag());

        TranslatedExpression te = mathTermTranslator.translate(listPTE.remove(0), listPTE);

        assertEquals(expectedResult, te.toString());
    }
}
