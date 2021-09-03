package gov.nist.drmf.interpreter.pom.generic;

import gov.nist.drmf.interpreter.pom.SemanticMLPWrapper;
import gov.nist.drmf.interpreter.pom.common.meta.AssumeMLPAvailability;
import gov.nist.drmf.interpreter.pom.extensions.PrintablePomTaggedExpression;
import gov.nist.drmf.interpreter.pom.generic.GenericDifferentialDFixer;
import mlp.ParseException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Andre Greiner-Petter
 */
@AssumeMLPAvailability
public class GenericDifferentialDFixerTests {

    private static final SemanticMLPWrapper mlp = SemanticMLPWrapper.getStandardInstance();

    @Test
    void fixGenericDifferentialDTests() throws ParseException {
        PrintablePomTaggedExpression ppte = mlp.parse("\\int x dx");
        GenericDifferentialDFixer fixer = new GenericDifferentialDFixer();
        PrintablePomTaggedExpression newPPTE = fixer.fixDifferentialD(ppte);

        assertEquals(ppte, newPPTE);
        assertEquals("\\int x \\diff{x}", newPPTE.getTexString());
    }

    @Test
    void fixMultiDiffDTests() throws ParseException {
        PrintablePomTaggedExpression ppte = mlp.parse("\\int \\int x y dx dy");
        GenericDifferentialDFixer fixer = new GenericDifferentialDFixer();
        PrintablePomTaggedExpression newPPTE = fixer.fixDifferentialD(ppte);

        assertEquals(ppte, newPPTE);
        assertEquals("\\int \\int x y \\diff{x} \\diff{y}", newPPTE.getTexString());
    }

    @Test
    void multiDiffDTests() throws ParseException {
        PrintablePomTaggedExpression ppte = mlp.parse("\\int \\int xy dxdy");
        GenericDifferentialDFixer fixer = new GenericDifferentialDFixer();
        PrintablePomTaggedExpression newPPTE = fixer.fixDifferentialD(ppte);

        assertEquals(ppte, newPPTE);
        assertEquals("\\int \\int xy \\diff{x} \\diff{y}", newPPTE.getTexString());
    }

    @Test
    void fixGenericDifferentialDInFracTests() throws ParseException {
        PrintablePomTaggedExpression ppte = mlp.parse("\\int \\frac{dx}{x}");
        GenericDifferentialDFixer fixer = new GenericDifferentialDFixer();
        PrintablePomTaggedExpression newPPTE = fixer.fixDifferentialD(ppte);

        assertEquals(ppte, newPPTE);
        assertEquals("\\int \\frac{\\diff{x}}{x}", newPPTE.getTexString());
    }

    @Test
    void trickyFracTest() throws ParseException {
        PrintablePomTaggedExpression ppte = mlp.parse("\\int x \\frac{x + dx}{x} + 2");
        GenericDifferentialDFixer fixer = new GenericDifferentialDFixer();
        PrintablePomTaggedExpression newPPTE = fixer.fixDifferentialD(ppte);

        assertEquals(ppte, newPPTE);
        assertEquals("\\int x \\frac{x + \\diff{x}}{x} + 2", newPPTE.getTexString());
    }

    @Test
    void argumentIntTests() throws ParseException {
        PrintablePomTaggedExpression ppte = mlp.parse("\\int_{-1}^1 (1 - x) dx");
        GenericDifferentialDFixer fixer = new GenericDifferentialDFixer();
        PrintablePomTaggedExpression newPPTE = fixer.fixDifferentialD(ppte);

        assertEquals(ppte, newPPTE);
        assertEquals("\\int_{-1}^1(1 - x) \\diff{x}", newPPTE.getTexString());
    }

    @Test
    void inlineIntDsTest() throws ParseException {
        PrintablePomTaggedExpression ppte = mlp.parse("\\int_{-1}^1 e^{izs} (1 - s^2)^{\\nu-\\frac{1}{2}} ds");
        GenericDifferentialDFixer fixer = new GenericDifferentialDFixer();
        PrintablePomTaggedExpression newPPTE = fixer.fixDifferentialD(ppte);

        assertEquals(ppte, newPPTE);
        assertEquals("\\int_{-1}^1 e^{izs}(1 - s^2)^{\\nu-\\frac{1}{2}} \\diff{s}", newPPTE.getTexString());
    }

    @Test
    void compellIntTest() throws ParseException {
        PrintablePomTaggedExpression ppte = mlp.parse("E(e) \\,=\\, \\int_0^{\\pi/2}\\sqrt {1 - e^2 \\sin^2\\theta}\\ d\\theta");
        GenericDifferentialDFixer fixer = new GenericDifferentialDFixer();
        PrintablePomTaggedExpression newPPTE = fixer.fixDifferentialD(ppte);

        assertEquals(ppte, newPPTE);
        assertEquals("E(e) = \\int_0^{\\pi/2} \\sqrt {1 - e^2 \\sin^2\\theta} \\diff{\\theta}", newPPTE.getTexString());
    }

    @Test
    void mathrmDTest() throws ParseException {
        PrintablePomTaggedExpression ppte = mlp.parse("\\int_0^\\varphi \\frac {\\mathrm{d}\\theta}{\\sqrt{1 - k^2 \\sin^2 \\theta}}");
        GenericDifferentialDFixer fixer = new GenericDifferentialDFixer();
        PrintablePomTaggedExpression newPPTE = fixer.fixDifferentialD(ppte);

        assertEquals(ppte, newPPTE);
        assertEquals("\\int_0^\\varphi \\frac{\\diff{\\theta}}{\\sqrt{1 - k^2 \\sin^2 \\theta}}", newPPTE.getTexString());
    }
}
