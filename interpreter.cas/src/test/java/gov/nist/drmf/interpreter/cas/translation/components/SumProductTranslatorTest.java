package gov.nist.drmf.interpreter.cas.translation.components;

import gov.nist.drmf.interpreter.cas.translation.SemanticLatexTranslator;
import gov.nist.drmf.interpreter.common.GlobalConstants;
import gov.nist.drmf.interpreter.common.GlobalPaths;
import gov.nist.drmf.interpreter.common.Keys;
import gov.nist.drmf.interpreter.common.TeXPreProcessor;
import gov.nist.drmf.interpreter.mlp.extensions.MacrosLexicon;
import mlp.PomParser;
import mlp.PomTaggedExpression;

import org.junit.jupiter.api.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SumProductTranslatorTest {

    private static final String stuffBeforeMathematica = "\n" +
            "This is a program that translated given LaTeX\n" +
            "code into a specified computer algebra system\n" +
            "representation.\n" +
            "\n" +
            "You set the following CAS: Mathematica\n" +
            "\n" +
            "You want to translate the following expression: " + "\n" +
            "\n" +
            "Set global variable to given CAS.\n" +
            "Set up translation...\n" +
            "Initialize translation...\n" +
            "Start translation...\n" +
            "\n" +
            "Finished conversion to Mathematica:\n";

    private static final String stuffBeforeMaple = "\n" +
            "This is a program that translated given LaTeX\n" +
            "code into a specified computer algebra system\n" +
            "representation.\n" +
            "\n" +
            "You set the following CAS: Maple\n" +
            "\n" +
            "You want to translate the following expression: " + "\n" +
            "\n" +
            "Set global variable to given CAS.\n" +
            "Set up translation...\n" +
            "Initialize translation...\n" +
            "Start translation...\n" +
            "\n" +
            "Finished conversion to Maple:\n";

    private static final String[] sums= {
            "\\sum_{x}^{y}{z}", //maple does not handle sums with only variable as lower limit {x} instead of something like {x=0}
            "\\sum^{t}_{y}{z}",
            "\\sum_{t}{y}",
            "\\sum_{t=0}^{\\infty}{t^2}",
            "\\sum_{t=3}{\\tan{t}}",
            "\\sum^{100}_{t=0}{12}",
            "\\sum_{x=-\\infty}^{\\infty}x^2(x+2)(y^3-3)-2x+y-2",
            "\\sum_{n=1}^{\\infty}\\frac{(-1)^{n}2^{2n-1}B_{2n}}{n(2n)!}z^{2n}",
            "\\sum^{50}_{r=0}r\\cos{\\Theta}r(3r^2-3)/23x+3q",
            "\\sum_{x=0}^{\\infty}x^3(3x+2y)^{25x^2}(x+2)x^2(x+3)+2x(x+2)^2",
            //6.6.5
            "\\sum_{n=0}^\\infty \\frac{\\opminus^n z^{2n+1}}{(2n+1)!(2n+1)}",
            //29.6.36
            "\\sum_{p \\hiderel{=} 0}^{\\infty} (2p+1) B_{2p+1}",
            //22.12.2 part 1
            "\\sum_{n=-\\infty}^{\\infty}\\frac{\\pi}{\\sin@{\\pi (t - (n+\\frac{1}{2}) \\tau)}}",
            //22.12.2 part 2
            "\\sum_{n=-\\infty}^{\\infty} \\left( \\sum_{m=-\\infty}^{\\infty}\\frac{(-1)^m}{t - m - (n+\\frac{1}{2}) \\tau}\\right)",
            //10.23.4 all
            "\\sum_{k \\hiderel{=} 0}^{2n} \\opminus^k \\BesselJ{k}@{z} \\BesselJ{2n-k}@{z} + 2 \\sum_{k \\hiderel{=} 1}^\\infty \\BesselJ{k}@{z} \\BesselJ{2n+k}@{z} = 0",
            //10.23.4 part 1
            "\\sum_{k \\hiderel{=} 0}^{2n} \\opminus^k \\BesselJ{k}@{z} \\BesselJ{2n-k}@{z}",
            //10.23.4 part 2
            "\\sum_{k \\hiderel{=} 1}^\\infty \\BesselJ{k}@{z} \\BesselJ{2n+k}@{z}",
            //10.23.27 part 1
            "\\sum_{k=0}^{n-1} \\frac{(\\tfrac{1}{2} z)^k \\BesselJ{k}@{z}}{k! (n-k)} + \\frac{2}{\\pi} \\left( \\ln@{\\tfrac{1}{2} z} - \\digamma@{n+1} \\right)\\BesselJ{n}@{z}",
            //10.23.27 part 2
            "\\sum_{k=1}^\\infty \\opminus^k \\frac{(n+2k) \\BesselJ{n+2k}@{z}}{k (n+k)}",
            //22.12.5 part 1
            "\\sum_{n=-\\infty}^{\\infty}\\frac{\\pi}{\\sin@{\\pi (t+\\frac{1}{2}-(n+\\frac{1}{2}) \\tau)}}",
            //22.12.5 part 2
            "\\sum_{n=-\\infty}^{\\infty} \\left( \\sum_{m=-\\infty}^{\\infty} \\frac{(-1)^m}{t + \\frac{1}{2} - m - (n+\\frac{1}{2}) \\tau}\\right)",
            //35.7.3
            "\\sum_{k=0}^\\infty\\frac{\\Pochhammersym{a}{k} \\Pochhammersym{c-a}{k}\\Pochhammersym{b}{k} \\Pochhammersym{c-b}{k}}{k! \\, \\Pochhammersym{c}{2k} \\Pochhammersym{c-\\tfrac{1}{2}}{k}}(t_1 t_2)^k",
            //25.16.11
            "\\sum_{n=1}^\\infty \\frac{1}{n^s} \\sum_{m=1}^n \\frac{1}{m^z}",
            //18.2.6
            "\\sum_{x \\in X} x \\left( p_n(x) \\right)^2 w_x",
            //16.11.2
            "\\sum_{m=1}^p \\sum_{k=0}^\\infty \\frac{\\opminus^k}{k!} \\EulerGamma@{a_m + k} \\left(\\frac{\\prod_{\\ell=1}^p \\EulerGamma@{a_\\ell - a_m - k}} {\\prod_{\\ell=1}^q \\EulerGamma@{b_\\ell - a_m - k}}\\right) z^{-a_m - k}",




    };

    private static final String[] prods = {
            "\\prod_{x}^{y}{z}",
            "\\prod^{t}_{y}{z}",
            "\\prod_{t}{y}",
            "\\prod_{t=0}^{\\infty}{t^2}",
            "\\prod_{t=3}{\\tan{t}}",
            "\\prod^{100}_{t=0}{12}",
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

    };

    private static final String[] translatedMapleSums = {
            "sum(z, x..y)",
            "sum(z, y..t)",
            "sum(y, t)",
            "sum((t)^(2), t = 0..infinity)",
            "sum(tan(t), t = 3)",
            "sum(12, t = 0..100)",
            "sum(x^(2)*(x + 2)((y)^(3) - 3)-2x, x = - infinity..infinity)",
            "sum(((- 1)^(n)* (2)^(2*n - 1)* B[2*n])/(n*factorial((2*n)))z^(2*n), n = 1..infinity)",
            "sum(rcos(Theta)r(3*(r)^(2) - 3)/23x, r = 0..50)",
            "sum(x^(3)*(3*x + 2*y)^(25*(x)^(2))*(x + 2)x^(2)*(x + 3)+2x(x + 2)^(2), x = 0..infinity)",
            "sum(((-1)^(n)* (z)^(2*n + 1))/(factorial((2*n + 1))*(2*n + 1)), n = 0..infinity)",
            "sum((2*p + 1)B[2*p + 1], p = 0..infinity)",
            "sum((pi)/(sin(pi*(t -(n +(1)/(2))tau))), n = - infinity..infinity)",
            "sum((sum(((- 1)^(m))/(t - m -(n +(1)/(2))*tau), m = - infinity..infinity)), n = - infinity..infinity)",
            "sum((-1)^(k)*BesselJ(k, z)BesselJ(2*n - k, z), k = 0..2*n)",
            "sum((-1)^(k)*BesselJ(k, z)BesselJ(2*n - k, z), k = 0..2*n)",
            "sum(BesselJ(k, z)BesselJ(2*n + k, z), k = 1..infinity)",
            "sum((((1)/(2)*z)^(k)* BesselJ(k, z))/(factorial(k)*(n - k)), k = 0..n - 1)",
            "sum((-1)^(k)*((n + 2*k)*BesselJ(n + 2*k, z))/(k*(n + k)), k = 1..infinity)",
            "sum((pi)/(sin(pi*(t +(1)/(2)-(n +(1)/(2))tau))), n = - infinity..infinity)",
            "sum((sum(((- 1)^(m))/(t +(1)/(2)- m -(n +(1)/(2))*tau), m = - infinity..infinity)), n = - infinity..infinity)",
            "sum((pochhammer(a, k)*pochhammer(c - a, k)*pochhammer(b, k)*pochhammer(c - b, k))/(factorial(k)*pochhammer(c, 2*k)*pochhammer(c -(1)/(2), k))(t[1] t[2])^(k), k = 0..infinity)",
            "sum((1)/((n)^(s))sum((1)/((m)^(z)), m = 1..n), n = 1..infinity)",
            "sum(x(p[n](x))^(2)*w[x], x in X)",
            "sum(sum(((-1)^(k))/(factorial(k))GAMMA(a[m] + k)((product(GAMMA(a[ell] - a[m] - k), ell = 1..p))/(product(GAMMA(b[ell] - a[m] - k), ell = 1..q)))z^(- a[m] - k), k = 0..infinity), m = 1..p)",

    };

    private static final String[] translatedMapleProds = {
            "product(z, x..y)",
            "product(z, y..t)",
            "product(y, t)",
            "product((t)^(2), t = 0..infinity)",
            "product(tan(t), t = 3)",
            "product(12, t = 0..100)",
            "product(xsin((x)^(2))cos(t), x = 0..infinity)",
            "product(product((h + j + t - 1)/(h + j - 1), j = 1..s), h = 1..r)",
            "product((a +(n - k)*c)/(a + b +(2*n - k - 1)*c), k = 1..m)",
            "product((1 - (q)^(2*n))(1 - 2*(q)^(2*n)* cos(2*z) + (q)^(4*n)), n = 1..infinity)",
            "product((1 -(4*(z)^(2))/((2*n - 1)^(2)* (pi)^(2))), n = 1..infinity)",
            "product((1 - (q)^(2*n))(1 + (q)^(2*n))^(2), n = 1..infinity)",
            "product((1 + sum(f((p)^(r)), r = 1..infinity)), p)",
            "product((a +(n - k)c), k = 1..m)",

    };

    private static final String[] translatedMathematicaSums = {
            "Sum[z, {x, x, y}]",
            "Sum[z, {y, y, t}]",
            "Sum[y, t]",
            "Sum[(t)^(2), {t, 0, Infinity}]",
            "Sum[Tan[t], t = 3]",
            "Sum[12, {t, 0, 100}]",
            "Sum[x^(2) (x + 2)((y)^(3) - 3)-2x, {x, -Infinity, Infinity}]",
            "Sum[Divide[(- 1)^(n)  (2)^(2 n - 1)  Subscript[B, 2 n],n (2 n)!]z^(2 n), {n, 1, Infinity}]",
            "Sum[rCos[\\[CapitalTheta]]r(3 (r)^(2) - 3)/23x, {r, 0, 50}]",
            "Sum[x^(3) (3 x + 2 y)^(25 (x)^(2)) (x + 2)x^(2) (x + 3)+2x(x + 2)^(2), {x, 0, Infinity}]",
            "Sum[Divide[(-1)^(n)  (z)^(2 n + 1),(2 n + 1)!(2 n + 1)], {n, 0, Infinity}]",
            "Sum[(2 p + 1)Subscript[B, 2 p + 1], {p, 0, Infinity}]",
            "Sum[Divide[\\[Pi],Sin[\\[Pi] (t -(n +Divide[1,2])\\[Tau])]], {n, -Infinity, Infinity}]",
            "Sum[(Sum[Divide[(- 1)^(m),t - m -(n +Divide[1,2]) \\[Tau]], {m, -Infinity, Infinity}]), {n, -Infinity, Infinity}]",
            "Sum[(-1)^(k) BesselJ[k, z]BesselJ[2 n - k, z], {k, 0, 2 n}]",
            "Sum[(-1)^(k) BesselJ[k, z]BesselJ[2 n - k, z], {k, 0, 2 n}]",
            "Sum[BesselJ[k, z]BesselJ[2 n + k, z], {k, 1, Infinity}]",
            "Sum[Divide[(Divide[1,2] z)^(k)  BesselJ[k, z],k!(n - k)], {k, 0, n - 1}]",
            "Sum[(-1)^(k) Divide[(n + 2 k) BesselJ[n + 2 k, z],k (n + k)], {k, 1, Infinity}]",
            "Sum[Divide[\\[Pi],Sin[\\[Pi] (t +Divide[1,2]-(n +Divide[1,2])\\[Tau])]], {n, -Infinity, Infinity}]",
            "Sum[(Sum[Divide[(- 1)^(m),t +Divide[1,2]- m -(n +Divide[1,2]) \\[Tau]], {m, -Infinity, Infinity}]), {n, -Infinity, Infinity}]",
            "Sum[Divide[Pochhammer[a, k] Pochhammer[c - a, k] Pochhammer[b, k] Pochhammer[c - b, k],k! Pochhammer[c, 2 k] Pochhammer[c -Divide[1,2], k]](Subscript[t, 1] Subscript[t, 2])^(k), {k, 0, Infinity}]",
            "Sum[Divide[1,(n)^(s)]Sum[Divide[1,(m)^(z)], {m, 1, n}], {n, 1, Infinity}]",
            "Sum[x(Subscript[p, n](x))^(2) Subscript[w, x], x \\[Element] X]",
            "Sum[Sum[Divide[(-1)^(k),k!]Gamma[Subscript[a, m] + k](Divide[Product[Gamma[Subscript[a, \\[ScriptL]] - Subscript[a, m] - k], {a, 1, p}],Product[Gamma[Subscript[b, \\[ScriptL]] - Subscript[a, m] - k], {b, 1, q}]])z^(- Subscript[a, m] - k), {k, 0, Infinity}], {m, 1, p}]",


    };

    private static final String[] translatedMathematicaProds = {
            "Product[z, {x, x, y}]",
            "Product[z, {y, y, t}]",
            "Product[y, t]",
            "Product[(t)^(2), {t, 0, Infinity}]",
            "Product[Tan[t], t = 3]",
            "Product[12, {t, 0, 100}]",
            "Product[xSin[(x)^(2)]Cos[t], {x, 0, Infinity}]",
            "Product[Product[Divide[h + j + t - 1,h + j - 1], {j, 1, s}], {h, 1, r}]",
            "Product[Divide[a +(n - k) c,a + b +(2 n - k - 1) c], {k, 1, m}]",
            "Product[(1 - (q)^(2 n))(1 - 2 (q)^(2 n)  Cos[2 z] + (q)^(4 n)), {n, 1, Infinity}]",
            "Product[(1 -Divide[4 (z)^(2),(2 n - 1)^(2)  (\\[Pi])^(2)]), {n, 1, Infinity}]",
            "Product[(1 - (q)^(2 n))(1 + (q)^(2 n))^(2), {n, 1, Infinity}]",
            "Product[(1 + Sum[f((p)^(r)), {r, 1, Infinity}]), p]",
            "Product[(a +(n - k)c), {k, 1, m}]"

    };

    private static SemanticLatexTranslator slt;
    private static PomParser parser;
    private static SumProductTranslator spt;

    @TestFactory
    Stream<DynamicTest>  sumMathematicaTest() {
        List<String> expressions = Arrays.asList(sums);
        List<String> output = Arrays.asList(translatedMathematicaSums);
        return test(expressions, output);
    }

    @TestFactory
    Stream<DynamicTest>  prodMathematicaTest() {
        List<String> expressions = Arrays.asList(prods);
        List<String> output = Arrays.asList(translatedMathematicaProds);
        return test(expressions, output);
    }


    @TestFactory
    Stream<DynamicTest>  sumMapleTest() {
        mapleSetUp();
        List<String> expressions = Arrays.asList(sums);
        List<String> output = Arrays.asList(translatedMapleSums);
        return test(expressions, output);
    }

    @TestFactory
    Stream<DynamicTest>  prodMapleTest() {
        mapleSetUp();
        List<String> expressions = Arrays.asList(prods);
        List<String> output = Arrays.asList(translatedMapleProds);
        return test(expressions, output);
    }

    @BeforeEach
    private void mathematicaSetUp(){
        GlobalConstants.CAS_KEY = Keys.KEY_MATHEMATICA;
        slt = new SemanticLatexTranslator(Keys.KEY_LATEX, Keys.KEY_MATHEMATICA);
        try {
            slt.init(GlobalPaths.PATH_REFERENCE_DATA);
        } catch (IOException e) {
            throw new RuntimeException();
        }
        parser = new PomParser(GlobalPaths.PATH_REFERENCE_DATA);
        parser.addLexicons(MacrosLexicon.getDLMFMacroLexicon());
        spt = new SumProductTranslator();
    }

    private void mapleSetUp(){
        GlobalConstants.CAS_KEY = Keys.KEY_MAPLE;
        slt = new SemanticLatexTranslator(Keys.KEY_LATEX, Keys.KEY_MAPLE);
        try {
            slt.init(GlobalPaths.PATH_REFERENCE_DATA);
        } catch (IOException e) {
            throw new RuntimeException();
        }
    }

    private Stream<DynamicTest> test(List<String> expressions, List<String> output){
        return expressions
                .stream()
                .map(
                        exp -> DynamicTest.dynamicTest("Expression: " + exp, () -> {
                            int index = expressions.indexOf(exp);
                            PomTaggedExpression ex = parser.parse(TeXPreProcessor.preProcessingTeX(expressions.get(index)));

                            List<PomTaggedExpression> components = ex.getComponents();
                            PomTaggedExpression first = components.remove(0);
                            spt.translate(first, components);
                            assertEquals(output.get(index), spt.getTranslation());
                        }));
    }

    @AfterEach
    private void tearDown(){
        slt = null;
        parser = null;
        spt = null;
    }

//    @Test
//    public void mathematicaTest(){
//        String more = "";
//        for(int i = 0; i < expression.length; i++){
//            String[] args = {"-CAS=Mathematica", "-Expression=" + expression[i]};
//            SemanticToCASInterpreter.main(args);
//            more += stuffBeforeMathematica.substring(0, stuffBeforeMathematica.indexOf("n: ") + 3) + expression[i]
//                    + stuffBeforeMathematica.substring(stuffBeforeMathematica.indexOf("n: ") + 3);
//            more += translatedMathematica[i] + "\n\n";
//            assertEquals(more, result.toString());
//        }
//    }
//
//    @Test
//    public void mapleTest(){
//        String more = "";
//        for(int i = 0; i < expression.length; i++){
//            String[] args = {"-CAS=Maple", "-Expression=" + expression[i]};
//            SemanticToCASInterpreter.main(args);
//            more += stuffBeforeMaple.substring(0, stuffBeforeMaple.indexOf("n: ") + 3) + expression[i]
//                    + stuffBeforeMaple.substring(stuffBeforeMaple.indexOf("n: ") + 3);
//            more += translatedMaple[i] + "\n\n";
//            assertEquals(more, result.toString());
//        }
//    }

}
