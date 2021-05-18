package gov.nist.drmf.interpreter.pom.generic;

import gov.nist.drmf.interpreter.pom.SemanticMLPWrapper;
import gov.nist.drmf.interpreter.pom.common.MathTermUtility;
import gov.nist.drmf.interpreter.pom.extensions.PrintablePomTaggedExpression;
import gov.nist.drmf.interpreter.pom.generic.GenericFunctionAnnotator;
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
    void functionNegativeTest() throws ParseException {
        PrintablePomTaggedExpression ppte = mlp.parse("M(-x)");
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

    @Test
    void consistentFunctionSecondTest() throws ParseException {
        PrintablePomTaggedExpression ppte = mlp.parse("f(x) = f(z+z)");
        GenericFunctionAnnotator replacementTool = new GenericFunctionAnnotator();
        PomTaggedExpression f1 = ppte.getComponents().get(0);
        PomTaggedExpression f2 = ppte.getComponents().get(5);
        assertFalse(MathTermUtility.isFunction(f1.getRoot()));
        assertFalse(MathTermUtility.isFunction(f2.getRoot()));

        replacementTool.preProcess(ppte);
        assertTrue(MathTermUtility.isFunction(f1.getRoot()));
        assertTrue(MathTermUtility.isFunction(f2.getRoot()));
    }

    @Test
    void multiArgumentTest() throws ParseException {
        PrintablePomTaggedExpression ppte = mlp.parse("f(x, y) = x+y");
        GenericFunctionAnnotator replacementTool = new GenericFunctionAnnotator();
        PomTaggedExpression f = ppte.getComponents().get(0);
        assertFalse(MathTermUtility.isFunction(f.getRoot()));

        replacementTool.preProcess(ppte);
        assertTrue(MathTermUtility.isFunction(f.getRoot()));
    }

    @Test
    void multiArgumentArithmeticTest() throws ParseException {
        PrintablePomTaggedExpression ppte = mlp.parse("f(x + y, z) = (x+y)^z");
        GenericFunctionAnnotator replacementTool = new GenericFunctionAnnotator();
        PomTaggedExpression f = ppte.getComponents().get(0);
        assertFalse(MathTermUtility.isFunction(f.getRoot()));

        replacementTool.preProcess(ppte);
        assertTrue(MathTermUtility.isFunction(f.getRoot()));
    }

    @Test
    void noArgumentTest() throws ParseException {
        PrintablePomTaggedExpression ppte = mlp.parse("\\frac{a}{b}(x^2)");
        GenericFunctionAnnotator replacementTool = new GenericFunctionAnnotator();
        PomTaggedExpression b = ppte.getComponents().get(0).getComponents().get(1);
        assertFalse(MathTermUtility.isFunction(b.getRoot()));

        replacementTool.preProcess(ppte);
        assertFalse(MathTermUtility.isFunction(b.getRoot()));
    }

    @Test
    void underscoreTest() throws ParseException {
        PrintablePomTaggedExpression ppte = mlp.parse("\\phi_{1}(x)");
        GenericFunctionAnnotator replacementTool = new GenericFunctionAnnotator();
        PomTaggedExpression phi = ppte.getComponents().get(0);
        assertFalse(MathTermUtility.isFunction(phi.getRoot()));

        replacementTool.preProcess(ppte);
        assertTrue(MathTermUtility.isFunction(phi.getRoot()));
    }

    @Test
    void fontManipulationTest() throws ParseException {
        PrintablePomTaggedExpression ppte = mlp.parse("\\mathbf{F}(x,y) = F_{1}(x,y)\\mathbf{i}+F_{2}(x,y)\\mathbf{j}");
        GenericFunctionAnnotator replacementTool = new GenericFunctionAnnotator();
        PomTaggedExpression F = ppte.getComponents().get(0);
        assertFalse(MathTermUtility.isFunction(F.getRoot()));

        replacementTool.preProcess(ppte);
        assertTrue(MathTermUtility.isFunction(F.getRoot()));
    }

    @Test
    void noVariableDetectionTest() throws ParseException {
        PrintablePomTaggedExpression ppte = mlp.parse("f(x, y) = \\sum_{n > 0}^{m} n(x)");
        GenericFunctionAnnotator replacementTool = new GenericFunctionAnnotator();
        PomTaggedExpression f = ppte.getComponents().get(0);
        PomTaggedExpression n = ppte.getComponents().get(9);
        assertFalse(MathTermUtility.isFunction(f.getRoot()));
        assertFalse(MathTermUtility.isFunction(n.getRoot()));

        replacementTool.preProcess(ppte);
        assertTrue(MathTermUtility.isFunction(f.getRoot()));
        assertFalse(MathTermUtility.isFunction(n.getRoot()));
    }

    @Test
    void noFunctionToDetectTest() throws ParseException {
        PrintablePomTaggedExpression ppte = mlp.parse("\\deriv[2]{f}{x} = \\deriv{}{x}\\left(\\deriv{f}{x}\\right)");
        GenericFunctionAnnotator replacementTool = new GenericFunctionAnnotator();
        replacementTool.preProcess(ppte);

        // looks like a weird test but x is actually followed by (...) and, therefore, may look like a function...
        PomTaggedExpression x = ppte.getComponents().get(5);
        assertFalse(MathTermUtility.isFunction(x.getRoot()));
    }

    @Test
    void localVariableTest() throws ParseException {
        PrintablePomTaggedExpression ppte = mlp.parse("\\sum_{n=1}^{\\infty}\\frac{(-1)^{n}2^{2n-1}\\BernoullinumberB{2n}}{n(2n)!}z^{2n}");
        GenericFunctionAnnotator replacementTool = new GenericFunctionAnnotator();
        PomTaggedExpression n = ppte.getComponents().get(1).getComponents().get(0).getComponents().get(0).getComponents().get(0);
        assertFalse(MathTermUtility.isFunction(n.getRoot()));

        replacementTool.preProcess(ppte);
        assertFalse(MathTermUtility.isFunction(n.getRoot()));
    }

    @Test
    void noFunctionToDetectAbsoluteValueTest() throws ParseException {
        PrintablePomTaggedExpression ppte = mlp.parse("\\ln@{x+\\iunit 0} = \\ln@@{|x|}+ \\iunit \\cpi");
        GenericFunctionAnnotator replacementTool = new GenericFunctionAnnotator();
        replacementTool.preProcess(ppte);

        // looks like a weird test but x is actually followed by (...) and, therefore, may look like a function...
        PomTaggedExpression x = ppte.getComponents().get(2).getComponents().get(0);
        assertFalse(MathTermUtility.isFunction(x.getRoot()));
    }
}
