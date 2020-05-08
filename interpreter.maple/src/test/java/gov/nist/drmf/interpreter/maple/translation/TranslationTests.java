package gov.nist.drmf.interpreter.maple.translation;

import gov.nist.drmf.interpreter.maple.cases.TranslationTestCases;
import gov.nist.drmf.interpreter.maple.setup.AssumeMapleAvailability;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.util.Arrays;
import java.util.stream.Stream;

import static gov.nist.drmf.interpreter.common.tests.IgnoresAllWhitespacesMatcher.ignoresAllWhitespaces;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Andre Greiner-Petter
 */
@AssumeMapleAvailability
public class TranslationTests {
    private static final Logger LOG = LogManager.getLogger(TranslationTests.class.getName());

    private static MapleTranslator mi;

    @BeforeAll
    public static void setup() {
        mi = MapleTranslator.getDefaultInstance();
        if ( mi == null ) fail("Cannot instantiate maple.");
    }

    @TestFactory
    Stream<DynamicTest> translationTest() {
        return Arrays.stream(TranslationTestCases.values())
                .map(exp ->
                        DynamicTest.dynamicTest(
                                exp.getTitle() + ": " + exp.getTeX(),
                                () -> {
                                    String in = exp.getMaple();
                                    String eOut = exp.getTeX();

                                    String result = mi.translate(in);

                                    LOG.debug("Expected: " + eOut);
                                    LOG.debug("Result:   " + result);
                                    LOG.info( "Translated to: " + result);

                                    result = result.replaceAll("\\s+", "");

                                    assertThat(result, ignoresAllWhitespaces(eOut));
                                }
                        )
                );
    }
}
