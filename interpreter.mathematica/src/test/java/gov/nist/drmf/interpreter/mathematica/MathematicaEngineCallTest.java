package gov.nist.drmf.interpreter.mathematica;

import com.wolfram.jlink.*;
import gov.nist.drmf.interpreter.common.meta.DLMF;
import gov.nist.drmf.interpreter.mathematica.common.AssumeMathematicaAvailability;
import gov.nist.drmf.interpreter.mathematica.config.MathematicaConfig;
import gov.nist.drmf.interpreter.mathematica.evaluate.SymbolicEquivalenceChecker;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Set;

import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    private static final String TRIG_EQ_LHS = "Sinh[x+y I]";
    private static final String TRIG_EQ_RHS = "(Sinh[x] Cos[y] + I Cosh[x] Sin[y])";

    @DLMF("9.2.3")
    private static final String complTestMathS_LHS = "(AiryAi[0])";
    private static final String complTestMathS_RHS = "Divide[1, Power[3, Divide[2, 3]] * Gamma[Divide[2, 3]]]";

    @DLMF("9.2.4")
    private static final String complTestMath_LHS = "(D[AiryAi[temp], {temp, 1}]/.temp-> 0)";
    private static final String complTestMath_RHS = "- Divide[1, Power[3, Divide[1, 3]] * Gamma[Divide[1, 3]]]";

    private static final int TYPE_FUNCTION = 100;
    private static final int TYPE_IDENTIFIER = 4;

    private static MathematicaInterface mi;

    @BeforeAll
    public static void setup() throws MathLinkException {
        mi = MathematicaInterface.getInstance();
    }

    @Test
    public void initTest() throws MathLinkException {
        String out = mi.evaluate("$CharacterEncoding");
        assertEquals("\"ASCII\"", out);
    }

    @Test
    public void simpleParseTest() throws MathLinkException {
        String result = mi.evaluate("2+2");
        assertEquals("4", result, "We expected to generate 4 when calling '2+2'.");
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
        Expr expr = mi.evaluateToExpression(JACOBIP);
        assertEquals("JacobiP", expr.head().asString());
        assertEquals(4, expr.args().length);

        Expr[] args = expr.args();
        assertEquals("n", args[0].asString());

        System.out.println(expr.toString());
    }

    @Test
    public void getFullFormTest() throws MathLinkException {
        String fullForm = mi.convertToFullForm(JACOBIP);
        assertEquals(JACOBIP_FULL_FORM, fullForm, "Expected a different full form of JacobiP");
        System.out.println(fullForm);
    }

    @Test
    public void complexEvaluationTest() throws MathLinkException {
        SymbolicEquivalenceChecker checker = mi.getEvaluationChecker();
        boolean b = checker.fullSimplifyDifference( complTestMathS_LHS, complTestMathS_RHS );
        assertTrue(b, "The engine should be able to symbolically simplify the expression to 0.");
    }

    @Test
    public void complexPrimeEvaluationTest() throws MathLinkException {
        SymbolicEquivalenceChecker checker = mi.getEvaluationChecker();
        boolean b = checker.fullSimplifyDifference( complTestMath_LHS, complTestMath_RHS );
        assertTrue(b, "The engine should be able to symbolically simplify the expression to 0.");
    }

    @Test
    public void fullsimplifyEquivalenceForms() throws MathLinkException {
        SymbolicEquivalenceChecker checker = mi.getEvaluationChecker();
        boolean b = checker.fullSimplifyDifference( TRIG_EQ_LHS, TRIG_EQ_RHS );
        assertTrue(b, "The engine should be able to symbolically simplify the expression to 0.");
    }

    @Test
    public void setVariablesTest() throws MathLinkException {
        mi.evaluate("x = 1");
        String one = mi.evaluate("x");
        assertEquals("1", one);
    }

    @Test
    @Disabled
    public void extractVariableTest() throws MathLinkException {
        Set<String> vars = mi.getVariables(JACOBIP);
        assertTrue(vars.contains("a"));
        assertTrue(vars.contains("n"));
        assertTrue(vars.contains("\\[Alpha]"));
        assertTrue(vars.contains("\\[Beta]"));
        assertTrue(vars.contains("\\[CapitalTheta]"));
    }

    @Test
    public void errorTest() {
        KernelLink engine = mi.getMathKernel();
        try {
            engine.evaluate("Cos[x]");
            engine.waitForAnswer();
            engine.getBoolean(); // error
            fail("No MathLinkException thrown? Impossible!");
        } catch ( MathLinkException mle ) {
            engine.clearError();
            engine.newPacket();
        }
    }

    @Test
    public void abortTest() {
        KernelLink engine = mi.getMathKernel();
        String test = "Integrate[Divide[1,t], {t, 1, Divide[1,z]}]";
        test = test + " - " + test;

        Thread abortThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println("Finished delay, call abort evaluation.");
                engine.abortEvaluation();
            }
        });

        abortThread.start();
        try {
            engine.evaluate(test);
            engine.waitForAnswer();
            System.out.println(engine.getExpr());
            engine.newPacket();
        } catch (MathLinkException e) {
            e.printStackTrace();
            System.out.println(engine.getLastError());
            engine.clearError();
            engine.newPacket();
        }
    }

    @AfterAll
    public static void shutwodn() {
        mi.shutdown();
    }
}
