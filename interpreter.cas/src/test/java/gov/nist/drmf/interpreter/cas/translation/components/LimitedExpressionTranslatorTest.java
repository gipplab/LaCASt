package gov.nist.drmf.interpreter.cas.translation.components;

import gov.nist.drmf.interpreter.cas.translation.components.cases.Integrals;
import gov.nist.drmf.interpreter.cas.translation.components.cases.Products;
import gov.nist.drmf.interpreter.cas.translation.components.cases.Sums;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Before;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Note that we use TestFactory rather than parameterized tests over enum sources, since
 * the factory solution is way faster (20sec vs 1sec).
 *
 * @author Avi Trost
 * @author Andre Greiner-Petter
 */
public class LimitedExpressionTranslatorTest {
    private static final Logger LOG = LogManager.getLogger(LimitedExpressionTranslatorTest.class.getName());

    private static final String[] lims = {
            //4.31.1
            "\\lim_{z \\to 0} \\frac{\\sinh@@{z}}{z}",
            //4.31.3
            "\\lim_{z \\to 0} \\frac{\\cosh@@{z} - 1}{z^2}",
            //4.4.13
            "\\lim_{x \\to \\infty} x^{-a} \\ln@@{x}",
            //4.4.17
            "\\lim_{n \\to \\infty} \\left( 1 + \\frac{z}{n} \\right)^n",
            //22.12.4 all
            "\\lim_{N \\to \\infty} \\sum_{n=-N}^N \\opminus^n \\frac{\\pi}{\\tan@{\\pi (t - (n+\\frac{1}{2}) \\tau)}} = \\lim_{N \\to \\infty} \\sum_{n=-N}^N \\opminus^n \\left( \\lim_{M \\to \\infty} \\sum_{m=-M}^M \\frac{1}{t - m - (n+\\frac{1}{2}) \\tau} \\right)",
            //22.12.4 part 1
            "\\lim_{N \\to \\infty} \\sum_{n=-N}^N \\opminus^n \\frac{\\pi}{\\tan@{\\pi (t - (n+\\frac{1}{2}) \\tau)}}",
            //22.12.4 part 2
            "\\lim_{N \\to \\infty} \\sum_{n=-N}^N \\opminus^n \\left( \\lim_{M \\to \\infty} \\sum_{m=-M}^M \\frac{1}{t - m - (n+\\frac{1}{2}) \\tau} \\right)",
            //22.12.13 all
            "\\lim_{N \\to \\infty} \\sum_{n=-N}^N \\opminus^n \\frac{\\pi}{\\tan@{\\pi (t-n\\tau)}} = \\lim_{N \\to \\infty} \\sum_{n=-N}^N \\opminus^n \\left( \\lim_{M \\to \\infty} \\sum_{m=-M}^M \\frac{1}{t - m - n \\tau} \\right)",
            //22.12.13 part 1
            "\\lim_{N \\to \\infty} \\sum_{n=-N}^N \\opminus^n \\frac{\\pi}{\\tan@{\\pi (t-n\\tau)}}",
            //22.12.13 part 2
            "\\lim_{N \\to \\infty} \\sum_{n=-N}^N \\opminus^n \\left( \\lim_{M \\to \\infty} \\sum_{m=-M}^M \\frac{1}{t - m - n \\tau} \\right)",
            //20.5.15
            "\\lim_{N \\to \\infty} \\prod_{n=-N}^{N} \\lim_{M \\to \\infty} \\prod_{m=1-M}^{M} \\left( 1 + \\frac{z}{(m - \\tfrac{1}{2} + n \\tau) \\pi} \\right)",
            //20.5.17
            "\\lim_{N \\to \\infty} \\prod_{n=1-N}^{N} \\lim_{M \\to \\infty} \\prod_{m=-M}^{M} \\left( 1 + \\frac{z}{(m + (n-\\tfrac{1}{2}) \\tau) \\pi} \\right)",

    };

    private static final String[] translatedMapleLims = {
            "limit((sinh(z))/(z), z = 0)",
            "limit((cosh(z)- 1)/((z)^(2)), z = 0)",
            "limit((x)^(- a)*ln(x), x = infinity)",
            "limit((1 +(z)/(n))^(n), n = infinity)",
            "limit(sum((-1)^(n)*(pi)/(tan(pi*(t -(n +(1)/(2))tau))), n = - N..N), N = infinity)",
            "limit(sum((-1)^(n)*(limit(sum((1)/(t - m -(n +(1)/(2))*tau), m = - M..M), M = infinity)), n = - N..N), N = infinity)",
            "limit(sum((-1)^(n)*(pi)/(tan(pi*(t - n*tau))), n = - N..N), N = infinity)",
            "limit(sum((-1)^(n)*(limit(sum((1)/(t - m - n*tau), m = - M..M), M = infinity)), n = - N..N), N = infinity)",
            "limit(product(limit(product((1 +(z)/((m -(1)/(2)+ n*tau)*pi)), m = 1 - M..M), M = infinity), n = - N..N), N = infinity)",
            "limit(product(limit(product((1 +(z)/((m +(n -(1)/(2))tau)*pi)), m = - M..M), M = infinity), n = 1 - N..N), N = infinity)",
    };

    private static final String[] translatedMathematicaLims = {
            "Limit[Divide[Sinh[z],z], z -> 0]",
            "Limit[Divide[Cosh[z] - 1,(z)^(2)], z -> 0]",
            "Limit[(x)^(- a) Log[x], x -> Infinity]",
            "Limit[(1 +Divide[z,n])^(n), n -> Infinity]",
            "Limit[Sum[(-1)^(n) Divide[\\[Pi],Tan[\\[Pi] (t -(n +Divide[1,2])\\[Tau])]], {n, -N, N}], N -> Infinity]",
            "Limit[Sum[(-1)^(n) (Limit[Sum[Divide[1,t - m -(n +Divide[1,2]) \\[Tau]], {m, -M, M}], M -> Infinity]), {n, -N, N}], N -> Infinity]",
            "Limit[Sum[(-1)^(n) Divide[\\[Pi],Tan[\\[Pi] (t - n \\[Tau])]], {n, -N, N}], N -> Infinity]",
            "Limit[Sum[(-1)^(n) (Limit[Sum[Divide[1,t - m - n \\[Tau]], {m, -M, M}], M -> Infinity]), {n, -N, N}], N -> Infinity]",
            "Limit[Product[Limit[Product[(1 +Divide[z,(m -Divide[1,2]+ n \\[Tau]) \\[Pi]]), {m, 1-M, M}], M -> Infinity], {n, -N, N}], N -> Infinity]",
            "Limit[Product[Limit[Product[(1 +Divide[z,(m +(n -Divide[1,2])\\[Tau]) \\[Pi]]), {m, -M, M}], M -> Infinity], {n, 1-N, N}], N -> Infinity]",
    };

    private static TranslationTester tester;

    @BeforeAll
    public static void mapleSetUp() throws IOException {
        tester = new TranslationTester();
    }

    @TestFactory
    Stream<DynamicTest> sumsMapleTest() {
        return tester.test(Sums.values(), true);
    }

    @TestFactory
    Stream<DynamicTest> sumsMathematicaTest() {
        return tester.test(Sums.values(), false);
    }

    @TestFactory
    Stream<DynamicTest> prodsMapleTest() {
        return tester.test(Products.values(), true);
    }

    @TestFactory
    Stream<DynamicTest> prodsMathematicaTest() {
        return tester.test(Products.values(), false);
    }

    @TestFactory
    Stream<DynamicTest> intsMapleTest() {
        return tester.test(Integrals.values(), true);
    }

    @TestFactory
    Stream<DynamicTest> intsMathematicaTest() {
        return tester.test(Integrals.values(), false);
    }
}
