package gov.nist.drmf.interpreter.evaluation.core.cases;

import gov.nist.drmf.interpreter.evaluation.core.AbstractRoundTrip;
import org.junit.jupiter.api.DynamicTest;

/**
 * Created by AndreG-P on 06.03.2017.
 */
public class FunctionsTests extends AbstractRoundTrip {
    static final String[] test_functions = new String[]{
            "sin(alpha/2)",
            "JacobiP(alpha, beta, n, cos(a*Theta))",
            "cos(sin(I/2)+gamma*ln(x))",
            "(x+1)!",
            "n*sum(1/(y^k),k=1..n)",
//            "Psi(3! mod x^3)"
    };

    static final String[] test_names = new String[]{
            "Simple Sine Test",
            "JacobiP Use Case Test",
            "Complex Sine, Cosine and Logarithm Test",
            "Factorial of Sum Test",
            "Summation Symbol Test",
//            "Tricky PolyGamma with Factorial and Modulus Test"
    };

    protected Iterable<DynamicTest> functionsRoundTripTests(){
        return createFromMapleTestList( test_functions, test_names );
    }
}
