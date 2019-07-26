package gov.nist.drmf.interpreter.cas.translation.components;

import gov.nist.drmf.interpreter.cas.translation.SemanticLatexTranslator;
import gov.nist.drmf.interpreter.common.GlobalConstants;
import gov.nist.drmf.interpreter.common.GlobalPaths;
import gov.nist.drmf.interpreter.common.Keys;
import gov.nist.drmf.interpreter.common.TeXPreProcessor;
import gov.nist.drmf.interpreter.mlp.extensions.MacrosLexicon;
import mlp.PomParser;
import mlp.PomTaggedExpression;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
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

    private static final String[] expression = {
            "\\sum_{x}^{y}{z}", //maple does not handle sums with only variable as lower limit {x} instead of something like {x=0}
            "\\prod_{x}^{y}{z}", //so the next couple of expressions don't work for maple
            "\\sum^{t}_{y}{z}",
            "\\prod^{t}_{y}{z}",
            "\\sum_{t}{y}",
            "\\prod_{t}{y}",
            "\\sum^{t}{y}", //maple also does not handle sums with only an upper limit
            "\\prod^{t}{y}",
            "\\sum{t}",
            "\\prod{t}",
            "\\sum_{t=0}^{\\infty}{t^2}",
            "\\prod_{t=0}^{\\infty}{t^2}",
            "\\sum_{t=3}{\\tan{t}}",
            "\\prod_{t=3}{\\tan{t}}",
            "\\sum^{100}_{t=0}{12}",
            "\\prod^{100}_{t=0}{12}",
            "\\cos{\\sum_{t=-12}^{24y}{-2\\ln{q}}}",
            "\\prod{A_{2\\sin{u}}}",
            "\\frac{1}{z}+2z\\sum_{n=1}^{\\infty}\\frac{1}{z^{2}-n^{2}\\pi^{2}}",
            "\\sum_{n=1}^{\\infty}\\frac{(-1)^{n}2^{2n-1}B_{2n}}{n(2n)!}z^{2n}",
            "\\frac{2p}{y^2}+\\ln{2(3+p^2)}+\\prod^{\\infty}_{t=0}[t^2\\sin{t}]+2^t\\cos{y}",
            "\\frac{2p}{y^2}+\\ln{2(3+p^2)}+\\prod^{\\infty}_{x=0}(t^2\\sin{x})+2^t\\cos{y}",
            "z_{\\prod_{t=1}^{\\infty}{1/t^3}}",
            "z^{\\sum^{100}{u}{t^2+2_v}+3}+\\frac{2\\tan{\\sum{\\prod_{t=0}^{63}{2+t^3}}+5f}}{37}", //does not work for maple because the first sum only has upper limit
            "\\sum_{t}^{y}{\\sum^{100}{\\prod_{t=3}^{5}{\\sum^{\\infty}_{t=1}{\\prod{3t}}}}}", //this one too
            "q_{j}=\\gamma_{j}\\sum_{k=1}^{n}\\frac{1}{z_{k}-a_{j}}",

            "\\prod_{t=7}^{\\infty}\\sum^{-72.3z}_{t=-2}\\prod^{12y}{\\frac{2}" +               // and this
                    "{3g\\prod_{q=16\\pi}^{\\infty}{q\\sin{q}}}}\\prod_{h=23}^{\\infty}" +
                    "\\sum^{z}_{h=\\ln{p^2\\tan{i}}}" + "\\prod_{o=15}^{\\cos{321o+4}}" +
                    "{\\frac{\\sum{l=2}{y-3}+4}{r\\prod^{\\infty}_{l=y}{l}}} + 3.5",

            "(b^2)\\sin{b^2}+4\\arccos{\\prod_{b=1}{12}{y^p\\log{b}}}+\\frac{b\\sum^{200}{3\\cos{b}}}{1.2n}",            //and this
            "(b^2)\\sin{b^2}+4\\arccos{\\prod_{b=1}{12}{y^p\\log{b}}}+\\frac{b\\sum^{200}_{b=1}{3\\cos{b}}}{1.2n}",
            "\\frac{{\\mathrm{d}}^{2}w}{{\\mathrm{d}z}^{2}}+\\left(\\sum_{j=1}^{N}\\frac{\\gamma_{j}}{z-a_{j}}\\right)\\frac{\\mathrm{d}w}{\\mathrm{d}z}+\\frac{\\Phi(z)}{\\prod_{j=1}^{N}(z-a_{j})}w=0"

//a_b^c gives error        "\\sum_{j=1}^{N}\\frac{\\gamma_{j}}{t_{k}-a_{j}}+\\sum_{j=1}^{n-1}\\frac{1}{t_{k}-z_{j}^{\\prime}}=0",
//subarrays don't work     "\\sum_{j=1}^{N}\\frac{\\gamma_{j}/2}{z_{k}-a_{j}}+\\sum_{\\begin{subarray}{c}j=1\\\\ j\\neq k\\end{subarray}}^{n}\\frac{1}{z_{k}-z_{j}}=0",
    };

    private static final String[] translatedMaple = {
            "sum(z, x..y)",
            "product(z, x..y)",
            "sum(z, y..t)",
            "product(z, y..t)",
            "sum(y, t)",
            "product(y, t)",
            "\"errorsequence\"",
            "\"errorsequence\"",
            "sum(t, i)",
            "product(t, i)",
            "sum((t)^(2), t = 0..infinity)",
            "product((t)^(2), t = 0..infinity)",
            "sum(tan(t), t = 3)",
            "product(tan(t), t = 3)",
            "sum(12, t = 0..100)",
            "product(12, t = 0..100)",
            "cos(sum(- 2*ln(q), t = - 12..24*y))",
            "product(A[2*sin(u)], A)",
            "(1)/(z)+ 2*z*sum((1)/((z)^(2)- (n)^(2)* (pi)^(2)), n = 1..infinity)",
            "sum(((- 1)^(n)* (2)^(2*n - 1)* B[2*n])/(n*factorial((2*n))), n = 1..infinity)*(z)^(2*n)",
            "(2*p)/((y)^(2))+ ln(2*(3 + (p)^(2)))+ product([(t)^(2)* sin(t)], t = 0..infinity)+ (2)^(t)* cos(y)",
            "(2*p)/((y)^(2))+ ln(2*(3 + (p)^(2)))+ product(((t)^(2)* sin(x)), x = 0..infinity)+ (2)^(t)* cos(y)",
            "z[product(1/ (t)^(3), t = 1..infinity)]",
            "^(\"errorsequence\")+(2*tan(sum(product(2 + (t)^(3), t = 0..63), i)+ 5*f))/(37)",
            "sum(\"errorsequence\", t..y)",
            "q[j] = gamma[j] sum((1)/(z[k] - a[j]), k = 1..n)",
            "\"errorsequence\"",
            "((b)^(2))*sin((b)^(2))+ 4*arccos(product(12, b = 1)*(y)^(p)* log(b))+(\"errorsequence\")/(1.2*n)",
            "((b)^(2))*sin((b)^(2))+ 4*arccos(product(12, b = 1)*(y)^(p)* log(b))+(b*sum(3*cos(b), b = 1..200))/(1.2*n)",
            "((d)^(2)* w)/(d*(z)^(2))+(sum((gamma[j])/(z - a[j]), j = 1..N))*(d*w)/(d*z)+(Phi*(z))/(product((z - a[j]), j = 1..N))*w = 0"
    };

    private static final String[] translatedMathematica = {
            "Sum[z, {x, x, y}]",
            "Product[z, {x, x, y}]",
            "Sum[z, {y, y, t}]",
            "Product[z, {y, y, t}]",
            "Sum[y, t]",
            "Product[y, t]",
            "Sum[y, {i, t}]",
            "Product[y, {i, t}]",
            "Sum[t, i]",
            "Product[t, i]",
            "Sum[(t)^(2), {t, t = 0, Infinity}]",
            "Product[(t)^(2), {t, t = 0, Infinity}]",
            "Sum[Tan[t], t = 3]",
            "Product[Tan[t], t = 3]",
            "Sum[12, {t, t = 0, 100}]",
            "Product[12, {t, t = 0, 100}]",
            "Cos[Sum[- 2 Log[q], {t, t = - 12, 24 y}]]",
            "Product[Subscript[A, 2 Sin[u]], A]",
            "Divide[1,z]+ 2 z Sum[Divide[1,(z)^(2)- (n)^(2)  (\\[Pi])^(2)], {n, n = 1, Infinity}]",
            "Sum[Divide[(- 1)^(n)  (2)^(2 n - 1)  Subscript[B, 2 n],n (2 n)!], {n, n = 1, Infinity}] (z)^(2 n)",
            "Divide[2 p,(y)^(2)]+ Log[2 (3 + (p)^(2))] + Product[[(t)^(2)  Sin[t]], {t, t = 0, Infinity}] + (2)^(t)  Cos[y]",
            "Divide[2 p,(y)^(2)]+ Log[2 (3 + (p)^(2))] + Product[((t)^(2)  Sin[x]), {x, x = 0, Infinity}] + (2)^(t)  Cos[y]",
            "Subscript[z, Product[1/ (t)^(3), {t, t = 1, Infinity}]]",
            "(z)^(Sum[u, {i, 100}] (t)^(2)+ Subscript[2, v]+ 3)+Divide[2 Tan[Sum[Product[2 + (t)^(3), {t, t = 0, 63}], i] + 5 f],37]",
            "Sum[Sum[Product[Sum[Product[3 t, t], {t, t = 1, Infinity}], {t, t = 3, 5}], {i, 100}], {t, t, y}]",
            "Subscript[q, j] = Subscript[\\[Gamma], j] Sum[Divide[1,Subscript[z, k] - Subscript[a, j]], {k, k = 1, n}]",
            "Product[Sum[Product[Divide[2,3 g Product[q Sin[q], {q, q = 16 \\[Pi], Infinity}]], {i, 12 y}], {t, t = - 2, - 72.3 z}], {t, t = 7, Infinity}] Product[Sum[Product[Divide[Sum[l = 2, l] y - 3+ 4,r Product[l, {l, l = y, Infinity}]], {o, o = 15, Cos[321 o + 4]}], {h, h = Log[(p)^(2)  Tan[i]], z}], {h, h = 23, Infinity}] + 3.5",
            "((b)^(2)) Sin[(b)^(2)] + 4 arccos(Product[12, b = 1] (y)^(p)  Log[b])+Divide[b Sum[3 Cos[b], {b, 200}],1.2 n]",
            "((b)^(2)) Sin[(b)^(2)] + 4 arccos(Product[12, b = 1] (y)^(p)  Log[b])+Divide[b Sum[3 Cos[b], {b, b = 1, 200}],1.2 n]",
            "Divide[(d)^(2)  w,d (z)^(2)]+(Sum[Divide[Subscript[\\[Gamma], j],z - Subscript[a, j]], {j, j = 1, N}]) Divide[d w,d z]+Divide[\\[CapitalPhi] (z),Product[(z - Subscript[a, j]), {j, j = 1, N}]] w = 0"
    };

    private static ByteArrayOutputStream result;
    private static SemanticLatexTranslator slt;

    @BeforeAll
    public static void setUp() {
        GlobalConstants.CAS_KEY = Keys.KEY_MATHEMATICA;
        slt = new SemanticLatexTranslator(Keys.KEY_LATEX, Keys.KEY_MATHEMATICA);
        try {
            slt.init(GlobalPaths.PATH_REFERENCE_DATA);
        } catch (IOException e) {
            throw new RuntimeException();
        }
        result = new ByteArrayOutputStream();
//        System.setOut(new PrintStream(result));
    }

    @TestFactory
    Stream<DynamicTest> sumMathematicaTest() {
        PomParser parser = new PomParser(GlobalPaths.PATH_REFERENCE_DATA.toString());
        parser.addLexicons( MacrosLexicon.getDLMFMacroLexicon() );

//        SumProductTranslator spt = new SumProductTranslator();

        List<String> expressions = Arrays.asList(expression);
        List<String> output = Arrays.asList(translatedMathematica);

        return expressions
                .stream()
                .map(
                        exp -> DynamicTest.dynamicTest("Expression: " + exp, () -> {
                            int index = expressions.indexOf(exp);
                            PomTaggedExpression ex = parser.parse(TeXPreProcessor.preProcessingTeX(expressions.get(index)));

//                            List<PomTaggedExpression> components = ex.getComponents();
//                            PomTaggedExpression first = components.remove(0);

                            slt.translate(ex);
                            assertEquals(output.get(index), slt.getTranslatedExpression());
                        }));
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
