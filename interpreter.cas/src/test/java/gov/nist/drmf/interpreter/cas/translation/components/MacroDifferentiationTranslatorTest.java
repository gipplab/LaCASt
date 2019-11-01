package gov.nist.drmf.interpreter.cas.translation.components;

import gov.nist.drmf.interpreter.cas.translation.components.cases.SpecialFunctionsAndDerivatives;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.io.IOException;
import java.util.stream.Stream;

/**
 * @author Avi Trost
 * @author Andre Greiner-Petter
 */
class MacroDifferentiationTranslatorTest {
    private static TranslationTester tester;

    @BeforeAll
    static void setUp() throws IOException {
        tester = new TranslationTester();
    }

    @TestFactory
    Stream<DynamicTest> specialFunctionsMapleTest() {
        return tester.test(SpecialFunctionsAndDerivatives.values(), true);
    }

    @TestFactory
    Stream<DynamicTest> specialFunctionsMathematicaTest() {
        return tester.test(SpecialFunctionsAndDerivatives.values(), false);
    }
}