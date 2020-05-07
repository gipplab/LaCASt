package gov.nist.drmf.interpreter.cas.translation.components;

import gov.nist.drmf.interpreter.cas.logging.TranslatedExpression;
import gov.nist.drmf.interpreter.cas.translation.SemanticLatexTranslator;
import gov.nist.drmf.interpreter.common.constants.GlobalPaths;
import gov.nist.drmf.interpreter.common.constants.Keys;
import gov.nist.drmf.interpreter.common.exceptions.TranslationException;
import gov.nist.drmf.interpreter.common.grammar.MathTermTags;
import gov.nist.drmf.interpreter.mlp.MLPWrapper;
import gov.nist.drmf.interpreter.mlp.FakeMLPGenerator;
import gov.nist.drmf.interpreter.mlp.SemanticMLPWrapper;
import mlp.ParseException;
import mlp.PomTaggedExpression;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Andre Greiner-Petter
 */
public class MathTermTranslatorTests {

    private static SemanticLatexTranslator mapleTranslator;
    private MathTermTranslator mathTermTranslator;

    @BeforeAll
    public static void setup() throws IOException {
        mapleTranslator = new SemanticLatexTranslator(Keys.KEY_MAPLE);
        mapleTranslator.init(GlobalPaths.PATH_REFERENCE_DATA);
    }

    @BeforeEach
    public void setupTranslator() {
        mathTermTranslator = new MathTermTranslator(mapleTranslator);
    }

    @Test
    public void expectWrongTranslatorFuncTest() {
        PomTaggedExpression funcPTE = FakeMLPGenerator.generateMathTermEmptyPTE(
                MathTermTags.function, "cos"
        );
        assertThrows(TranslationException.class, () -> mathTermTranslator.translate(funcPTE));
    }

    @Test
    public void expectWrongTranslatorDelimiterTest() {
        PomTaggedExpression delimiterPTE = FakeMLPGenerator.generateMathTermEmptyPTE(
                MathTermTags.left_delimiter, "\\left("
        );
        assertThrows(TranslationException.class, () -> mathTermTranslator.translate(delimiterPTE));
    }

    @Test
    public void expectWrongTranslatorAbbreviationTest() {
        PomTaggedExpression pte = FakeMLPGenerator.generateMathTermEmptyPTE(
                MathTermTags.abbreviation, "etc."
        );
        assertThrows(TranslationException.class, () -> mathTermTranslator.translate(pte));
    }

    @Test
    public void expectWrongTranslatorMacroTest() {
        PomTaggedExpression pte = FakeMLPGenerator.generateMathTermEmptyPTE(
                MathTermTags.macro, "\\customMacro"
        );
        assertThrows(TranslationException.class, () -> mathTermTranslator.translate(pte));
    }

    @Test
    public void expectWrongTranslatorEmptyTagTest() {
        PomTaggedExpression emptyPTE = FakeMLPGenerator.generateEmptySequencePTE();
        assertThrows(TranslationException.class, () -> mathTermTranslator.translate(emptyPTE));
    }

    @Test
    public void translateRelationTest() {
        PomTaggedExpression pte = FakeMLPGenerator.generateMathTermEmptyPTE(
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
