package gov.nist.drmf.interpreter.pom.common.grammar;

import mlp.MathTerm;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Andre Greiner-Petter
 */
public class LimitedExpressionTest {
    @Test
    public void intTest() {
        isIntTest("\\int", 1);
        isIntTest("\\iint", 2);
        isIntTest("\\iiint", 3);
        isIntTest("\\iiiint", 4);
    }

    @Test
    public void invalidIntTest() {
        isInvalidIntTest("\\nt");
        isInvalidIntTest("\\iiiiint"); // max are 4 i
    }

    private void isIntTest( String intExpr, int degree ) {
        MathTerm mathTerm = new MathTerm(intExpr, MathTermTags.operator.tag());
        assertTrue(LimitedExpressions.isLimitedExpression(mathTerm));
        assertTrue(LimitedExpressions.isIntegral(mathTerm));

        assertEquals(LimitedExpressions.INT, LimitedExpressions.getExpression(mathTerm));
        assertEquals(degree, LimitedExpressions.getMultiIntDegree(mathTerm));
    }

    private void isInvalidIntTest( String intExpr ) {
        MathTerm mathTerm = new MathTerm(intExpr, MathTermTags.operator.tag());
        assertFalse(LimitedExpressions.isLimitedExpression(mathTerm));
        assertFalse(LimitedExpressions.isIntegral(mathTerm));

        assertNull(LimitedExpressions.getExpression(mathTerm));
        assertThrows(
                IllegalArgumentException.class,
                () -> LimitedExpressions.getMultiIntDegree(mathTerm));
    }
}
