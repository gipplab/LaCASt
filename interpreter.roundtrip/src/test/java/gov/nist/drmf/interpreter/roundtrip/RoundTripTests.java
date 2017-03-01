package gov.nist.drmf.interpreter.roundtrip;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Created by AndreG-P on 01.03.2017.
 */
public class RoundTripTests {

    private static RoundTripInterface roundtrip;

    @BeforeAll
    public static void setup(){
        roundtrip = new RoundTripInterface();
        roundtrip.init();
    }

    @Test
    public void alphaTest(){
        String latex = "\\alpha";
        String toMaple = roundtrip.translateFromLaTeX( latex );
        String back = roundtrip.translateFromMaple( toMaple ).trim();
        assertEquals( latex, back, "Expression is not the same!" );
    }
}
