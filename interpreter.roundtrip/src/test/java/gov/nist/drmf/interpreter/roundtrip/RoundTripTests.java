package gov.nist.drmf.interpreter.roundtrip;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Created by AndreG-P on 01.03.2017.
 */
public class RoundTripTests {

    private static RoundTripInterface roundtrip;

    @BeforeAll
    public static void setup(){
        roundtrip = new RoundTripInterface();
        try {
            roundtrip.init();
        } catch ( Exception e ){
            e.printStackTrace();
        }
    }

    private void tester( String maple_input ){
        String latex_result = roundtrip.translateFromMaple( maple_input );
        String back_to_maple = roundtrip.translateFromLaTeX( latex_result );

        try {
            boolean b = roundtrip.equivalenceValidationMaple( maple_input, back_to_maple );
            assertTrue(b);
        } catch (Exception e){
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void alphaTest(){
        String latex = "\\alpha";
        String toMaple = roundtrip.translateFromLaTeX( latex );
        String back = roundtrip.translateFromMaple( toMaple ).trim();
        assertEquals( latex, back, "Expression is not the same!" );
    }

    @Test
    public void symbolicConstantsTest(){
        tester("(infinity+Catalan/2)^gamma");
    }

    @Test
    public void symbolicComplexConstantsTest(){
        tester("gamma+alpha^5-I^(x/5)+Catalan");
    }

    @Test
    public void symbolicPolynomialTest(){
        tester("x + x^2 + ((1-gamma)*x/2)^I");
    }

    @Test
    public void symbolicNestedFractionTest(){
        tester("1/((a+2)/(b^(Catalan/I)/(alpha*q^I*x/3)*alpha))");
    }

    @Test
    public void symbolicNestedPowerTest(){
        tester("((x^a)^b)^c");
    }

    @Test
    public void multipleTranslationTest(){
        String test = "1/((a+2)/(b^(Catalan/I)/(alpha*q^I*x/3)*alpha))";
        String latex_result, back_to_maple = null;

        for ( int i = 0; i < 5; i++ ){
            latex_result = roundtrip.translateFromMaple( test );
            back_to_maple = roundtrip.translateFromLaTeX( latex_result );
        }

        try {
            boolean b = roundtrip.equivalenceValidationMaple( test, back_to_maple );
            assertTrue(b);
        } catch (Exception e){
            e.printStackTrace();
            fail();
        }
    }
}
