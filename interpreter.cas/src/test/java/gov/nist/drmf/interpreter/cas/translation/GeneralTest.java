package gov.nist.drmf.interpreter.cas.translation;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Scanner;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Created by AndreG-P on 12.03.2017.
 */
public class GeneralTest {

    @Test
    public void testParenthesisChecker1(){
        assertFalse( AbstractTranslator.testBrackets("(1)/(2)") );
    }

    @Test
    public void testParenthesisChecker2(){
        assertTrue( AbstractTranslator.testBrackets("((1)/((2)^(3) + 4*(5)^(6)))") );
    }

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
}