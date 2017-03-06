package gov.nist.drmf.interpreter.roundtrip;

import gov.nist.drmf.interpreter.MapleTranslator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *
 * Created by AndreG-P on 01.03.2017.
 */
public class RoundTripTests {

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
    }

    @Nested
    public class FunctionsInnerTester extends FunctionsTests {
        FunctionsInnerTester() {
            super.translator = global_translator;
        }
    }

    @Test
    public void multipleTranslationTest() throws Exception {
        String test = "1/((a+2)/(b^(Catalan/I)/(alpha*q^I*x/3)*alpha))";
        String latex_result, back_to_maple = null;

        for ( int i = 0; i < 5; i++ ){
            latex_result = global_translator.translateFromMapleToLaTeXClean( test );
            back_to_maple = global_translator.translateFromLaTeXToMapleClean( latex_result );
        }

        try {
            boolean b = global_translator.simplificationTester( test, back_to_maple );
            assertTrue(b,
                    "Not symbolically equivalent! Expected: " + test + System.lineSeparator() +
                            "But get: " + back_to_maple);
        } catch (Exception e){
            e.printStackTrace();
            fail("An exception appeared during multiple TranslationTests!");
        }
    }
}
