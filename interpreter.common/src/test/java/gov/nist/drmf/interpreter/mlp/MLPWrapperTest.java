package gov.nist.drmf.interpreter.mlp;

import gov.nist.drmf.interpreter.common.meta.AssumeMLPAvailability;
import gov.nist.drmf.interpreter.mlp.extensions.PrintablePomTaggedExpression;
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
        mlp = MLPWrapper.getStandardInstance();
    }

    @Test
    public void normalizeAllTest() throws ParseException {
        PrintablePomTaggedExpression ppte = mlp.parse("\\left( x^{1+x}_n \\right)");
        MLPWrapper.normalize(ppte);
        assertEquals("(x_n^{1+x})", ppte.getTexString());
    }
}
