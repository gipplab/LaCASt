package gov.nist.drmf.interpreter.mathematica.extension;

import gov.nist.drmf.interpreter.common.exceptions.ComputerAlgebraSystemEngineException;
import gov.nist.drmf.interpreter.common.meta.DLMF;
import gov.nist.drmf.interpreter.mathematica.common.AssumeMathematicaAvailability;
import gov.nist.drmf.interpreter.mathematica.core.MathematicaInterface;
import gov.nist.drmf.interpreter.mathematica.evaluate.SymbolicEquivalenceChecker;
import gov.nist.drmf.interpreter.mathematica.wrapper.jlink.Expr;
import gov.nist.drmf.interpreter.mathematica.wrapper.ExprFormatException;
import gov.nist.drmf.interpreter.mathematica.wrapper.MathLinkException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

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
public class MathematicaInterfaceCallTest {
    private static final String JACOBIP = "JacobiP[n,\\[Alpha],\\[Beta],Cos[a \\[CapitalTheta]]]";

    private static final String TRIG_EQ_LHS = "Sinh[x+y I]";
    private static final String TRIG_EQ_RHS = "(Sinh[x] Cos[y] + I Cosh[x] Sin[y])";

    @DLMF("9.2.3")
    private static final String complTestMathS_LHS = "(AiryAi[0])";
    private static final String complTestMathS_RHS = "Divide[1, Power[3, Divide[2, 3]] * Gamma[Divide[2, 3]]]";

    @DLMF("9.2.4")
    private static final String complTestMath_LHS = "(D[AiryAi[temp], {temp, 1}]/.temp-> 0)";
    private static final String complTestMath_RHS = "- Divide[1, Power[3, Divide[1, 3]] * Gamma[Divide[1, 3]]]";

    private static MathematicaInterface mi;

    @BeforeAll
    public static void setup() {
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
    public void conditionalTest() throws ComputerAlgebraSystemEngineException {
        Expr expr = mi.internalEnterCommand("ConditionalExpression[0, Re[z] > 0]");

        assertEquals("ConditionalExpression", expr.head().toString());

        Expr[] args = expr.args();
        assertEquals("0", args[0].toString());
        assertEquals("Greater[Re[z], 0]", args[1].toString());
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
    void returnValueCheck() throws MathLinkException {
        mi.evaluate("ClearAll[\"Global`*\"]");
        assertEquals("Equal[x, PlusMinus[1]]", mi.evaluate("x == \\[PlusMinus]1"));
        assertEquals("Equal[x, PlusMinus[1]]", mi.evaluate("x == \\[PlusMinus]1"));
        assertEquals("Equal[x, PlusMinus[1]]", mi.evaluate("x == \\[PlusMinus]1"));
        assertEquals("Equal[x, PlusMinus[1]]", mi.evaluate("x == \\[PlusMinus]1"));
    }

    @Test
    public void extractVariableTest() throws MathLinkException {
        Set<String> vars = mi.getVariables(JACOBIP);
        assertTrue(vars.contains("a"));
        assertTrue(vars.contains("n"));
        assertTrue(vars.contains("\\\\[Alpha]"));
        assertTrue(vars.contains("\\\\[Beta]"));
        assertTrue(vars.contains("\\\\[CapitalTheta]"));
    }

    @Test
    void gcTest() {
        mi.forceGC();
    }

    @Test
    void buildList() {
        List<String> list = new LinkedList<>();
        list.add("a");
        list.add("b");
        assertEquals("a, b", mi.buildList(list));
    }

    @AfterAll
    public static void shutdown() {
        mi.shutdown();
    }
}
