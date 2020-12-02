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
public class GenericDifferentialDFixerTests {

    private static final SemanticMLPWrapper mlp = SemanticMLPWrapper.getStandardInstance();

    @Test
    void fixGenericDifferentialDTests() throws ParseException {
        PrintablePomTaggedExpression ppte = mlp.parse("\\int x dx");
        GenericDifferentialDFixer fixer = new GenericDifferentialDFixer(ppte);
        PrintablePomTaggedExpression newPPTE = fixer.fixDifferentialD();

        assertEquals(ppte, newPPTE);
        assertEquals("\\int x \\diff{x}", newPPTE.getTexString());
    }

    @Test
    void fixMultiDiffDTests() throws ParseException {
        PrintablePomTaggedExpression ppte = mlp.parse("\\int \\int x y dx dy");
        GenericDifferentialDFixer fixer = new GenericDifferentialDFixer(ppte);
        PrintablePomTaggedExpression newPPTE = fixer.fixDifferentialD();

        assertEquals(ppte, newPPTE);
        assertEquals("\\int \\int x y \\diff{x} \\diff{y}", newPPTE.getTexString());
    }

    @Test
    void fixGenericDifferentialDInFracTests() throws ParseException {
        PrintablePomTaggedExpression ppte = mlp.parse("\\int \\frac{dx}{x}");
        GenericDifferentialDFixer fixer = new GenericDifferentialDFixer(ppte);
        PrintablePomTaggedExpression newPPTE = fixer.fixDifferentialD();

        assertEquals(ppte, newPPTE);
        assertEquals("\\int \\frac{\\diff{x}}{x}", newPPTE.getTexString());
    }

    @Test
    void argumentIntTests() throws ParseException {
        PrintablePomTaggedExpression ppte = mlp.parse("\\int_{-1}^1 (1 - x) dx");
        GenericDifferentialDFixer fixer = new GenericDifferentialDFixer(ppte);
        PrintablePomTaggedExpression newPPTE = fixer.fixDifferentialD();

        assertEquals(ppte, newPPTE);
        assertEquals("\\int_{-1}^1 (1 - x) \\diff{x}", newPPTE.getTexString());
    }
}
