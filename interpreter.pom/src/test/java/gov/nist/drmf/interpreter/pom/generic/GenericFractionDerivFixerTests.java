package gov.nist.drmf.interpreter.pom.generic;

import gov.nist.drmf.interpreter.pom.SemanticMLPWrapper;
import gov.nist.drmf.interpreter.pom.common.grammar.ExpressionTags;
import gov.nist.drmf.interpreter.pom.common.meta.AssumeMLPAvailability;
import gov.nist.drmf.interpreter.pom.extensions.PrintablePomTaggedExpression;
import gov.nist.drmf.interpreter.pom.generic.GenericFractionDerivFixer;
import mlp.ParseException;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Andre Greiner-Petter
 */
@AssumeMLPAvailability
public class GenericFractionDerivFixerTests {
    private static final SemanticMLPWrapper mlp = SemanticMLPWrapper.getStandardInstance();

    @Test
    void partialTest() throws ParseException {
        PrintablePomTaggedExpression derivPTE = mlp.parse("\\frac{\\partial}{\\partial z}");
        GenericFractionDerivFixer fixer = new GenericFractionDerivFixer(derivPTE);
        PrintablePomTaggedExpression fixedPTE = fixer.fixGenericDeriv();

        assertEquals(derivPTE, fixedPTE);
        assertEquals("\\deriv [1]{ }{z}", fixedPTE.getTexString());
        checkList( fixedPTE.getPrintableComponents(),
                "\\deriv", "[", "1", "]", "{ }", "{z}"
        );
    }

    @Test
    void simpleDerivTest() throws ParseException {
        PrintablePomTaggedExpression derivPTE = mlp.parse("\\frac{d}{dz}");
        GenericFractionDerivFixer fixer = new GenericFractionDerivFixer(derivPTE);
        PrintablePomTaggedExpression fixedPTE = fixer.fixGenericDeriv();

        assertEquals(derivPTE, fixedPTE);
        assertEquals("\\deriv [1]{ }{z}", fixedPTE.getTexString());
        checkList( fixedPTE.getPrintableComponents(),
                "\\deriv", "[", "1", "]", "{ }", "{z}"
        );
    }

    @Test
    void mathRmDerivTest() throws ParseException {
        PrintablePomTaggedExpression derivPTE = mlp.parse("\\frac{\\mathrm{d}}{\\mathrm{d}z}");
        assertTrue(ExpressionTags.fraction.equalsPTE(derivPTE));

        GenericFractionDerivFixer fixer = new GenericFractionDerivFixer(derivPTE);
        PrintablePomTaggedExpression fixedPTE = fixer.fixGenericDeriv();
        assertEquals(derivPTE, fixedPTE);
        assertEquals("\\deriv [1]{ }{z}", fixedPTE.getTexString());
        checkList( fixedPTE.getPrintableComponents(),
                "\\deriv", "[", "1", "]", "{ }", "{z}"
        );

        assertTrue(ExpressionTags.sequence.equalsPTE(fixedPTE));
    }

    @Test
    void alphaDerivTest() throws ParseException {
        PrintablePomTaggedExpression derivPTE = mlp.parse("\\frac{d}{d\\alpha}");
        GenericFractionDerivFixer fixer = new GenericFractionDerivFixer(derivPTE);
        PrintablePomTaggedExpression fixedPTE = fixer.fixGenericDeriv();

        assertEquals(derivPTE, fixedPTE);
        assertEquals("\\deriv [1]{ }{\\alpha}", fixedPTE.getTexString());
    }

    @Test
    void simpleDegreeTest() throws ParseException {
        PrintablePomTaggedExpression derivPTE = mlp.parse("\\frac{d^n}{dz^n}");
        GenericFractionDerivFixer fixer = new GenericFractionDerivFixer(derivPTE);
        PrintablePomTaggedExpression fixedPTE = fixer.fixGenericDeriv();

        assertEquals(derivPTE, fixedPTE);
        assertEquals("\\deriv [n]{ }{z}", fixedPTE.getTexString());
    }

    @Test
    void simpleDegreePartialTest() throws ParseException {
        PrintablePomTaggedExpression derivPTE = mlp.parse("\\frac{\\partial^n}{\\partial z^n}");
        GenericFractionDerivFixer fixer = new GenericFractionDerivFixer(derivPTE);
        PrintablePomTaggedExpression fixedPTE = fixer.fixGenericDeriv();

        assertEquals(derivPTE, fixedPTE);
        assertEquals("\\deriv [n]{ }{z}", fixedPTE.getTexString());
    }

    @Test
    void alphaDegreeTest() throws ParseException {
        PrintablePomTaggedExpression derivPTE = mlp.parse("\\frac{d^n}{d\\alpha^n}");
        GenericFractionDerivFixer fixer = new GenericFractionDerivFixer(derivPTE);
        PrintablePomTaggedExpression fixedPTE = fixer.fixGenericDeriv();

        assertEquals(derivPTE, fixedPTE);
        assertEquals("\\deriv [n]{ }{\\alpha}", fixedPTE.getTexString());
    }

    @Test
    void wrongDegreeMatchTest() throws ParseException {
        PrintablePomTaggedExpression derivPTE = mlp.parse("\\frac{d^n}{d\\alpha^m}");
        GenericFractionDerivFixer fixer = new GenericFractionDerivFixer(derivPTE);
        PrintablePomTaggedExpression fixedPTE = fixer.fixGenericDeriv();

        assertEquals(derivPTE, fixedPTE);
        assertEquals("\\frac{d^n}{d\\alpha^m}", fixedPTE.getTexString());
    }

    @Test
    void halfDegreeMatchTest() throws ParseException {
        PrintablePomTaggedExpression derivPTE = mlp.parse("\\frac{d}{d\\alpha^n}");
        GenericFractionDerivFixer fixer = new GenericFractionDerivFixer(derivPTE);
        PrintablePomTaggedExpression fixedPTE = fixer.fixGenericDeriv();

        assertEquals(derivPTE, fixedPTE);
        assertEquals("\\frac{d}{d\\alpha^n}", fixedPTE.getTexString());
    }

    @Test
    void balancedCurlyBracketsTest() throws ParseException {
        PrintablePomTaggedExpression derivPTE = mlp.parse("x + \\frac{d^n}{dz^n} \\left\\{ (1-z)^\\alpha \\left (1 - z \\right )^n \\right\\}");
        GenericFractionDerivFixer fixer = new GenericFractionDerivFixer(derivPTE);
        PrintablePomTaggedExpression fixedPTE = fixer.fixGenericDeriv();

        assertEquals(derivPTE, fixedPTE);
        assertEquals("x + \\deriv [n]{ }{z} \\left\\{(1 - z)^\\alpha \\left (1 - z \\right )^n \\right\\}", fixedPTE.getTexString());
    }

    @Test
    void balancedCurlyBracketsPartialTest() throws ParseException {
        PrintablePomTaggedExpression derivPTE = mlp.parse("x + \\frac{\\partial^n}{\\partial z^n} \\left\\{ (1-z)^\\alpha \\left (1 - z \\right )^n \\right\\}");
        GenericFractionDerivFixer fixer = new GenericFractionDerivFixer(derivPTE);
        PrintablePomTaggedExpression fixedPTE = fixer.fixGenericDeriv();

        assertEquals(derivPTE, fixedPTE);
        assertEquals("x + \\deriv [n]{ }{z} \\left\\{(1 - z)^\\alpha \\left (1 - z \\right )^n \\right\\}", fixedPTE.getTexString());
    }

    @Test
    void derivArgTest() throws ParseException {
        PrintablePomTaggedExpression ppte = mlp.parse("x + \\frac{d^n }{ d z^n }  ( z^2 - 1 )^n");
        GenericFractionDerivFixer fixer = new GenericFractionDerivFixer(ppte);
        PrintablePomTaggedExpression newPPTE = fixer.fixGenericDeriv();

        assertEquals(ppte, newPPTE);
        assertEquals("x + \\deriv [n]{ }{z}(z^2 - 1)^n", newPPTE.getTexString());

        checkList( ppte.getPrintableComponents(),
                "x", "+", "\\deriv", "[", "n", "]", "{ }", "{z}", "(", "z", "^2", "-", "1", ")", "^n"
        );
    }

    @Test
    void derivArgPartialTest() throws ParseException {
        PrintablePomTaggedExpression ppte = mlp.parse("x + \\frac{\\partial^n }{ \\partial z^n }  ( z^2 - 1 )^n");
        GenericFractionDerivFixer fixer = new GenericFractionDerivFixer(ppte);
        PrintablePomTaggedExpression newPPTE = fixer.fixGenericDeriv();

        assertEquals(ppte, newPPTE);
        assertEquals("x + \\deriv [n]{ }{z}(z^2 - 1)^n", newPPTE.getTexString());

        checkList( ppte.getPrintableComponents(),
                "x", "+", "\\deriv", "[", "n", "]", "{ }", "{z}", "(", "z", "^2", "-", "1", ")", "^n"
        );
    }

    @Test
    void derivNonEmptyArgTest() throws ParseException {
        PrintablePomTaggedExpression ppte = mlp.parse("x + \\frac{d^n z^2}{ d z^n } ");
        GenericFractionDerivFixer fixer = new GenericFractionDerivFixer(ppte);
        PrintablePomTaggedExpression newPPTE = fixer.fixGenericDeriv();

        assertEquals(ppte, newPPTE);
        assertEquals("x + \\deriv [n]{z^2}{z}", newPPTE.getTexString());

        checkList( ppte.getPrintableComponents(),
                "x", "+", "\\deriv", "[", "n", "]", "{z^2}", "{z}"
        );
    }

    private void checkList(List<PrintablePomTaggedExpression> components, String... matches ) {
        assertEquals(matches.length, components.size(), "Length doesnt match: [" +
                components.stream().map(PrintablePomTaggedExpression::getTexString).collect(Collectors.joining(", ")) + "] VS " + Arrays.toString(matches));
        for ( int i = 0; i < matches.length; i++ ){
            assertEquals(matches[i], components.get(i).getTexString());
        }
    }
}
