package gov.nist.drmf.interpreter.generic.common;

import gov.nist.drmf.interpreter.pom.SemanticMLPWrapper;
import gov.nist.drmf.interpreter.pom.common.meta.AssumeMLPAvailability;
import gov.nist.drmf.interpreter.pom.extensions.PrintablePomTaggedExpression;
import mlp.ParseException;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Andre Greiner-Petter
 */
@AssumeMLPAvailability
public class GenericFractionDerivFixerTests {
    private static final SemanticMLPWrapper mlp = SemanticMLPWrapper.getStandardInstance();

    @Test
    void simpleDerivTest() throws ParseException {
        PrintablePomTaggedExpression derivPTE = mlp.parse("\\frac{d}{dz}");
        GenericFractionDerivFixer fixer = new GenericFractionDerivFixer(derivPTE);
        PrintablePomTaggedExpression fixedPTE = fixer.fixGenericDeriv();

        assertEquals(derivPTE, fixedPTE);
        assertEquals("\\deriv [1]{ }{z}", fixedPTE.getTexString());
    }

    @Test
    void mathRmDerivTest() throws ParseException {
        PrintablePomTaggedExpression derivPTE = mlp.parse("\\frac{\\mathrm{d}}{\\mathrm{d}z}");
        GenericFractionDerivFixer fixer = new GenericFractionDerivFixer(derivPTE);
        PrintablePomTaggedExpression fixedPTE = fixer.fixGenericDeriv();

        assertEquals(derivPTE, fixedPTE);
        assertEquals("\\deriv [1]{ }{z}", fixedPTE.getTexString());
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
        PrintablePomTaggedExpression derivPTE = mlp.parse("\\frac{d^n}{dz^n} \\left\\{ (1-z)^\\alpha \\left (1 - z \\right )^n \\right\\}");
        GenericFractionDerivFixer fixer = new GenericFractionDerivFixer(derivPTE);
        PrintablePomTaggedExpression fixedPTE = fixer.fixGenericDeriv();

        assertEquals(derivPTE, fixedPTE);
        assertEquals("\\deriv [n]{ }{z} \\left\\{(1 - z)^\\alpha \\left (1 - z \\right )^n \\right\\}", fixedPTE.getTexString());
    }

    @Test
    @Disabled
    void derivArgTest() throws ParseException {
        PrintablePomTaggedExpression ppte = mlp.parse("\\frac{d^n }{ d z^n }  ( z^2 - 1 )^n");
        GenericFractionDerivFixer fixer = new GenericFractionDerivFixer(ppte);
        PrintablePomTaggedExpression newPPTE = fixer.fixGenericDeriv();

        assertEquals(ppte, newPPTE);
        assertEquals("\\deriv [n]{ }{z} (z^2 - 1)^n", newPPTE.getTexString());

        checkList( ppte.getPrintableComponents(),
                "\\deriv", "[", "n", "]", "{ }", "{z}", "(", "z", "^2", "-", "1", ")", "^n"
        );

        // TODO deriv[]{}{} is a sequence object but should be in the same sequence... otherwise the translator cannot work properly
        // TODO merge definiens seems not working... i have no clue whats going on...
    }

    private void checkList(List<PrintablePomTaggedExpression> components, String... matches ) {
        assertEquals(matches.length, components.size(), "Length doesnt match: [" +
                components.stream().map(PrintablePomTaggedExpression::getTexString).collect(Collectors.joining(", ")) + "] VS " + Arrays.toString(matches));
        for ( int i = 0; i < matches.length; i++ ){
            assertEquals(matches[i], components.get(i).getTexString());
        }
    }
}
