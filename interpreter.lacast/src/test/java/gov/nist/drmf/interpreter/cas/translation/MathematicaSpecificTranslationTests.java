package gov.nist.drmf.interpreter.cas.translation;

import gov.nist.drmf.interpreter.common.constants.GlobalPaths;
import gov.nist.drmf.interpreter.common.constants.Keys;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Andre Greiner-Petter
 */
public class MathematicaSpecificTranslationTests {

    private static SemanticLatexTranslator slt;

    @BeforeAll
    static void setup() throws IOException {
        slt = new SemanticLatexTranslator(Keys.KEY_MATHEMATICA);
        slt.init(GlobalPaths.PATH_REFERENCE_DATA);
    }

    /**
     * TODO
     * This scenario is not yet supported. However, the technology
     * is implemented. We could in theory support more arguments.
     * The problem is, in Maple we can only support the case of 2 arguments,
     * in Mathematica we can support an unlimited number of arguments.
     * But the code cannot handle this difference between the systems yet.
     * It requires further improvements.
     */
    @Test
    @Disabled
    void longWronskianTest() {
        String in = "\\Wronskian@{z+1, z+2, z+3}";
        String expect = "Wronskian[{z + 1, z + 2, z + 3}, z]";
        String out = slt.translate(in);
        assertEquals(expect, out);
    }

}
