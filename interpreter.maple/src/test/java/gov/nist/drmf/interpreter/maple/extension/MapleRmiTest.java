package gov.nist.drmf.interpreter.maple.extension;

import gov.nist.drmf.interpreter.common.exceptions.ComputerAlgebraSystemEngineException;
import gov.nist.drmf.interpreter.maple.secure.DefaultMapleRmiServerSubprocessInfo;
import gov.nist.drmf.interpreter.maple.secure.MapleRmiClient;
import gov.nist.drmf.interpreter.maple.setup.AssumeMapleAvailability;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Andre Greiner-Petter
 */
@AssumeMapleAvailability
public class MapleRmiTest {

    private static final Logger LOG = LogManager.getLogger(MapleRmiTest.class.getName());

    private static MapleRmiClient connector;

    @BeforeAll
    static void setup() {
        connector = new MapleRmiClient(new DefaultMapleRmiServerSubprocessInfo());
        connector.start();
    }

    @AfterAll
    static void closeup() {
        connector.stop();
    }

    @Test
    void simpleRmiTest() throws ComputerAlgebraSystemEngineException {
        assertEquals("2", connector.enterCommand("1+1;"));
        assertEquals("3", connector.enterCommand("1+2;"));
        assertEquals("4", connector.enterCommand("1+3;"));
    }

    /**
     * This test case causing a SIGSEGV in native Maple. Since we implemented an RMI workaround,
     * Maple is running in a separate JVM. Hence, this Maple-JVM can crash and we would be able to recover
     * from it. Let's see if this works with this test.
     */
    @Test
    void crashTest() throws ComputerAlgebraSystemEngineException {
        stressTestTwice();
        stressTestTwice();

        // since the JVM crashed fatally (SIGSEGV) there was probably a report created. Time to clean up the main dir.
        try {
            Files.list(Paths.get(".")).forEach(f -> {
                String s = f.getFileName().toString();
                if ( s.matches(".*hs_err_pid\\d+\\.log$") ) {
                    try {
                        Files.deleteIfExists( f.toAbsolutePath() );
                    } catch (IOException e) {
                        LOG.warn("Unable to clean fatal crash report from test case in main directory. Sorry");
                    }
                }
            });
        } catch (IOException e) {
            // nothing to do here.
        }
    }

    private void stressTestTwice() throws ComputerAlgebraSystemEngineException {
        String testExpression = "evalf((sum(JacobiP(n, alpha, beta, z)*(t)^(n), n = 0..infinity))-((2)^(alpha + beta)* (R)^(- 1)*(1 - t + R)^(- alpha)*(1 + t + R)^(- beta)))";
        String testValues = "[[R = Exp[1/6*I*Pi], alpha = 3/2, beta = 3/2, t = -3/2, z = 3/2], [R = Exp[1/6*I*Pi], alpha = 3/2, beta = 3/2, t = -3/2, z = 1/2], [R = Exp[1/6*I*Pi], alpha = 3/2, beta = 3/2, t = -3/2, z = 2]]";

        LOG.debug("Enter test expression.");
        connector.enterCommand("nTest := " + testExpression + ";");
        LOG.debug("Enter test values.");
        connector.enterCommand("nTestVals := " + testValues + ";");
        LOG.debug("Start evaluation.");
        String result = connector.enterCommand("numResults := SpecialNumericalTesterTimeLimit(10.0, nTest, nTestVals, 10);");
//        assertNull(result);
        LOG.debug("Result: " + result);
        LOG.debug("The Maple JVM might have crashed but it should have been restarted again and recovered. So let's test if the we can test simply expression.");
        assertEquals("4", connector.enterCommand("1+3;"));
    }
}
