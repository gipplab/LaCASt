package gov.nist.drmf.interpreter.pom;

import gov.nist.drmf.interpreter.pom.common.PomTaggedExpressionNormalizer;
import gov.nist.drmf.interpreter.pom.common.meta.AssumeMLPAvailability;
import gov.nist.drmf.interpreter.pom.extensions.PrintablePomTaggedExpression;
import mlp.ParseException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Andre Greiner-Petter
 */
@AssumeMLPAvailability
public class MLPWrapperTest {
    private static MLPWrapper mlp;

    @BeforeAll
    public static void setup() {
        mlp = SemanticMLPWrapper.getStandardInstance();
    }

    @Test
    public void normalizeAllTest() throws ParseException {
        PrintablePomTaggedExpression ppte = mlp.parse("\\left( x^{1+x}_n \\right)");
        PomTaggedExpressionNormalizer.normalize(ppte);
        assertEquals("(x_n^{1+x})", ppte.getTexString());
    }

    @Test
    public void paraTest() throws ParseException {
        PrintablePomTaggedExpression ppte = mlp.parseRaw("w=\\ifrac{(1-x^{2})}{\\Delta({q,p})}");
        PomTaggedExpressionNormalizer.normalize(ppte);
        assertEquals("w=\\ifrac{(1-x^{2})}{\\Delta({q,p})}", ppte.getTexString());
    }
}
