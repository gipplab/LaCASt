package gov.nist.drmf.interpreter.evaluation.core;

import com.maplesoft.externalcall.MapleException;
import gov.nist.drmf.interpreter.common.exceptions.ComputerAlgebraSystemEngineException;
import gov.nist.drmf.interpreter.common.meta.AssumeMLPAvailability;
import gov.nist.drmf.interpreter.evaluation.core.translation.MapleTranslator;
import gov.nist.drmf.interpreter.maple.setup.AssumeMapleAvailability;
import gov.nist.drmf.interpreter.evaluation.core.cases.FunctionsTests;
import gov.nist.drmf.interpreter.evaluation.core.cases.LaTeXRoundTripIdentityTests;
import gov.nist.drmf.interpreter.evaluation.core.cases.PolynomialTests;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 *
 * Created by AndreG-P on 01.03.2017.
 */
@AssumeMapleAvailability
@AssumeMLPAvailability
public class RoundTripTest {
    private static MapleTranslator global_translator = new MapleTranslator();

    @BeforeAll
    public static void setup(){
        try {
            global_translator.init();
        } catch ( Exception e ){
            e.printStackTrace();
        }
    }

    @Nested
    public class PolynomialInnerTester extends PolynomialTests {
        PolynomialInnerTester() {
            super.translator = global_translator;
        }

        @TestFactory
        @Override
        public Iterable<DynamicTest> polynomialRoundTripTests() {
            return super.polynomialRoundTripTests();
        }

        @Test
        @Override
        public void fixPointTest() {
            super.fixPointTest();
        }
    }

    @Nested
    public class FunctionsInnerTester extends FunctionsTests {
        FunctionsInnerTester() {
            super.translator = global_translator;
        }

        @TestFactory
        @Override
        public Iterable<DynamicTest> functionsRoundTripTests() {
            return super.functionsRoundTripTests();
        }
    }

    @Nested
    public class LaTeXInnerTester extends LaTeXRoundTripIdentityTests {
        LaTeXInnerTester() {
            super.translator = global_translator;
        }

        @Test
        @Override
        public void straight() throws ComputerAlgebraSystemEngineException, MapleException {
            super.straight();
        }
    }

    @Test
    public void multipleTranslationTest() throws Exception {
        String test = "1/((a+2)/(b^(Catalan/I)/(alpha*q^I*x/3)*alpha))";
        String latex_result, back_to_maple = null;
        int threshold = 40;

        for ( int i = 0; i < threshold; i++ ){
            latex_result = global_translator.translateFromMapleToLaTeXClean( test );
            back_to_maple = global_translator.translateFromLaTeXToMapleClean( latex_result, null );
            global_translator.forceGC();
        }

        try {
            boolean b = global_translator.getMapleSimplifier().isEquivalent( test, back_to_maple );
            assertTrue(b,
                    "Not symbolically equivalent! Expected: " + test + System.lineSeparator() +
                            "But get: " + back_to_maple);
        } catch (Exception e){
            e.printStackTrace();
            fail("An exception appeared during multiple TranslationTestCases!");
        }
    }

    @Test void fixPointTest() throws Exception {
        int threshold = 10;
        String[] tests = PolynomialTests.getTestPolynomials();
        String cycles = "";

        String test_case = "", new_trans = "";
        for ( String test : tests ){
            test_case = test;
            for ( int i = 1; i <= threshold; i++ ){
                new_trans = global_translator.oneCycleRoundTripTranslationFromMaple( test_case );
                if ( new_trans.equals( test_case ) ) {
                    cycles += i + ", ";
                    break;
                }
                test_case = new_trans;
                if ( i == threshold )
                    fail( test_case + System.lineSeparator() + new_trans );
            }

            global_translator.forceGC();
        }
        System.out.println( cycles );
    }
}
