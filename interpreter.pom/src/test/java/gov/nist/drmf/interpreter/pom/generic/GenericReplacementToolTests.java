package gov.nist.drmf.interpreter.pom.generic;

import gov.nist.drmf.interpreter.pom.SemanticMLPWrapper;
import gov.nist.drmf.interpreter.pom.common.meta.AssumeMLPAvailability;
import gov.nist.drmf.interpreter.pom.extensions.PrintablePomTaggedExpression;
import gov.nist.drmf.interpreter.pom.generic.GenericConstantReplacer;
import gov.nist.drmf.interpreter.pom.generic.GenericReplacementTool;
import mlp.ParseException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Andre Greiner-Petter
 */
@AssumeMLPAvailability
public class GenericReplacementToolTests {
    private static SemanticMLPWrapper mlp = SemanticMLPWrapper.getStandardInstance();

    @Test
    void constantReplaceTest() throws ParseException {
        PrintablePomTaggedExpression ppte = mlp.parse("i + e^{\\pi}");
        GenericReplacementTool replacementTool = new GenericReplacementTool(ppte);
        ppte = replacementTool.getSemanticallyEnhancedExpression();
        assertEquals("\\iunit + \\expe^{\\cpi}", ppte.getTexString());
    }

    @Test
    void constantInplaceReplaceTest() throws ParseException {
        PrintablePomTaggedExpression ppte = mlp.parse("ix^n");
        GenericReplacementTool replacementTool = new GenericReplacementTool(ppte);
        ppte = replacementTool.getSemanticallyEnhancedExpression();
        assertEquals("\\iunit x^n", ppte.getTexString());
    }

    @Test
    void iAsIndexTest() throws ParseException {
        PrintablePomTaggedExpression ppte = mlp.parse("x_i");
        GenericReplacementTool replacementTool = new GenericReplacementTool(ppte);
        ppte = replacementTool.getSemanticallyEnhancedExpression();
        assertEquals("x_i", ppte.getTexString());
    }

    @Test
    void imReplacementTest() throws ParseException {
        PrintablePomTaggedExpression ppte = mlp.parse("x + \\operatorname{Im} (z)");
        GenericConstantReplacer replacementTool = new GenericConstantReplacer(ppte);
        ppte = replacementTool.fixConstants();
        assertEquals("x + \\imagpart(z)", ppte.getTexString());
    }

    @Test
    void notEqualTest() throws ParseException {
        PrintablePomTaggedExpression ppte = mlp.parse("s \\not = 1");
        GenericReplacementTool replacementTool = new GenericReplacementTool(ppte);
        ppte = replacementTool.getSemanticallyEnhancedExpression();
        assertEquals("s \\neq 1", ppte.getTexString());
    }

    @Test
    void simpleDiffTest() throws ParseException {
        PrintablePomTaggedExpression ppte = mlp.parse("\\int_0^1 x dx");
        GenericReplacementTool replacementTool = new GenericReplacementTool(ppte);
        ppte = replacementTool.getSemanticallyEnhancedExpression();
        assertEquals("\\int_0^1 x \\diff{x}", ppte.getTexString());
    }

    @Test
    void keepEmptyExpressionsTest() throws ParseException {
        PrintablePomTaggedExpression ppte = mlp.parse("\\pi + {}_1 F_1");
        GenericReplacementTool replacementTool = new GenericReplacementTool(ppte);
        ppte = replacementTool.getSemanticallyEnhancedExpression();
        assertEquals("\\cpi +{}_1 F_1", ppte.getTexString());
    }

    @Test
    void noBracketsTest() throws ParseException {
        PrintablePomTaggedExpression ppte = mlp.parse("\\int_{-\\infty}^\\infty \\frac {\\gamma\\left(\\frac s 2, z^2 \\pi \\right)} {(z^2 \\pi)^\\frac s 2} e^{-2 \\pi i k z} \\mathrm d z = \\frac {\\Gamma\\left(\\frac {1-s} 2, k^2 \\pi \\right)} {(k^2 \\pi)^\\frac {1-s} 2}");
        GenericReplacementTool replacementTool = new GenericReplacementTool(ppte);
        ppte = replacementTool.getSemanticallyEnhancedExpression();
        assertEquals("\\int_{-\\infty}^\\infty \\frac{\\gamma \\left(\\frac s 2 , z^2 \\cpi \\right)}{(z^2 \\cpi)^\\frac s 2} \\expe^{- 2 \\cpi \\iunit k z} \\diff{z} = \\frac{\\Gamma \\left(\\frac {1-s} 2 , k^2 \\cpi \\right)}{(k^2 \\cpi)^\\frac {1-s} 2}", ppte.getTexString());
    }

    @Test
    void complexIntTest() throws ParseException {
        PrintablePomTaggedExpression ppte = mlp.parse("\\int_{-1}^1 (1 - x)^{\\alpha} (1 + x)^{\\beta} \\JacobipolyP{\\alpha}{\\beta}{m}@{x} \\JacobipolyP{\\alpha}{\\beta}{n}@{x} dx");
        GenericReplacementTool replacementTool = new GenericReplacementTool(ppte);
        ppte = replacementTool.getSemanticallyEnhancedExpression();
        assertEquals("\\int_{-1}^1(1 - x)^{\\alpha}(1 + x)^{\\beta} \\JacobipolyP{\\alpha}{\\beta}{m}@{x} \\JacobipolyP{\\alpha}{\\beta}{n}@{x} \\diff{x}", ppte.getTexString());
    }

    @Test
    void alignMatrixTest() throws ParseException {
        PrintablePomTaggedExpression ppte = mlp.parse("\\begin{align}\\int x^m \\exp(ix^n)\\,dx & =\\frac{x^{m+1}}{m+1}\\,_1F_1\\left(\\begin{array}{c} \\frac{m+1}{n}\\\\1+\\frac{m+1}{n}\\end{array}\\mid ix^n\\right) \\\\& =\\frac{1}{n} i^{(m+1)/n}\\gamma\\left(\\frac{m+1}{n},-ix^n\\right),\\end{align}");
        GenericReplacementTool replacementTool = new GenericReplacementTool(ppte);
        ppte = replacementTool.getSemanticallyEnhancedExpression();
        assertEquals("\\begin{align}\\int x^m \\exp(\\iunit x^n) \\diff{x} &= \\frac{x^{m+1}}{m+1}_1 F_1 \\left(\\begin{array}{c} \\frac{m+1}{n}\\\\1+\\frac{m+1}{n}\\end{array} \\mid \\iunit x^n \\right) \\\\ &= \\frac{1}{n} \\iunit^{(m+1)/n} \\gamma \\left(\\frac{m+1}{n} , - \\iunit x^n \\right) ,\\end{align}", ppte.getTexString());
    }

    @Test
    void derivTest() throws ParseException {
        PrintablePomTaggedExpression ppte = mlp.parse("P_{n}(z) = \\frac{1 }{2^n  n! } \\frac{d^n }{ d z^n }  ( z^2 - 1 )^n");
        GenericReplacementTool replacementTool = new GenericReplacementTool(ppte);
        ppte = replacementTool.getSemanticallyEnhancedExpression();
        assertEquals("P_{n}(z) = \\frac{1 }{2^n  n! } \\deriv [n]{ }{z}(z^2 - 1)^n", ppte.getTexString());
    }
}
