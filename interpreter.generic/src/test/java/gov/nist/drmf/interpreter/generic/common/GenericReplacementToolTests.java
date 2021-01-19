package gov.nist.drmf.interpreter.generic.common;

import gov.nist.drmf.interpreter.pom.SemanticMLPWrapper;
import gov.nist.drmf.interpreter.pom.common.meta.AssumeMLPAvailability;
import gov.nist.drmf.interpreter.pom.extensions.PrintablePomTaggedExpression;
import mlp.ParseException;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Andre Greiner-Petter
 */
@AssumeMLPAvailability
public class GenericReplacementToolTests {
    private static SemanticMLPWrapper mlp = SemanticMLPWrapper.getStandardInstance();

    @Test
    void simpleDiffTest() throws ParseException {
        PrintablePomTaggedExpression ppte = mlp.parse("\\int_0^1 x dx");
        GenericReplacementTool replacementTool = new GenericReplacementTool(ppte);
        ppte = replacementTool.getSemanticallyEnhancedExpression();
        assertEquals("\\int_0^1 x \\diff{x}", ppte.getTexString());
    }

    @Test
    void complexIntTest() throws ParseException {
        PrintablePomTaggedExpression ppte = mlp.parse("\\int_{-1}^1 (1 - x)^{\\alpha} (1 + x)^{\\beta} \\JacobipolyP{\\alpha}{\\beta}{m}@{x} \\JacobipolyP{\\alpha}{\\beta}{n}@{x} dx");
        GenericReplacementTool replacementTool = new GenericReplacementTool(ppte);
        ppte = replacementTool.getSemanticallyEnhancedExpression();
        assertEquals("\\int_{-1}^1(1 - x)^{\\alpha}(1 + x)^{\\beta} \\JacobipolyP{\\alpha}{\\beta}{m}@{x} \\JacobipolyP{\\alpha}{\\beta}{n}@{x} \\diff{x}", ppte.getTexString());
    }

    @Test
    void derivTest() throws ParseException {
        PrintablePomTaggedExpression ppte = mlp.parse("P_{n}(z) = \\frac{1 }{2^n  n! } \\frac{d^n }{ d z^n }  ( z^2 - 1 )^n");
        GenericReplacementTool replacementTool = new GenericReplacementTool(ppte);
        ppte = replacementTool.getSemanticallyEnhancedExpression();
        assertEquals("P_{n}(z) = \\frac{1 }{2^n  n! } \\deriv [n]{ }{z}(z^2 - 1)^n", ppte.getTexString());
    }
}
