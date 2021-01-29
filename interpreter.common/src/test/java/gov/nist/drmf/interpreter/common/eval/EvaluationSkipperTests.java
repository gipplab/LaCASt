package gov.nist.drmf.interpreter.common.eval;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Andre Greiner-Petter
 */
public class EvaluationSkipperTests {

    @Test
    void noEquationSkipTest() {
        String in = "x + y";
        assertFalse( EvaluationSkipper.shouldBeEvaluated(in) );
        assertTrue( EvaluationSkipper.shouldNotBeEvaluated(in) );
    }

    @Test
    void validEquationTest() {
        String in = "x = y";
        assertTrue( EvaluationSkipper.shouldBeEvaluated(in) );
        assertFalse( EvaluationSkipper.shouldNotBeEvaluated(in) );
    }

    @Test
    void validEquationButUnderscoreTest() {
        String in = "x_2 = y";
        assertFalse( EvaluationSkipper.shouldBeEvaluated(in) );
        assertTrue( EvaluationSkipper.shouldNotBeEvaluated(in) );
    }

    @Test
    void sumIsAllowedTest() {
        String in = "\\sum_2^x = y";
        assertTrue( EvaluationSkipper.shouldBeEvaluated(in) );
        assertFalse( EvaluationSkipper.shouldNotBeEvaluated(in) );
    }

    @Test
    void sumIsAllowedSecondTest() {
        String in = "\\sum_{x=1}^n = y";
        assertTrue( EvaluationSkipper.shouldBeEvaluated(in) );
        assertFalse( EvaluationSkipper.shouldNotBeEvaluated(in) );
    }

    @Test
    void invalidOnlySumTest() {
        String in = "\\sum_{x=1}^n n^2";
        assertFalse( EvaluationSkipper.shouldBeEvaluated(in) );
        assertTrue( EvaluationSkipper.shouldNotBeEvaluated(in) );
    }

    @Test
    void sumIsAllowedOrderChangeTest() {
        String in = "\\sum^n_{x=1} = y";
        assertTrue( EvaluationSkipper.shouldBeEvaluated(in) );
        assertFalse( EvaluationSkipper.shouldNotBeEvaluated(in) );
    }

    @Test
    void jacobiPolyDefTest() {
        String in = "\\JacobipolyP{\\alpha}{\\beta}{n}@{z} = \\frac{\\Pochhammersym{\\alpha + 1}{n}}{n!} \\genhyperF{2}{1}@{- n , 1 + \\alpha + \\beta + n}{\\alpha + 1}{\\tfrac{1}{2}(1 - z)}";
        assertTrue( EvaluationSkipper.shouldBeEvaluated(in) );
        assertFalse( EvaluationSkipper.shouldNotBeEvaluated(in) );
    }

    @Test
    void jacobiPolyRelTest() {
        String in = "\\JacobipolyP{\\alpha}{\\beta}{n}@{z} < \\frac{\\Pochhammersym{\\alpha + 1}{n}}{n!} \\genhyperF{2}{1}@{- n , 1 + \\alpha + \\beta + n}{\\alpha + 1}{\\tfrac{1}{2}(1 - z)}";
        assertTrue( EvaluationSkipper.shouldBeEvaluated(in) );
        assertFalse( EvaluationSkipper.shouldNotBeEvaluated(in) );
    }

    @Test
    void jacobiPolyUnequalTest() {
        String in = "\\JacobipolyP{\\alpha}{\\beta}{n}@{z} \\neq \\frac{\\Pochhammersym{\\alpha + 1}{n}}{n!} \\genhyperF{2}{1}@{- n , 1 + \\alpha + \\beta + n}{\\alpha + 1}{\\tfrac{1}{2}(1 - z)}";
        assertTrue( EvaluationSkipper.shouldBeEvaluated(in) );
        assertFalse( EvaluationSkipper.shouldNotBeEvaluated(in) );
    }

    @Test
    void jacobiPolyNoRelationNegTest() {
        String in = "\\JacobipolyP{\\alpha}{\\beta}{n}@{z} - \\frac{\\Pochhammersym{\\alpha + 1}{n}}{n!} \\genhyperF{2}{1}@{- n , 1 + \\alpha + \\beta + n}{\\alpha + 1}{\\tfrac{1}{2}(1 - z)}";
        assertFalse( EvaluationSkipper.shouldBeEvaluated(in) );
        assertTrue( EvaluationSkipper.shouldNotBeEvaluated(in) );
    }

    @Test
    void jacobiPolyGammaTest() {
        String in = "\\JacobipolyP{\\alpha}{\\beta}{n}@{z} = \\frac{\\EulerGamma@{\\alpha + n + 1}}{n! \\EulerGamma@{\\alpha + \\beta + n + 1}} \\sum_{m=0}^n{n\\choose m} \\frac{\\EulerGamma@{\\alpha + \\beta + n + m + 1}}{\\EulerGamma@{\\alpha + m + 1}}(\\frac{z-1}{2})^m";
        assertTrue( EvaluationSkipper.shouldBeEvaluated(in) );
        assertFalse( EvaluationSkipper.shouldNotBeEvaluated(in) );
    }

    @Test
    void jacobiPolyGammaNegTest() {
        String in = "\\frac{\\EulerGamma@{\\alpha + n + 1}}{n! \\EulerGamma@{\\alpha + \\beta + n + 1}} \\sum_{m=0}^n{n\\choose m} \\frac{\\EulerGamma@{\\alpha + \\beta + n + m + 1}}{\\EulerGamma@{\\alpha + m + 1}}(\\frac{z-1}{2})^m";
        assertFalse( EvaluationSkipper.shouldBeEvaluated(in) );
        assertTrue( EvaluationSkipper.shouldNotBeEvaluated(in) );
    }

    @Test
    void limIntTest() {
        String in = "\\logint@{x} = \\lim_{\\varepsilon \\to 0+}(\\int_0^{1-\\varepsilon} \\frac{\\diff{t}}{\\ln t} + \\int_{1+\\varepsilon}^x \\frac{\\diff{t}}{\\ln t})";
        assertTrue( EvaluationSkipper.shouldBeEvaluated(in) );
        assertFalse( EvaluationSkipper.shouldNotBeEvaluated(in) );
    }
}
