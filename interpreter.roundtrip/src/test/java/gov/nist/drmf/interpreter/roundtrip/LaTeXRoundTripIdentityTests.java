package gov.nist.drmf.interpreter.roundtrip;

import org.junit.jupiter.api.Test;

import static org.junit.Assert.fail;

/**
 * Created by AndreG-P on 08.03.2017.
 */
public class LaTeXRoundTripIdentityTests extends AbstractRoundTrip {
    static final String[] latex_test_polynomials = new String[]{
            "(ab^2c13b+2) \\cdot \\CatalansConstant 2",
            "x+x^3-\\frac{(\\iunit*\\alpha+y)*3}{\\left( y^2+x^3 \\right)^z}"
    };

    @Test
    void straight() {
        String test = "x+x^3-\\frac{(\\iunit\\alpha+y)\\cdot 3}{\\left( y^2+x^3 \\right)^z}";
        test = "(ab^2c13b+2) \\cdot \\CatalansConstant 2";
        String maple = translator.translateFromLaTeXToMapleClean( test );
        System.out.println(maple);
        try {
            String latex = translator.translateFromMapleToLaTeXClean( maple );
            String maple2 = translator.translateFromLaTeXToMapleClean( latex );
            String latex2 = translator.translateFromMapleToLaTeXClean( maple2 );
            System.out.println(test);
            System.out.println(maple);
            System.out.println(latex);
            System.out.println(maple2);
            System.out.println(latex2);
        } catch ( Exception e ){
            e.printStackTrace();
            fail();
        }
    }
}
