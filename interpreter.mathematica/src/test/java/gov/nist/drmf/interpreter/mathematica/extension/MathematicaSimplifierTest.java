package gov.nist.drmf.interpreter.mathematica.extension;

import gov.nist.drmf.interpreter.common.exceptions.ComputerAlgebraSystemEngineException;
import gov.nist.drmf.interpreter.mathematica.common.AssumeMathematicaAvailability;
import gov.nist.drmf.interpreter.mathematica.wrapper.Expr;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Andre Greiner-Petter
 */
@AssumeMathematicaAvailability
public class MathematicaSimplifierTest {

    private static MathematicaSimplifier simplifier;

    @BeforeAll
    static void setup() {
        simplifier = new MathematicaSimplifier();
    }

    @Test
    void timeoutTest() throws ComputerAlgebraSystemEngineException {
        double timeout = 0.05;
        simplifier.setTimeout(timeout);
        Instant start = Instant.now();
        Expr exp = simplifier.simplify("Integrate[x^a, {x, 0, 1}]", new HashSet<>());
        Instant stop = Instant.now();
        assertTrue(simplifier.wasAborted(exp));
        simplifier.disableTimeout();
        System.out.println("Successfully aborted computation.\n" +
                "Configured timeout after " + timeout*1_000 + "ms\n" +
                "Real elapsed time: " + Duration.between(start, stop).toMillis() + "ms" );
    }

    @Test
    void noTimeoutTest() throws ComputerAlgebraSystemEngineException {
        simplifier.disableTimeout();
        Instant start = Instant.now();
        Expr exp = simplifier.simplify("Integrate[x^a, {x, 0, 1}]", new HashSet<>());
        Instant stop = Instant.now();
        assertFalse(simplifier.wasAborted(exp));
        System.out.println("Configured no timeout.\n" +
                "Elapsed time: " + Duration.between(start, stop).toMillis() + "ms\n" +
                "Result: " + exp.toString());
    }
}
