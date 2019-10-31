package gov.nist.drmf.interpreter.cas.translation.components;

import gov.nist.drmf.interpreter.cas.translation.SemanticLatexTranslator;
import gov.nist.drmf.interpreter.cas.translation.components.cases.Products;
import gov.nist.drmf.interpreter.cas.translation.components.cases.Sums;
import gov.nist.drmf.interpreter.cas.translation.components.cases.TestCase;
import gov.nist.drmf.interpreter.common.constants.GlobalConstants;
import gov.nist.drmf.interpreter.common.constants.GlobalPaths;
import gov.nist.drmf.interpreter.common.constants.Keys;
import gov.nist.drmf.interpreter.mlp.extensions.MacrosLexicon;
import mlp.PomParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Stream;

import static gov.nist.drmf.interpreter.cas.translation.components.matcher.IgnoresAllWhitespacesMatcher.ignoresAllWhitespaces;
import static org.hamcrest.MatcherAssert.assertThat;

public class SumProductTranslatorTest {
    private static final Logger LOG = LogManager.getLogger(SumProductTranslatorTest.class.getName());

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

    private static final String[] translatedMapleProds = {
            "product((x)^(3), x=- infinity..infinity)",
            "product(sin(i)+sum((i)^(2)+jproduct(k, k = 0..infinity)+j, j=n..m)+i, i=n..m)",
            "product((k)^(3), i = 0..infinity)",
            "product((x)^(2)+(x)^(3), x in P)",
            "product((i)^(2)+product((i)^(3)-3j+product(j+2+sin(l), l = 0..k), j = 0..k), i = 0..k)",
            "product((i)^(2)*, i = 0..10)",
            "product((i)^(2)*product(i, j = 2..12), i = 0..10)",
            "product(sum(i+sin((j)^(2)), j = 2..12), i = 0..10)",
            "product(product((j)^(2), j = 2..12)+i, i = 0..10)",
            "product(product(sum((j + k)^(2), k = - infinity..infinity)-3j, j = 2..12)+2+(log(i))^(2), i = 0..10)",
            "product(sin(product((k)^(3)-2k, k = 0..r)), i = 0..infinity)",
            "product(xsin((x)^(2))cos(t), x = 0..infinity)",
            "product(product((h + j + t - 1)/(h + j - 1), j = 1..s), h = 1..r)",
            "product((a +(n - k)*c)/(a + b +(2*n - k - 1)*c), k = 1..m)",
            "product((1 - (q)^(2*n))(1 - 2*(q)^(2*n)* cos(2*z) + (q)^(4*n)), n = 1..infinity)",
            "product((1 -(4*(z)^(2))/((2*n - 1)^(2)* (pi)^(2))), n = 1..infinity)",
            "product((1 - (q)^(2*n))(1 + (q)^(2*n))^(2), n = 1..infinity)",
            "product((1 + sum(f((p)^(r)), r = 1..infinity)), p)",
            "product((a +(n - k)c), k = 1..m)",
            "product((1)/((1 - (q)^(5*n + 1))*(1 - (q)^(5*n + 4))), n = 0..infinity)",
            "product((sin(pi*(2*n*omega[3] + z)/(2*omega[1]))*sin(pi*(2*n*omega[3] - z)/(2*omega[1])))/((sin(pi*n*omega[3]/ omega[1]))^(2)), n = 1..infinity)",
            "product((t - k), k = n[0]..n[1])",

    };

    private static final String[] translatedMathematicaProds = {
            "Product[(x)^(3), {x, - Infinity, Infinity}]",
            "Product[Sin[i]+Sum[(i)^(2)+jProduct[k, {k, 0, Infinity}]+j, {j, n, m}]+i, {i, n, m}]",
            "Product[(k)^(3), {i, 0, Infinity}]",
            "Product[(x)^(2)+(x)^(3), {x, P}]",
            "Product[(i)^(2)+Product[(i)^(3)-3j+Product[j+2+Sin[l], {l, 0, k}], {j, 0, k}], {i, 0, k}]",
            "Product[(i)^(2) , {i, 0, 10}]",
            "Product[(i)^(2) Product[i, {j, 2, 12}], {i, 0, 10}]",
            "Product[Sum[i+Sin[(j)^(2)], {j, 2, 12}], {i, 0, 10}]",
            "Product[Product[(j)^(2), {j, 2, 12}]+i, {i, 0, 10}]",
            "Product[Product[Sum[(j + k)^(2), {k, -Infinity, Infinity}]-3j, {j, 2, 12}]+2+(Log[i])^(2), {i, 0, 10}]",
            "Product[Sin[Product[(k)^(3)-2k, {k, 0, r}]], {i, 0, Infinity}]",
            "Product[xSin[(x)^(2)]Cos[t], {x, 0, Infinity}]",
            "Product[Product[Divide[h + j + t - 1,h + j - 1], {j, 1, s}], {h, 1, r}]",
            "Product[Divide[a +(n - k) c,a + b +(2 n - k - 1) c], {k, 1, m}]",
            "Product[(1 - (q)^(2 n))(1 - 2 (q)^(2 n)  Cos[2 z] + (q)^(4 n)), {n, 1, Infinity}]",
            "Product[(1 -Divide[4 (z)^(2),(2 n - 1)^(2)  (\\[Pi])^(2)]), {n, 1, Infinity}]",
            "Product[(1 - (q)^(2 n))(1 + (q)^(2 n))^(2), {n, 1, Infinity}]",
            "Product[(1 + Sum[f((p)^(r)), {r, 1, Infinity}]), {p, p}]",
            "Product[(a +(n - k)c), {k, 1, m}]",
            "Product[Divide[1,(1 - (q)^(5 n + 1)) (1 - (q)^(5 n + 4))], {n, 0, Infinity}]",
            "Product[Divide[Sin[\\[Pi] (2 n Subscript[\\[Omega], 3] + z)/(2 Subscript[\\[Omega], 1])] Sin[\\[Pi] (2 n Subscript[\\[Omega], 3] - z)/(2 Subscript[\\[Omega], 1])],(Sin[\\[Pi] n Subscript[\\[Omega], 3]/ Subscript[\\[Omega], 1]])^(2)], {n, 1, Infinity}]",
            "Product[(t - k), {k, Subscript[n, 0], Subscript[n, 1]}]",

    };

    private static SemanticLatexTranslator sltMap, sltMath;

    @BeforeEach
    void mapleSetUp() throws IOException {
        sltMap = new SemanticLatexTranslator(Keys.KEY_MAPLE);
        sltMap.init(GlobalPaths.PATH_REFERENCE_DATA);

        sltMath = new SemanticLatexTranslator(Keys.KEY_MATHEMATICA);
        sltMath.init(GlobalPaths.PATH_REFERENCE_DATA);
    }

    @TestFactory
    Stream<DynamicTest> sumsMapleTest() {
        return test(Sums.values(), true);
    }

    @TestFactory
    Stream<DynamicTest> sumsMathematicaTest() {
        return test(Sums.values(), false);
    }

    @TestFactory
    Stream<DynamicTest> prodsMapleTest() {
        return test(Products.values(), true);
    }

    @TestFactory
    Stream<DynamicTest> prodsMathematicaTest() {
        return test(Products.values(), false);
    }

    private Stream<DynamicTest> test(TestCase[] cases, boolean maple) {
        return Arrays.stream(cases)
                .map(exp -> DynamicTest.dynamicTest(exp.getTitle() + ": " + exp.getTeX(), () -> {
                    LOG.debug("Testing " + exp.getTitle());
                    LOG.trace("Input:  " + exp.getTeX());
                    String in = exp.getTeX();
                    String expected = maple ? exp.getMaple() : exp.getMathematica();

                    if ( maple ) {
                        sltMap.translate(in);
                    } else {
                        sltMath.translate(in);
                    }

                    String result = maple ?
                            sltMap.getTranslatedExpression() :
                            sltMath.getTranslatedExpression();

                    LOG.debug("Expected: " + expected);
                    LOG.debug("Result:   " + result);

                    result = result.replaceAll("\\s+", "");

                    assertThat(result, ignoresAllWhitespaces(expected));
                }));
    }
}
