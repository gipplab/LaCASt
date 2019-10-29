package gov.nist.drmf.interpreter.cas.translation.components;

import gov.nist.drmf.interpreter.cas.translation.SemanticLatexTranslator;
import gov.nist.drmf.interpreter.cas.translation.components.cases.Sums;
import gov.nist.drmf.interpreter.cas.translation.components.cases.TestCase;
import gov.nist.drmf.interpreter.common.constants.GlobalConstants;
import gov.nist.drmf.interpreter.common.constants.GlobalPaths;
import gov.nist.drmf.interpreter.common.constants.Keys;
import gov.nist.drmf.interpreter.mlp.extensions.MacrosLexicon;
import mlp.PomParser;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Stream;

import static gov.nist.drmf.interpreter.cas.translation.components.matcher.IgnoresAllWhitespacesMatcher.ignoresAllWhitespaces;
import static org.hamcrest.MatcherAssert.assertThat;

public class SumProductTranslatorTest {

    private static final String[] prods = {
            "\\prod_{-\\infty}^{\\infty}x^3",
            "\\prod_{n \\leq i \\leq m}\\sin{i} + \\sum_{n \\leq j \\leq m}i^2+j\\prod_{k=0}^{\\infty}k+j+i",
            "\\prod_{i=0}^{\\infty}k^3",
            "\\prod_{x \\in P}x^2+x^3-3",
            "\\prod_{i=0}^{k}i^2+\\prod_{j=0}^{k}i^3-3j+\\prod_{l=0}^{k}j+2+\\sin{l}",
            "\\prod_{i=0}^{10}i^2\\prod_{i=2}^{12}k",
            "\\prod_{i=0}^{10}i^2\\prod_{j=2}^{12}i",
            "\\prod_{i=0}^{10}\\sum_{j=2}^{12}i+\\sin{j^2}",
            "\\prod_{i=0}^{10}\\prod_{j=2}^{12}j^2+i",
            "\\prod_{i=0}^{10}\\prod_{j=2}^{12}\\sum_{k=-\\infty}^\\infty {j+k}^2-3j+2+\\log{i}^2-5",
            "\\prod_{i=0}^{\\infty}\\sin{\\prod_{k=0}^{r}k^3-2k}\\sum_{i=0}^{r}12i^2+k",
            "\\prod_{x=0}^{\\infty}x\\sin{x^2}\\cos{t}+2sin{4}+3",
            //26.12.4
            "\\prod_{h=1}^r \\prod_{j=1}^s \\frac{h+j+t-1}{h+j-1}",
            //5.14.4
            "\\prod_{k=1}^m \\frac{a+(n-k)c}{a+b+(2n-k-1)c} \\prod_{k=1}^n \\frac{\\EulerGamma@{a+(n-k)c} \\EulerGamma@{b+(n-k)c} \\EulerGamma@{1+kc}} {\\EulerGamma@{a+b+(2n-k-1)c}}",
            //20.5.1
            "\\prod_{n=1}^{\\infty} {\\left( 1 - q^{2n} \\right)} {\\left( 1 - 2 q^{2n} \\cos@{2z} + q^{4n} \\right)}",
            //4.22.2
            "\\prod_{n=1}^\\infty \\left( 1 - \\frac{4z^2}{(2n - 1)^2 \\pi^2} \\right)",
            //20.4.3
            "\\prod_{n=1}^{\\infty} \\left( 1 - q^{2n} \\right) \\left( 1 + q^{2n} \\right)^2",
            //27.4.1
            "\\prod_p \\left( 1 + \\sum_{r=1}^\\infty f(p^r) \\right)",
            //5.14.5
            "\\prod_{k=1}^m (a + (n-k)c) \\frac{\\prod_{k=1}^n \\EulerGamma@{a+(n-k)c} \\EulerGamma@{1+kc}} {(\\EulerGamma@{1+c})^n}",
            //17.2.49 part 2
            "\\prod_{n=0}^\\infty \\frac{1}{(1 - q^{5n+1}) (1 - q^{5n+4})}",
            //23.8.7
            "\\prod_{n=1}^\\infty \\frac{\\sin@{\\pi (2n \\omega_3 + z) / (2 \\omega_1)} \\sin@{\\pi (2n \\omega_3 - z) / (2 \\omega_1)}} {\\sin^2@{\\pi n \\omega_3 / \\omega_1}}",
            //3.4.3
            "\\prod_{k = n_0}^{n_1}(t-k)+f^{(n+2)}(\\xi_1)\\prod_{k = n_0}^{n_1}(t-k)",

    };

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
            "limit(sum((-1)^(n)*(pi)/(tan(pi*(t -(n +(1)/(2))tau))), n = - N..N), N = infinity)",
            "limit(sum((-1)^(n)*(limit(sum((1)/(t - m -(n +(1)/(2))*tau), m = - M..M), M = infinity)), n = - N..N), N = infinity)",
            "limit(sum((-1)^(n)*(pi)/(tan(pi*(t - n*tau))), n = - N..N), N = infinity)",
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
            "Limit[Sum[(-1)^(n) Divide[\\[Pi],Tan[\\[Pi] (t -(n +Divide[1,2])\\[Tau])]], {n, -N, N}], N -> Infinity]",
            "Limit[Sum[(-1)^(n) (Limit[Sum[Divide[1,t - m -(n +Divide[1,2]) \\[Tau]], {m, -M, M}], M -> Infinity]), {n, -N, N}], N -> Infinity]",
            "Limit[Sum[(-1)^(n) Divide[\\[Pi],Tan[\\[Pi] (t - n \\[Tau])]], {n, -N, N}], N -> Infinity]",
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

    private static SemanticLatexTranslator slt;
    private static PomParser parser;

    //    @BeforeEach
    private void mathematicaSetUp() throws IOException {
        slt = new SemanticLatexTranslator(Keys.KEY_MATHEMATICA);
        slt.init(GlobalPaths.PATH_REFERENCE_DATA);
        parser = new PomParser(GlobalPaths.PATH_REFERENCE_DATA);
        parser.addLexicons(MacrosLexicon.getDLMFMacroLexicon());
    }

    @BeforeEach
    void mapleSetUp() throws IOException {
        slt = new SemanticLatexTranslator(Keys.KEY_MAPLE);
        slt.init(GlobalPaths.PATH_REFERENCE_DATA);

//        parser = new PomParser(GlobalPaths.PATH_REFERENCE_DATA);
//        parser.addLexicons(MacrosLexicon.getDLMFMacroLexicon());
    }

//    @Test
//    public void singleTest() {
//        String first = "hello";
//        String second = "   Hello   ";
//
//        assertThat(first, equalToIgnoringWhiteSpace(second));
//    }

    @TestFactory
    Stream<DynamicTest> sumTest() {
        return test(Sums.values());
    }

//    @TestFactory
//    Stream<DynamicTest>  prodMathematicaTest() {
//        List<String> expressions = Arrays.asList(prods);
//        List<String> output = Arrays.asList(translatedMathematicaProds);
//        return test(expressions, output);
//    }
/*
messed with onlyLower
    @TestFactory
    Stream<DynamicTest> limMathematicaTest(){
        List<String> expressions = Arrays.asList(lims);
        List<String> output = Arrays.asList(translatedMathematicaLims);
        return test(expressions, output);
    }


 */
//    @TestFactory
//    Stream<DynamicTest>  sumMapleTest() {
//        mapleSetUp();
//        List<String> expressions = Arrays.asList(sums);
//        List<String> output = Arrays.asList(translatedMapleSums);
//        return test(expressions, output);
//    }

//    @TestFactory
//    Stream<DynamicTest>  prodMapleTest() {
//        mapleSetUp();
//        List<String> expressions = Arrays.asList(prods);
//        List<String> output = Arrays.asList(translatedMapleProds);
//        return test(expressions, output);
//    }
/*
    @TestFactory
    Stream<DynamicTest> limMapleTest(){
        mapleSetUp();
        List<String> expressions = Arrays.asList(lims);
        List<String> output = Arrays.asList(translatedMapleLims);
        return test(expressions, output);
    }

 */


    private Stream<DynamicTest> test(TestCase[] cases) {
        return Arrays.stream(cases)
                .map(exp -> DynamicTest.dynamicTest(exp.getTeX(), () -> {
                    String in = exp.getTeX();
                    String expected = exp.getMaple();
                    slt.translate(in);
                    String result = slt.getTranslatedExpression();

                    System.out.println("Result: " + result);

                    result = result.replaceAll("\\s+", "");
                    System.out.println("Expected: "+ expected);

                    assertThat(result, ignoresAllWhitespaces(expected));
                }));
    }

    @AfterEach
    void tearDown() {
        slt = null;
        parser = null;
    }

}
