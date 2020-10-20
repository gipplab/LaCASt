package gov.nist.drmf.interpreter.cas.translation;

import gov.nist.drmf.interpreter.common.constants.GlobalPaths;
import gov.nist.drmf.interpreter.common.constants.Keys;
import gov.nist.drmf.interpreter.common.exceptions.InitTranslatorException;
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
    static void setup() throws InitTranslatorException {
        slt = new SemanticLatexTranslator(Keys.KEY_MATHEMATICA);
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

    @Test
    void paraVAlternativeTest() {
        String in = "\\paraV@{a}{x}";
        String expect = "Divide[GAMMA[1/2 + a], Pi]*(Sin[Pi*(a)] * ParabolicCylinderD[-(a) - 1/2, x] + ParabolicCylinderD[-(a) - 1/2, -(x)])";
        String out = slt.translate(in);
        assertEquals(expect, out);
    }

    @Test
    void jacobiThetaQTest() {
        String in = "\\frac{\\Jacobithetaq{3}@{0}{q}}{\\Jacobithetaq{2}@{0}{q}}\\frac{\\Jacobithetaq{1}@{\\zeta}{q}}{\\Jacobithetaq{4}@{\\zeta}{q}}=\\frac{1}{\\Jacobiellnsk@{z}{k}}";
        String expect = "Divide[EllipticTheta[3, 0, q],EllipticTheta[2, 0, q]]*Divide[EllipticTheta[1, \\[Zeta], q],EllipticTheta[4, \\[Zeta], q]] == Divide[1,JacobiNS[z, (k)^2]]";
        String out = slt.translate(in, "22.2.6");
        assertEquals(expect, out);
    }
}
