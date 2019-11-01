package gov.nist.drmf.interpreter.cas.translation.components;

import gov.nist.drmf.interpreter.cas.translation.SemanticLatexTranslator;
import gov.nist.drmf.interpreter.cas.translation.components.cases.TestCase;
import gov.nist.drmf.interpreter.common.constants.GlobalPaths;
import gov.nist.drmf.interpreter.common.constants.Keys;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.function.Executable;

import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Stream;

import static gov.nist.drmf.interpreter.cas.translation.components.matcher.IgnoresAllWhitespacesMatcher.ignoresAllWhitespaces;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Andre Greiner-Petter
 */
public class TranslationTester {
    private static final Logger LOG = LogManager.getLogger(TranslationTester.class.getName());

    private SemanticLatexTranslator sltMaple;
    private SemanticLatexTranslator sltMathematica;

    public TranslationTester() throws IOException {
        sltMaple = new SemanticLatexTranslator(Keys.KEY_MAPLE);
        sltMaple.init(GlobalPaths.PATH_REFERENCE_DATA);

        sltMathematica = new SemanticLatexTranslator(Keys.KEY_MATHEMATICA);
        sltMathematica.init(GlobalPaths.PATH_REFERENCE_DATA);
    }

    public SemanticLatexTranslator getMapleTranslator() {
        return sltMaple;
    }

    public SemanticLatexTranslator getMathematicaTranslator() {
        return sltMathematica;
    }

    public Stream<DynamicTest> test(TestCase[] cases, boolean maple) {
        return Arrays.stream(cases)
                .map(exp ->
                        DynamicTest.dynamicTest(
                                exp.getTitle() + ": " + exp.getTeX(),
                                createTest(exp, maple)
                        )
                );
    }

    public Executable createTest(TestCase tc, boolean maple) {
        return () -> {
            LOG.debug("Testing " + tc.getTitle());
            LOG.trace("Input:  " + tc.getTeX());
            String in = tc.getTeX();
            String expected = maple ? tc.getMaple() : tc.getMathematica();

            if ( maple ) {
                sltMaple.translate(in);
            } else {
                sltMathematica.translate(in);
            }

            String result = maple ?
                    sltMaple.getTranslatedExpression() :
                    sltMathematica.getTranslatedExpression();

            LOG.debug("Expected: " + expected);
            LOG.debug("Result:   " + result);

            result = result.replaceAll("\\s+", "");

            assertThat(result, ignoresAllWhitespaces(expected));
        };
    }

}
