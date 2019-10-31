package gov.nist.drmf.interpreter.cas.logging;

import org.junit.jupiter.api.Test;

import java.util.LinkedList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Andre Greiner-Petter
 */
public class TranslatedExpressionTests {
    @Test
    public void simpleSplittingTest() {
        TranslatedExpression te = generateTE(
                "3*", "x", "+", "y"
        );

        LinkedList<String> vars = new LinkedList<>();
        vars.add("x");

        TranslatedExpression extracted = te.removeUntilLastAppearanceOfVar(vars, "*");
        assertEquals("+y", te.toString());
        assertEquals("3*x", extracted.toString());
    }

    @Test
    public void divisonTest() {
        TranslatedExpression te = generateTE(
                "r*", "cos(Theta)*", "r*", "(3*r^2)", "/", "23*", "x",
                "+", "3*", "q"
        );

        LinkedList<String> vars = new LinkedList<>();
        vars.add("r");

        TranslatedExpression extracted = te.removeUntilLastAppearanceOfVar(vars, "*");
        assertEquals("+3*q", te.toString());
        assertEquals("r*cos(Theta)*r*(3*r^2)/23*x", extracted.toString());
    }

    @Test
    public void regexTest() {
        String regex = "^(?:.*[^\\p{Alpha}]|\\s*)(r)(?:[^\\p{Alpha}].*|\\s*)$";
        String match = "r*";
        String match2 = "*r";
        String match3 = "cos(r*a)";
        String match4 = "rud(2)";
        assertTrue(match.matches(regex));
        assertTrue(match2.matches(regex));
        assertTrue(match3.matches(regex));
        assertTrue(!match4.matches(regex));
    }

    private TranslatedExpression generateTE(String... elements) {
        TranslatedExpression te = new TranslatedExpression();
        for ( String e : elements ) te.addTranslatedExpression(e);
        return te;
    }

}
