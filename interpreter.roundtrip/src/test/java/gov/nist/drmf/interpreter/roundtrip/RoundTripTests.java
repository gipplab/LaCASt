package gov.nist.drmf.interpreter.roundtrip;

import com.maplesoft.externalcall.MapleException;
import gov.nist.drmf.interpreter.Translator;
import gov.nist.drmf.interpreter.common.TranslationException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *
 * Created by AndreG-P on 01.03.2017.
 */
public class RoundTripTests {

    private static Translator translator;

    @BeforeAll
    public static void setup(){
        translator = new Translator();
        try {
            translator.init();
        } catch ( Exception e ){
            e.printStackTrace();
        }
    }

    private void tester( String maple_input ){
        try {
            String latex_result = translator.translateFromMapleToLaTeXClean( maple_input );
            String back_to_maple = translator.translateFromLaTeXToMapleClean( latex_result );
            boolean b = translator.simplificationTester( maple_input, back_to_maple );
            assertTrue(b);
        } catch ( TranslationException | MapleException e ){
            e.printStackTrace();
            fail("Cannot test because: " + e.getMessage());
        }
    }

    @Test
    public void alphaTest() throws Exception {
        String latex = "\\alpha";
        String toMaple = translator.translateFromLaTeXToMapleClean( latex );
        String back = translator.translateFromMapleToLaTeXClean( toMaple ).trim();
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
    public void sineFunctionTest(){
        tester( "sin(alpha+1)" );
    }

    @Test
    public void jacobiUseCaseTest(){
        tester( "JacobiP(n, alpha, beta, cos(a Theta))" );
    }

    @Test
    public void multipleTranslationTest() throws Exception {
        String test = "1/((a+2)/(b^(Catalan/I)/(alpha*q^I*x/3)*alpha))";
        String latex_result, back_to_maple = null;

        for ( int i = 0; i < 5; i++ ){
            latex_result = translator.translateFromMapleToLaTeXClean( test );
            back_to_maple = translator.translateFromLaTeXToMapleClean( latex_result );
        }

        try {
            boolean b = translator.simplificationTester( test, back_to_maple );
            assertTrue(b);
        } catch (Exception e){
            e.printStackTrace();
            fail();
        }
    }
}
