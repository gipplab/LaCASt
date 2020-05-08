package gov.nist.drmf.interpreter.mathematica;

import com.wolfram.jlink.MathLinkException;
import gov.nist.drmf.interpreter.mathematica.common.AssumeMathematicaAvailability;
import gov.nist.drmf.interpreter.mathematica.extension.MathematicaInterface;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Andre Greiner-Petter
 */
@AssumeMathematicaAvailability
public class MathematicaNumericalMethodsTests {

    private static MathematicaInterface mi;

    @BeforeAll
    public static void setup() throws MathLinkException {
        mi = MathematicaInterface.getInstance();
    }

    @Test
    void extractVarsTest() throws MathLinkException {
        String varName = "myVars";
        String exp = "Sum[i^2+Sin[u], {i, 0, N}]";
        mi.extractAndStoreVariables(varName, exp);
        String res = mi.evaluate(varName);
        assertEquals("{N, u}", res);
    }

}
