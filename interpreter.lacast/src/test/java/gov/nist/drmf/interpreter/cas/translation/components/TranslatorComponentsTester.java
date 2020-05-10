package gov.nist.drmf.interpreter.cas.translation.components;

import gov.nist.drmf.interpreter.cas.logging.TranslatedExpression;
import gov.nist.drmf.interpreter.cas.translation.SemanticLatexTranslator;
import gov.nist.drmf.interpreter.common.constants.GlobalPaths;
import gov.nist.drmf.interpreter.common.constants.Keys;
import gov.nist.drmf.interpreter.common.exceptions.TranslationException;
import gov.nist.drmf.interpreter.mlp.SemanticMLPWrapper;
import mlp.ParseException;
import mlp.PomTaggedExpression;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Andre Greiner-Petter
 */
public class TranslatorComponentsTester {

    private static SemanticLatexTranslator slt;
    private static SemanticMLPWrapper mlp;

    @BeforeAll
    static void setup() throws IOException {
        slt = new SemanticLatexTranslator(Keys.KEY_MAPLE);
        slt.init(GlobalPaths.PATH_REFERENCE_DATA);
        mlp = SemanticMLPWrapper.getStandardInstance();
    }

    @Test
    public void greekLetterWithoutBackslash() throws ParseException {
        GreekLetterTranslator gt = new GreekLetterTranslator(slt);
        PomTaggedExpression pte = mlp.simpleParse("alpha");
        TranslatedExpression te = gt.translate(pte);
        assertEquals("alpha", te.toString());
        assertEquals(te, gt.getTranslatedExpressionObject());
    }

    @Test
    public void nonGreekLetterExceptionTest() throws ParseException {
        GreekLetterTranslator gt = new GreekLetterTranslator(slt);
        PomTaggedExpression pte = mlp.simpleParse("\\beta");
        pte.getRoot().setTermText("nonGreekLetter");
        TranslationException te = assertThrows(TranslationException.class, () -> gt.translate(pte) );
        assertTrue( te.getMessage().toLowerCase().contains("cannot translate greek letter") );
    }

    @Test
    public void nonGreekLetterExceptionTest2() throws ParseException {
        GreekLetterTranslator gt = new GreekLetterTranslator(slt);
        PomTaggedExpression pte = mlp.simpleParse("noGreekLetter");
        assertThrows(TranslationException.class, () -> gt.translate(pte) );
    }

    @Test
    public void macroPackageTranslatorTest() throws ParseException {
        MacroTranslator mt = new MacroTranslator(slt);
        PomTaggedExpression pte = mlp.simpleParse("\\qGamma{q}@{\\qfactorial{n}{q}}");
        List<PomTaggedExpression> elements = pte.getComponents();
        TranslatedExpression te = mt.translate(elements.remove(0), elements);
        assertFalse(te.getRequiredPackages().isEmpty());
        assertEquals("QGAMMA(q, QFactorial(n, q))", te.getTranslatedExpression());


    }
}
