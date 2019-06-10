package gov.nist.drmf.interpreter.mathematica;

import com.wolfram.jlink.*;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * This requires JLink from Mathematica.
 * When downloading the open engine from: https://www.wolfram.com/engine/
 * you are able to find JLink.jar in '<path to open engine>/SystemFiles/Links/JLink/JLink.jar'
 *
 * Update the path in pom.xml from this module for the JLink.jar.
 *
 * Furthermore, update the {@link #LINK_TO_MATH}
 *
 * @author Andre Greiner-Petter
 */
public class MathematicaEngineCallTest {

    private static final String JACOBIP = "JacobiP[n,\\[Alpha],\\[Beta],Cos[a \\[CapitalTheta]]]";

    private static final String LINK_TO_MATH = "/opt/Wolfram/Executables/math";

    private static KernelLink math;

    @BeforeAll
    public static void setup() throws MathLinkException {
        math = MathLinkFactory.createKernelLink(new String[]{
                "-linkmode", "launch",
                "-linkname", LINK_TO_MATH, "-mathlink"
        });
        math.discardAnswer();
    }

    @Test
    public void simpleEvaluationTest() {
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

    @Test
    public void getParseTreeTest() throws MathLinkException, ExprFormatException {
        math.evaluate(JACOBIP);
        math.waitForAnswer();

        Expr expr = math.getExpr();
        assertEquals("JacobiP", expr.head().asString());
        assertEquals(4, expr.args().length);

        System.out.println(expr.toString());
    }

    @Test
    public void getFullFormTest() {
        String fullForm = math.evaluateToOutputForm("FullForm[" + JACOBIP + "]", 0);
        System.out.println(fullForm);
    }

    @AfterAll
    public static void shutwodn() {
        math.close();
    }
}
