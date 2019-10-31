package gov.nist.drmf.interpreter.cas.translation.components;

import gov.nist.drmf.interpreter.cas.translation.components.cases.ExceptionalTestCase;
import gov.nist.drmf.interpreter.cas.translation.components.cases.InvalidTests;
import gov.nist.drmf.interpreter.common.exceptions.TranslationException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.function.Executable;

import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Andre Greiner-Petter
 */
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
                                exp.getTitle() + ": " + exp.getTex(),
                                () -> {
                                    String in = exp.getTex();
                                    assertThrows(
                                            TranslationException.class,
                                            () -> tester.getMapleTranslator().translate(in)
                                    );
                                }
                        )
                );
    }
}
