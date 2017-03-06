package gov.nist.drmf.interpreter.roundtrip;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

/**
 * Created by AndreG-P on 06.03.2017.
 */
public class FunctionsTests extends AbstractRoundTrip {
    private static final String[] test_functions = new String[]{
            "sin(alpha/2)",
            "JacobiP(alpha, beta, n, cos(a*Theta))",
            "cos(sin(I/2)+gamma*ln(x))",
            "(x+1)!",
            "Psi(3! modp x^3)"
    };

    private static final String[] test_names = new String[]{
            "Simple Sine Test",
            "JacobiP Use Case Test",
            "Complex Sine, Cosine and Logarithm Test",
            "Factorial of Sum Test",
            "Tricky PolyGamma with Factorial and Modulus Test"
    };

    @TestFactory
    Iterable<DynamicTest> functionsRoundTripTests(){
        return createTestList( test_functions, test_names );
    }
}
