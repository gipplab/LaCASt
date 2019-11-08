package gov.nist.drmf.interpreter.mathematica;

import com.wolfram.jlink.*;
import gov.nist.drmf.interpreter.common.meta.DLMF;
import gov.nist.drmf.interpreter.mathematica.common.AssumeMathematicaAvailability;
import gov.nist.drmf.interpreter.mathematica.config.MathematicaConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * This requires JLink from Mathematica.
 * When downloading the open engine from: https://www.wolfram.com/engine/
 * you are able to find JLink.jar in '<path to open engine>/SystemFiles/Links/JLink/JLink.jar'
 *
 * Update the path in pom.xml from this module for the JLink.jar.
 *
 * Furthermore, update the path in libs/mathematica_config.properties
 *
 * @author Andre Greiner-Petter
 */
@AssumeMathematicaAvailability
public class MathematicaEngineCallTest {

    private static final String JACOBIP = "JacobiP[n,\\[Alpha],\\[Beta],Cos[a \\[CapitalTheta]]]";
    private static final String JACOBIP_FULL_FORM = "JacobiP[n, \\[Alpha], \\[Beta], Cos[Times[a, \\[CapitalTheta]]]]";

    private static final String TRIG_EQ = "Sinh[x+y I] - (Sinh[x] Cos[y] + I Cosh[x] Sin[y])";

    @DLMF("9.2.3")
    private static final String complTestMathS = "(AiryAi[0]) - Divide[1, Power[3, Divide[2, 3]] * Gamma[Divide[2, 3]]]";

    @DLMF("9.2.4")
    private static final String complTestMath = "(D[AiryAi[temp], {temp, 1}]/.temp-> 0) - (- Divide[1, Power[3, Divide[1, 3]] * Gamma[Divide[1, 3]]])";

    private static final int TYPE_FUNCTION = 100;
    private static final int TYPE_IDENTIFIER = 4;

    private static KernelLink math;

    @BeforeAll
    public static void setup() throws MathLinkException {
        Path mathPath = MathematicaConfig.loadMathematicaPath();

        math = MathLinkFactory.createKernelLink(new String[]{
                "-linkmode", "launch",
                "-linkname", mathPath.toString(), "-mathlink"
        });

        math.discardAnswer();
    }

    @Test
    public void simpleParseTest() {
        try {
            // request mathematica engine to parse expression
            math.evaluate("2+2");
            // we need to wait for answers after normal evaluations
            math.waitForAnswer();

            // test if it worked
            int result = math.getInteger();
            assertEquals(4, result, "We expected to generate 4 when calling '2+2'.");
        } catch (MathLinkException mle){
            mle.printStackTrace();
            fail();
        }
    }

    /**
     * There is one problem with inner Expression form. Symbols are returned as UTF-8
     * characters and not as Mathematica commands. For example \[Alpha] will only (!)
     * contains the UTF-8 character for an alpha rather than '\[Alpha]
     * @throws MathLinkException
     * @throws ExprFormatException
     */
    @Test
    public void getParseTreeTest() throws MathLinkException, ExprFormatException {
        math.evaluate(JACOBIP);
        math.waitForAnswer();

        Expr expr = math.getExpr();
        assertEquals("JacobiP", expr.head().asString());
        assertEquals(4, expr.args().length);

        Expr[] args = expr.args();
        assertEquals("n", args[0].asString());

        System.out.println(expr.toString());
    }

    @Test
    public void getFullFormTest() {
        String fullForm = math.evaluateToOutputForm("FullForm[" + JACOBIP + "]", 0);
        assertEquals(JACOBIP_FULL_FORM, fullForm, "Expected a different full form of JacobiP");
    }

    @Test
    public void simpleEvaluationTest() throws MathLinkException {
        math.evaluate("FullSimplify["+ TRIG_EQ +"]");
        math.waitForAnswer();

        Expr expr = math.getExpr();
        assertEquals("0", expr.toString(), "The engine should be able to symbolically simplify the expression to 0.");
    }

    @Test
    public void complexEvaluationTest() throws MathLinkException {
        math.evaluate("FullSimplify["+ complTestMathS +"]");
        math.waitForAnswer();

        Expr expr = math.getExpr();
        assertEquals("0", expr.toString(), "The engine should be able to symbolically simplify the expression to 0.");
    }

    @Test
    public void complexPrimeEvaluationTest() throws MathLinkException {
        math.evaluate("FullSimplify["+ complTestMath +"]");
        math.waitForAnswer();

        Expr expr = math.getExpr();
        assertEquals("0", expr.toString(), "The engine should be able to symbolically simplify the expression to 0.");
    }

    @AfterAll
    public static void shutwodn() {
        math.close();
    }
}
