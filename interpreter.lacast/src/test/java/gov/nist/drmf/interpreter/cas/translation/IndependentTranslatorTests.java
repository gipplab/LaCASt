package gov.nist.drmf.interpreter.cas.translation;

import gov.nist.drmf.interpreter.common.constants.GlobalPaths;
import gov.nist.drmf.interpreter.common.constants.Keys;
import gov.nist.drmf.interpreter.common.meta.AssumeMLPAvailability;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Andre Greiner-Petter
 */
@AssumeMLPAvailability
public class IndependentTranslatorTests {
    private static final Logger LOG = LogManager.getLogger(IndependentTranslatorTests.class.getName());

    private static SemanticLatexTranslator sltMaple;
    private static SemanticLatexTranslator sltMathematica;

    @BeforeAll
    public static void setup() throws IOException {
        sltMaple = new SemanticLatexTranslator(Keys.KEY_MAPLE);
        sltMathematica = new SemanticLatexTranslator(Keys.KEY_MATHEMATICA);
        sltMaple.init(GlobalPaths.PATH_REFERENCE_DATA);
        sltMathematica.init(GlobalPaths.PATH_REFERENCE_DATA);
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
    public void parallelTranslatorsTest() {
        String in = "\\JacobiP{\\alpha}{\\beta}{n}@{\\cos@{a\\Theta\\sqrt{\\frac{1}{\\iunit}}}}";
        String expOutMaple = "JacobiP(n, alpha, beta, cos(a*Theta*sqrt((1)/(I))))";
        String expOutMath = "JacobiP[n, \\[Alpha], \\[Beta], Cos[a*\\[CapitalTheta]*Sqrt[Divide[1,I]]]]";

        ForkJoinPool pool = new ForkJoinPool(2);
        String[] outMaple = new String[]{""};
        String[] outMath = new String[]{""};

        pool.submit(() -> {
            outMaple[0] = sltMaple.translate(in);
        });
        pool.submit(() -> {
            outMath[0] = sltMathematica.translate(in);
        });

        pool.shutdown();
        try {
            pool.awaitTermination(10, TimeUnit.SECONDS);
            assertEquals(expOutMaple, outMaple[0]);
            assertEquals(expOutMath, outMath[0]);
        } catch (InterruptedException e) {
            LOG.warn("Parallel execution did not fail but took too long.");
        }
    }
}
