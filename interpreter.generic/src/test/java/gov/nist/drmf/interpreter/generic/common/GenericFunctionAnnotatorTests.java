package gov.nist.drmf.interpreter.generic.common;

import gov.nist.drmf.interpreter.pom.SemanticMLPWrapper;
import gov.nist.drmf.interpreter.pom.common.MathTermUtility;
import gov.nist.drmf.interpreter.pom.extensions.PrintablePomTaggedExpression;
import mlp.ParseException;
import mlp.PomTaggedExpression;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Andre Greiner-Petter
 */
public class GenericFunctionAnnotatorTests {
    private static SemanticMLPWrapper mlp = SemanticMLPWrapper.getStandardInstance();

    @Test
    void functionTest() throws ParseException {
        PrintablePomTaggedExpression ppte = mlp.parse("\\psi(x)");
        GenericFunctionAnnotator replacementTool = new GenericFunctionAnnotator();
        PomTaggedExpression psi = ppte.getComponents().get(0);
        assertFalse(MathTermUtility.isFunction(psi.getRoot()));
        replacementTool.preProcess(ppte);
        assertTrue(MathTermUtility.isFunction(psi.getRoot()));
    }

    @Test
    void notFunctionRelationTest() throws ParseException {
        PrintablePomTaggedExpression ppte = mlp.parse("n \\leq 0");
        GenericFunctionAnnotator replacementTool = new GenericFunctionAnnotator();
        PomTaggedExpression psi = ppte.getComponents().get(0);
        assertFalse(MathTermUtility.isFunction(psi.getRoot()));
        replacementTool.preProcess(ppte);
        assertFalse(MathTermUtility.isFunction(psi.getRoot()));
    }

    @Test
    void notFunctionTest() throws ParseException {
        PrintablePomTaggedExpression ppte = mlp.parse("\\psi(x+y)");
        GenericFunctionAnnotator replacementTool = new GenericFunctionAnnotator();
        PomTaggedExpression psi = ppte.getComponents().get(0);
        assertFalse(MathTermUtility.isFunction(psi.getRoot()));
        replacementTool.preProcess(ppte);
        assertFalse(MathTermUtility.isFunction(psi.getRoot()));
    }

    @Test
    void consistentFunctionTest() throws ParseException {
        PrintablePomTaggedExpression ppte = mlp.parse("f(x+y) = f(z)");
        GenericFunctionAnnotator replacementTool = new GenericFunctionAnnotator();
        PomTaggedExpression f1 = ppte.getComponents().get(0);
        PomTaggedExpression f2 = ppte.getComponents().get(7);
        assertFalse(MathTermUtility.isFunction(f1.getRoot()));
        assertFalse(MathTermUtility.isFunction(f2.getRoot()));

        replacementTool.preProcess(ppte);
        assertTrue(MathTermUtility.isFunction(f1.getRoot()));
        assertTrue(MathTermUtility.isFunction(f2.getRoot()));
    }
}
