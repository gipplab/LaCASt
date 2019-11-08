package gov.nist.drmf.interpreter.maple.translation;

import com.maplesoft.externalcall.MapleException;
import gov.nist.drmf.interpreter.maple.cases.TranslationTestCases;
import gov.nist.drmf.interpreter.maple.setup.AssumeMapleAvailability;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Stream;

import static gov.nist.drmf.interpreter.common.tests.IgnoresAllWhitespacesMatcher.ignoresAllWhitespaces;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Andre Greiner-Petter
 */
@AssumeMapleAvailability
public class TranslationTests {
    private static final Logger LOG = LogManager.getLogger(TranslationTests.class.getName());

    private static MapleInterface mi;

    @BeforeAll
    public static void setup() throws IOException, MapleException {
        MapleInterface.init();
        mi = MapleInterface.getUniqueMapleInterface();
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
