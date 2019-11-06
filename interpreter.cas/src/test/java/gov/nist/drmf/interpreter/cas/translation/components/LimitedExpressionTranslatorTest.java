package gov.nist.drmf.interpreter.cas.translation.components;

import gov.nist.drmf.interpreter.cas.translation.components.cases.Integrals;
import gov.nist.drmf.interpreter.cas.translation.components.cases.Lims;
import gov.nist.drmf.interpreter.cas.translation.components.cases.Products;
import gov.nist.drmf.interpreter.cas.translation.components.cases.Sums;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Before;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Note that we use TestFactory rather than parameterized tests over enum sources, since
 * the factory solution is way faster (20sec vs 1sec).
 *
 * @author Avi Trost
 * @author Andre Greiner-Petter
 */
public class LimitedExpressionTranslatorTest {
    private static TranslationTester tester;

    @BeforeAll
    public static void mapleSetUp() throws IOException {
        tester = new TranslationTester();
    }

    @TestFactory
    Stream<DynamicTest> sumsMapleTest() {
        return tester.test(Sums.values(), true);
    }

    @TestFactory
    Stream<DynamicTest> sumsMathematicaTest() {
        return tester.test(Sums.values(), false);
    }

    @TestFactory
    Stream<DynamicTest> prodsMapleTest() {
        return tester.test(Products.values(), true);
    }

    @TestFactory
    Stream<DynamicTest> prodsMathematicaTest() {
        return tester.test(Products.values(), false);
    }

    @TestFactory
    Stream<DynamicTest> intsMapleTest() {
        return tester.test(Integrals.values(), true);
    }

    @TestFactory
    Stream<DynamicTest> intsMathematicaTest() {
        return tester.test(Integrals.values(), false);
    }

    @TestFactory
    Stream<DynamicTest> limsMapleTest() {
        return tester.test(Lims.values(), true);
    }

    @TestFactory
    Stream<DynamicTest> limsMathematicaTest() {
        return tester.test(Lims.values(), false);
    }
}
