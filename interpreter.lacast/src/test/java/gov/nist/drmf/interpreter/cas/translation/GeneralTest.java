package gov.nist.drmf.interpreter.cas.translation;

import gov.nist.drmf.interpreter.cas.blueprints.BlueprintRuleMatcher;
import gov.nist.drmf.interpreter.cas.common.DLMFPatterns;
import gov.nist.drmf.interpreter.common.TeXPreProcessor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by AndreG-P on 12.03.2017.
 */
public class GeneralTest {

    @Test
    public void startWithTest(){
        String op = "\\left(";
        assertTrue( op.matches( "\\\\(left|right).*" ) );
    }

    @Test
    public void scannerTest(){
        String in = "\\AiryBi@{x} = \\frac{3x^{5/4}} \\constraint{$x>0$}, \\label{eq:AI.IN.STB}";
        String[] carr = in.split("\\\\constraint");

        String eq = null;
        String con = null;
        String labelS = null;
        if ( carr.length > 1 ){
            String[] tmp = carr[1].split("\\\\label");
            eq = carr[0];
            con = tmp[0];
            labelS = tmp[1];
        } else {
            String[] larr = in.split("\\\\label");
            eq = larr[0];
            labelS = larr[1];
        }

        System.out.println(eq);
        System.out.println(con);
        System.out.println(labelS);
    }

    @Test
    public void stripParenthesesTest() {
        String simply = "(x+y)";
        String simplyOut = AbstractListTranslator.stripMultiParentheses(simply);
        assertEquals("x+y", simplyOut);

        String inner = "(x)/(y)";
        String innerOut = AbstractListTranslator.stripMultiParentheses(inner);
        assertEquals(inner, innerOut);
    }

    @Test
    public void texPreprocessing() {
        String l = "n \\hiderel{=} 1";
        String out = BlueprintRuleMatcher.preCleaning(l);
        assertEquals("n = 1", out);
        assertFalse("=".matches("\\\\in"));
    }

    @Test
    public void texPreprocessing2Test() {
        String l = "1.32 34 34";
        String out = TeXPreProcessor.preProcessingTeX(l);
        assertEquals("1.323434", out);
    }

    @Test
    public void matchDerivTest() {
        assertTrue("\\deriv".matches(DLMFPatterns.DERIV_NOTATION));
        assertTrue("\\tderiv".matches(DLMFPatterns.DERIV_NOTATION));
        assertTrue("\\ideriv".matches(DLMFPatterns.DERIV_NOTATION));
        assertTrue("\\pderiv".matches(DLMFPatterns.DERIV_NOTATION));
        assertTrue("\\tpderiv".matches(DLMFPatterns.DERIV_NOTATION));
        assertTrue("\\ipderiv".matches(DLMFPatterns.DERIV_NOTATION));
        assertFalse("\\tipderiv".matches(DLMFPatterns.DERIV_NOTATION));
        assertFalse("\\ptderiv".matches(DLMFPatterns.DERIV_NOTATION));
    }
}