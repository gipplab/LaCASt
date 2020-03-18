package gov.nist.drmf.interpreter.cas.translation.components;

import gov.nist.drmf.interpreter.cas.translation.components.cases.InvalidTests;
import gov.nist.drmf.interpreter.common.exceptions.TranslationException;
import gov.nist.drmf.interpreter.common.meta.AssumeMLPAvailability;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Andre Greiner-Petter
 */
@AssumeMLPAvailability
public class InvalidTranslationTest {
    private static TranslationTester tester;

    @BeforeAll
    static void setUp() throws IOException {
        tester = new TranslationTester();
    }

    @TestFactory
    Stream<DynamicTest> invalidTranslationTest() {
        return Arrays.stream(InvalidTests.values())
                .map(exp ->
                        DynamicTest.dynamicTest(
                                exp.getTitle() + ": " + exp.getTeX(),
                                () -> {
                                    String in = exp.getTeX();
                                    assertThrows(
                                            TranslationException.class,
                                            () -> tester.getMapleTranslator().translate(in)
                                    );
                                }
                        )
                );
    }
}
