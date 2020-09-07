package gov.nist.drmf.interpreter.cas.translation;

import gov.nist.drmf.interpreter.common.constants.Keys;
import gov.nist.drmf.interpreter.common.exceptions.InitTranslatorException;
import gov.nist.drmf.interpreter.common.exceptions.TranslationException;
import gov.nist.drmf.interpreter.common.exceptions.TranslationExceptionReason;
import gov.nist.drmf.interpreter.common.meta.AssumeMLPAvailability;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Andre Greiner-Petter
 */
@AssumeMLPAvailability
public class ThrowTranslationExceptionTests {

    private static SemanticLatexTranslator slt;

    @BeforeAll
    static void setup() throws InitTranslatorException {
        slt = new SemanticLatexTranslator(Keys.KEY_MAPLE);
    }

    @Test
    void unknownMacroTest() {
        String macro = "\\notmacro";
        TranslationException te = assertThrows(
                TranslationException.class,
                () -> slt.translate(macro + "@{x}")
        );

        assertEquals(TranslationExceptionReason.LATEX_MACRO_ERROR, te.getReason());

        Object reason = te.getReasonObj();
        assertTrue(reason instanceof String);
        assertEquals(macro, reason.toString());
    }

    @Test
    void innerUnknownMacroTest() {
        String test = "\\frac{1}{\\notmacro@{x}}";
        TranslationException te = assertThrows(
                TranslationException.class,
                () -> slt.translate(test)
        );

        assertEquals(TranslationExceptionReason.LATEX_MACRO_ERROR, te.getReason());

        Object reason = te.getReasonObj();
        assertTrue(reason instanceof String);
        assertEquals("\\notmacro", reason.toString());
    }

    @Test
    void unavailableTranslationError() {
        String test = "\\frac{1}{\\Lattice{L}}";
        TranslationException te = assertThrows(
                TranslationException.class,
                () -> slt.translate(test)
        );

        assertEquals(TranslationExceptionReason.MISSING_TRANSLATION_INFORMATION, te.getReason());

        Object reason = te.getReasonObj();
        assertTrue(reason instanceof String);
        assertEquals("\\Lattice", reason.toString());
    }

    @Test
    void confusingPrimeError() {
        String test = "f^{\\prime}(x)";
        TranslationException te = assertThrows(
                TranslationException.class,
                () -> slt.translate(test)
        );

        te.printStackTrace();

        assertEquals(
                TranslationExceptionReason.INVALID_LATEX_INPUT,
                te.getReason()
        );

        assertNull(te.getReasonObj());
    }
}
