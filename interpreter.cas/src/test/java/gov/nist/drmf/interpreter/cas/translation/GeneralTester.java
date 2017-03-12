package gov.nist.drmf.interpreter.cas.translation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Created by AndreG-P on 12.03.2017.
 */
public class GeneralTester {

    @Test
    public void testParenthesisChecker1(){
        assertFalse( AbstractTranslator.testBrackets("(1)/(2)") );
    }

    @Test
    public void testParenthesisChecker2(){
        assertTrue( AbstractTranslator.testBrackets("((1)/(2))") );
    }
}