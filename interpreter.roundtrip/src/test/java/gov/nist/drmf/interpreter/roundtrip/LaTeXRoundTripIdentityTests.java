package gov.nist.drmf.interpreter.roundtrip;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.fail;

/**
 * Created by AndreG-P on 08.03.2017.
 */
public class LaTeXRoundTripIdentityTests extends AbstractRoundTrip {
//        test = "\\cos\\frac{1}{2}2";
//        test = "(ab^2c13b+2) \\cdot \\CatalansConstant 2";
//        test = "\\JacobiP{(a! \\mod b^2)!!}{0}{0}@{0}";
//        test = "\\cos \\left( 1^{2^{3+3}*\\iunit} \\right)";
//        test = "\\cos@{2*\\iunit!}!^2 \\mod 2";
//        test = "x^{\\JacobiP{\\iunit}{b}{c}@{d}}!";
//        test = "\\sqrt[\\alpha]{\\cpi}+2\\JacobiP{i}{\\beta}{2}@{12.6}!";
//        test = "q*\\iunit+\\cos(2-\\frac{\\sqrt[\\alpha]{\\cpi}}{2\\JacobiP{i}{\\beta}{2}@{12.6}})";
//        test = "18*\\JacobiP{\\cos{\\sqrt{i}}}{\\frac{1}{\\cpi}}{2.0}@{\\gamma}";
//        test = "\\JacobiP{\\alpha}{b}{c}@{\\cos(\\sqrt{x}\\frac{ \\cos(\\cos(x\\frac{\\cos(x)}{\\sin(xz)}))}{\\tan(\\sin(\\sqrt[x]{absdsd}\\frac{\\cos(x)}{\\sin(xz)}))})}";
//        test = "\\JacobiP{\\alpha\\sqrt[3]{x}\\sin(x\\alpha xyz)\\sqrt[2]{3}}{b\\frac{1}{\\pi}}{1+0\\cos(\\sqrt{x}\\frac{ \\cos(\\cos(x\\frac{\\cos(x)}{\\sin(xz)}))}{\\tan(\\sin(\\sqrt[x]{absdsd}\\frac{\\cos(x)}{\\sin(xz)}))})}@{\\cos(\\sqrt{x}\\frac{ \\cos(\\cos(x\\frac{\\cos(x)}{\\sin(xz)}))}{\\tan(\\sin(\\sqrt[x]{absdsd}\\frac{\\cos(x)}{\\sin(xz)}))})}";


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

    //@Disabled
    @Test
    void fixPointTest() {
        int threshold = 10;
        String cycles = "";

        String test_case = "", new_trans = "";
        for ( String test : latex_test_polynomials ){
            test_case = test;
            for ( int i = 1; i <= threshold; i++ ){
                try {
                    new_trans = translator.oneCycleRoundTripTranslationFromLaTeX( test_case );
                } catch ( Exception e ){
                    e.printStackTrace();
                    fail();
                }
                if ( new_trans.equals( test_case ) ) {
                    cycles += i + ", ";
                    break;
                }
                test_case = new_trans;
                if ( i == threshold )
                    fail( test_case + System.lineSeparator() + new_trans );
            }
        }
        System.out.println( cycles );
    }
}
