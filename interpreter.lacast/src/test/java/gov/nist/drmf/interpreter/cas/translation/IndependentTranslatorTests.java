package gov.nist.drmf.interpreter.cas.translation;

import gov.nist.drmf.interpreter.common.constants.Keys;
import gov.nist.drmf.interpreter.common.exceptions.InitTranslatorException;
import gov.nist.drmf.interpreter.pom.SemanticMLPWrapper;
import gov.nist.drmf.interpreter.pom.common.meta.AssumeMLPAvailability;
import gov.nist.drmf.interpreter.pom.extensions.PrintablePomTaggedExpression;
import mlp.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Andre Greiner-Petter
 */
@AssumeMLPAvailability
public class IndependentTranslatorTests {
    private static final Logger LOG = LogManager.getLogger(IndependentTranslatorTests.class.getName());

    private static SemanticMLPWrapper mlp;
    private static SemanticLatexTranslator sltMaple;
    private static SemanticLatexTranslator sltMathematica;

    @BeforeAll
    public static void setup() throws InitTranslatorException {
        mlp = SemanticMLPWrapper.getStandardInstance();
        sltMaple = new SemanticLatexTranslator(Keys.KEY_MAPLE);
        sltMathematica = new SemanticLatexTranslator(Keys.KEY_MATHEMATICA);
    }

    @Test
    public void simpleTestOfBothTranslatorsTest() {
        String in = "\\cpi^k";
        String expOutMaple = "(Pi)^(k)";
        String expOutMath = "(Pi)^(k)";

        String outMaple = sltMaple.translate(in);
        String outMath = sltMathematica.translate(in);

        assertEquals(expOutMaple, outMaple);
        assertEquals(expOutMath, outMath);
    }

    @Test
    public void testParseTreeInstanceTest() throws ParseException {
        String in = "x + \\JacobiP{\\alpha}{\\beta}{n}@{\\cos@{a\\Theta\\sqrt{\\frac{1}{\\iunit}}}}";
        String expOutMaple = "x + JacobiP(n, alpha, beta, cos(a*Theta*sqrt((1)/(I))))";
        PrintablePomTaggedExpression parseTree = mlp.parse(in);

        String outMapleFirst = sltMaple.translate(parseTree).getTranslatedExpression();
        String outMapleSecond = sltMaple.translate(parseTree).getTranslatedExpression();

        assertEquals(expOutMaple, outMapleFirst, "Translation went wrong without even trying multiple calls");
        assertEquals(expOutMaple, outMapleSecond, "The parse tree instance must have been manipulated, " +
                "because the second call on the same tree failed!");
    }

    @Test
    public void testParseTreeInstanceSimultaneouslyTest() throws ParseException {
        String in = "x + \\JacobiP{\\alpha}{\\beta}{n}@{\\cos@{a\\Theta\\sqrt{\\frac{1}{\\iunit}}}}";
        String expOutMaple = "x + JacobiP(n, alpha, beta, cos(a*Theta*sqrt((1)/(I))))";
        String expOutMath = "x + JacobiP[n, \\[Alpha], \\[Beta], Cos[a*\\[CapitalTheta]*Sqrt[Divide[1,I]]]]";

        PrintablePomTaggedExpression parseTree = mlp.parse(in);

        // calling translations on the same instance simultaneously
        ForkJoinPool pool = new ForkJoinPool(4);
        List<String> outMaple = new LinkedList<>();
        List<String> outMath = new LinkedList<>();

        for ( int i = 0; i < 10; i++ ) {
            pool.submit(() -> {
                outMaple.add(sltMaple.translate(parseTree).getTranslatedExpression());
            });
            pool.submit(() -> {
                outMath.add(sltMathematica.translate(parseTree).getTranslatedExpression());
            });
        }

        pool.shutdown();
        try {
            pool.awaitTermination(10, TimeUnit.SECONDS);
            outMaple.forEach( s -> assertEquals(expOutMaple, s, "Parse tree was manipulated during translation process") );
            outMath.forEach( s -> assertEquals(expOutMath, s, "Parse tree was manipulated during translation process") );
        } catch (InterruptedException e) {
            LOG.warn("Parallel execution on a single instance of a parse tree did not fail but took too long.");
        }
    }

    @Test
    public void parallelTranslatorsTest() {
        String in = "\\JacobiP{\\alpha}{\\beta}{n}@{\\cos@{a\\Theta\\sqrt{\\frac{1}{\\iunit}}}}";
        String expOutMaple = "JacobiP(n, alpha, beta, cos(a*Theta*sqrt((1)/(I))))";
        String expOutMath = "JacobiP[n, \\[Alpha], \\[Beta], Cos[a*\\[CapitalTheta]*Sqrt[Divide[1,I]]]]";

        ForkJoinPool pool = new ForkJoinPool(4);
        List<String> outMaple = new LinkedList<>();
        List<String> outMath = new LinkedList<>();

        for ( int i = 0; i < 10; i++ ) {
            pool.submit(() -> {
                outMaple.add(sltMaple.translate(in));
            });
            pool.submit(() -> {
                outMath.add(sltMathematica.translate(in));
            });
        }

        pool.shutdown();
        try {
            pool.awaitTermination(10, TimeUnit.SECONDS);
            outMaple.forEach( s -> assertEquals(expOutMaple, s) );
            outMath.forEach( s -> assertEquals(expOutMath, s) );
        } catch (InterruptedException e) {
            LOG.warn("Parallel execution did not fail but took too long.");
        }
    }
}
