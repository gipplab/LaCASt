package gov.nist.drmf.interpreter.generic.common;

import gov.nist.drmf.interpreter.pom.SemanticMLPWrapper;
import gov.nist.drmf.interpreter.pom.common.meta.AssumeMLPAvailability;
import gov.nist.drmf.interpreter.pom.extensions.PrintablePomTaggedExpression;
import mlp.ParseException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Andre Greiner-Petter
 */
@AssumeMLPAvailability
public class NormalizeOperatorNameCaretTests {

    private static final SemanticMLPWrapper mlp = SemanticMLPWrapper.getStandardInstance();

    @Test
    void normalizeOperatorCaretEndTests() throws ParseException {
        PrintablePomTaggedExpression ppte = mlp.parse("\\operatorname{def}^x a");
        GenericNormalizeOperatorNameCarets fixer = new GenericNormalizeOperatorNameCarets(ppte);
        PrintablePomTaggedExpression newPPTE = fixer.normalize();

        assertEquals(ppte, newPPTE);
        assertEquals("\\operatorname{def} a^x", newPPTE.getTexString());
    }

    @Test
    void normalizeOperatorCaretTests() throws ParseException {
        PrintablePomTaggedExpression ppte = mlp.parse("\\operatorname{def}^x a + 2");
        GenericNormalizeOperatorNameCarets fixer = new GenericNormalizeOperatorNameCarets(ppte);
        PrintablePomTaggedExpression newPPTE = fixer.normalize();

        assertEquals(ppte, newPPTE);
        assertEquals("\\operatorname{def} a^x + 2", newPPTE.getTexString());
    }

    @Test
    void normalizeOperatorCaretBracketTests() throws ParseException {
        PrintablePomTaggedExpression ppte = mlp.parse("\\operatorname{erf}^{(k)}(z) + 2");
        GenericNormalizeOperatorNameCarets fixer = new GenericNormalizeOperatorNameCarets(ppte);
        PrintablePomTaggedExpression newPPTE = fixer.normalize();

        assertEquals(ppte, newPPTE);
        assertEquals("\\operatorname{erf}(z)^{(k)} + 2", newPPTE.getTexString());
    }

}
