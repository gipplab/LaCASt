package gov.nist.drmf.interpreter.roundtrip;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

/**
 *
 * Created by AndreG-P on 06.03.2017.
 */
public class PolynomialTests extends AbstractRoundTrip {
    private static final String[] test_polynomials = new String[]{
            "(infinity+Catalan/2)^gamma",
            "gamma+alpha^5-I^(x/5)+Catalan",
            "alpha+2*beta+3*I-kappa/Theta+x*y^2.3",
            "x + x^2 - ((1-gamma)*x/2)^I",
            "1/((a+2)/(b^(Catalan/I)/(alpha*q^I*x/3)*alpha))",
            "((x^a)^b)^c"
    };

    private static final String[] test_names = new String[]{
            "Simple Constants Test",
            "Greek letters and Constants Test 1/2",
            "Greek letters and Constants Test 2/2",
            "Polynomial Test",
            "Complex Nested Fractions Test",
            "Nested Power Tests"
    };

    @TestFactory
    Iterable<DynamicTest> polynomialRoundTripTests(){
        return createTestList( test_polynomials, test_names );
    }

}
