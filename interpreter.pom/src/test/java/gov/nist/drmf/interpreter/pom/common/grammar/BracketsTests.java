package gov.nist.drmf.interpreter.pom.common.grammar;

import mlp.MathTerm;
import mlp.PomTaggedExpression;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Andre Greiner-Petter
 */
public class BracketsTests {
    @Test
    public void testParenthesisCheckerFalse(){
        assertFalse( Brackets.isEnclosedByBrackets("(1)/(2)") );
    }

    @Test
    public void testParenthesisCheckerTrue(){
        assertTrue( Brackets.isEnclosedByBrackets("((1)/((2)^(3) + 4*(5)^(6)))") );
    }

    @Test
    public void testParenthesisCheckerFalse2(){
        assertFalse( Brackets.isEnclosedByBrackets("\\left( x + 1 )") );
    }

    @Test
    public void testParenthesisCheckerTrue2(){
        assertTrue( Brackets.isEnclosedByBrackets("\\left( (x/2) + 1 \\right)") );
    }

    @Test
    public void testBracketChecker(){
        assertTrue( Brackets.isEnclosedByBrackets("\\left\\{ (x/2) + 1 \\right\\}") );
        assertTrue( Brackets.isEnclosedByBrackets("\\{ (x/2) + 1 \\}") );
        assertTrue( Brackets.isEnclosedByBrackets("{ (x/2) + 1 }") );
    }

    @Test
    public void testGetBracketClosed() {
        assertEquals(Brackets.right_parenthesis, Brackets.getBracket(")"));
    }

    @Test
    public void testGetBracketClosed2() {
        MathTerm mt = new MathTerm(")");
        assertEquals(Brackets.right_parenthesis, Brackets.getBracket(mt));
    }

    @Test
    public void testGetBracketClosed4() {
        MathTerm mt = new MathTerm(")");
        PomTaggedExpression pte = new PomTaggedExpression(mt);
        assertEquals(Brackets.right_parenthesis, Brackets.getBracket(pte));
    }
}
