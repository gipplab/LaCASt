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
public class GenericReplacementToolTests {
    private static SemanticMLPWrapper mlp = SemanticMLPWrapper.getStandardInstance();

    @Test
    void simpleDiffTest() throws ParseException {
        PrintablePomTaggedExpression ppte = mlp.parse("\\int_0^1 x dx");
        GenericReplacementTool replacementTool = new GenericReplacementTool(ppte);
        ppte = replacementTool.getSemanticallyEnhancedExpression();
        assertEquals("\\int_0^1 x \\diff{x}", ppte.getTexString());
    }

}
