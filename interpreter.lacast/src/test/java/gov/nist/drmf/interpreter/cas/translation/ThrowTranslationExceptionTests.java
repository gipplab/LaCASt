package gov.nist.drmf.interpreter.cas.translation;

import gov.nist.drmf.interpreter.common.constants.Keys;
import gov.nist.drmf.interpreter.common.exceptions.InitTranslatorException;
import gov.nist.drmf.interpreter.common.exceptions.TranslationException;
import gov.nist.drmf.interpreter.common.exceptions.TranslationExceptionReason;
import gov.nist.drmf.interpreter.pom.common.meta.AssumeMLPAvailability;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

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

    @Test
    void intOverSetIsNotSupportedTest() {
        String in = "\\iint_{D}\\|\\mathbf{T}_{u}\\times\\mathbf{T}_{v}\\|\\diff{u}\\diff{v}";
        TranslationException te = assertThrows(
                TranslationException.class,
                () -> slt.translate(in)
        );

        assertEquals(
                TranslationExceptionReason.MISSING_TRANSLATION_INFORMATION,
                te.getReason()
        );
        assertNull(te.getReasonObj());
    }

    @Test
    void noArgumentForSemanticMacroTest() {
        String in = "\\deriv{\\LambertW}{x} = \\frac{e^{-\\LambertW}}{1+\\LambertW}";
        TranslationException te = assertThrows(
                TranslationException.class,
                () -> slt.translate(in)
        );

        assertEquals(
                TranslationExceptionReason.DLMF_MACRO_ERROR,
                te.getReason()
        );
        assertEquals("\\LambertW", te.getReasonObj());
    }

    @Test
    void parenthesisMismatchTest() {
        String expr = "x + ( y";
        TranslationException te = assertThrows(
                TranslationException.class,
                () -> slt.translate(expr)
        );

        assertEquals(TranslationExceptionReason.WRONG_PARENTHESIS, te.getReason());
    }

    @Test
    void parenthesisMismatchReverseTest() {
        String expr = "x + ) y";
        TranslationException te = assertThrows(
                TranslationException.class,
                () -> slt.translate(expr)
        );

        assertEquals(TranslationExceptionReason.WRONG_PARENTHESIS, te.getReason());
    }

    /**
     * It's kind of debatable. LaCASt would be more robust when we only allow arguments in curly brackets.
     * For example, <code>\cos(x)</code> should throw an error, because the argument is not strictly provided
     * in curly brackets (strict hierarchy in the parse tree).
     *
     * However, sometimes the DLMF itself ignores this and uses macros to write just the barely minimum, e.g.,
     * <code>\LambertW</code> or <code>\HankelmodM{\nu}^{3}(x)</code> (latest from DLMF 10.18.13).
     *
     * Now, we throw a warning that people should wrap their arguments in curly brackets.
     * If you want to change that in the future, update the opposite test in
     * {@link SimpleTranslationTests#macroParenthesisTranslator()}.
     */
    @Test
    @Disabled
    void functionTest() {
        String in = "\\cos(x)";
        TranslationException te = assertThrows(
                TranslationException.class,
                () -> slt.translate(in)
        );

        assertEquals(
                TranslationExceptionReason.DLMF_MACRO_ERROR,
                te.getReason()
        );
    }
}
