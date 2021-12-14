package giv.nist.drmf.interpreter.core.cases;

import gov.nist.drmf.interpreter.common.exceptions.ComputerAlgebraSystemEngineException;
import giv.nist.drmf.interpreter.core.AbstractRoundTrip;
import gov.nist.drmf.interpreter.maple.wrapper.MapleException;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Created by AndreG-P on 08.03.2017.
 */
public class LaTeXRoundTripIdentityTests extends AbstractRoundTrip {
    static final String[] latex_test_polynomials = new String[]{
            "(ab^2c13b+2) \\cdot \\CatalansConstant 2",
            "x+x^3-\\frac{(\\iunit*\\alpha+y)*3}{\\left( y^2+x^3 \\right)^z}"
    };

    protected void straight() throws ComputerAlgebraSystemEngineException, MapleException {
        boolean latex_equ = false, maple_equ = false;
        int threshold = 10;
        String maple, latex, firstMaple, prev_latex, prev_maple;
        for ( int i = 0; i < latex_test_polynomials.length; i++ ){
            prev_latex = latex_test_polynomials[i];
            firstMaple = translator.translateFromLaTeXToMapleClean( prev_latex, null );
            prev_maple = firstMaple;
            for ( int j = 0; j < threshold; j++ ){
                latex = translator.translateFromMapleToLaTeXClean( prev_maple );
                maple = translator.translateFromLaTeXToMapleClean( latex, null );
                if ( latex.equals( prev_latex ) ){
                    latex_equ = true;
                } else if ( translator.getMapleSimplifier().isEquivalent(firstMaple, maple) ){
                    maple_equ = true;
                }
                if ( latex_equ && maple_equ ) break;
                prev_latex = latex;
                prev_maple = maple;
            }

            if ( !(latex_equ && maple_equ) )
                fail( "Round trip test failed for: " + prev_latex );
        }
    }
}
