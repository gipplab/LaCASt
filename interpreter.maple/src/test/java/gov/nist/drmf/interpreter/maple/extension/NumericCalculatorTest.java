package gov.nist.drmf.interpreter.maple.extension;

import com.maplesoft.openmaple.Algebraic;
import gov.nist.drmf.interpreter.common.cas.IComputerAlgebraSystemEngine;
import gov.nist.drmf.interpreter.common.exceptions.ComputerAlgebraSystemEngineException;
import gov.nist.drmf.interpreter.maple.MapleConnector;
import gov.nist.drmf.interpreter.maple.setup.AssumeMapleAvailability;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * @author Andre Greiner-Petter
 */
@AssumeMapleAvailability
public class NumericCalculatorTest {
    private static final Logger LOG = LogManager.getLogger(NumericCalculatorTest.class.getName());

    private static IComputerAlgebraSystemEngine<Algebraic> engine;

    @BeforeAll
    static void setup() throws ComputerAlgebraSystemEngineException {
        MapleConnector connector = new MapleConnector();
        connector.loadNumericProcedures();
        engine = connector.getCASEngine();
    }

    /**
     * TODO: Careful, this test case crashes your VM with a SIGSEGV! No way to recover at the moment.
     */
    @Test
    @Disabled
    void crashTest() throws ComputerAlgebraSystemEngineException {
        String testExpression = "evalf((sum(JacobiP(n, alpha, beta, z)*(t)^(n), n = 0..infinity))-((2)^(alpha + beta)* (R)^(- 1)*(1 - t + R)^(- alpha)*(1 + t + R)^(- beta)))";
        String testValues = "[[R = Exp[1/6*I*Pi], alpha = 3/2, beta = 3/2, t = -3/2, z = 3/2], [R = Exp[1/6*I*Pi], alpha = 3/2, beta = 3/2, t = -3/2, z = 1/2], [R = Exp[1/6*I*Pi], alpha = 3/2, beta = 3/2, t = -3/2, z = 2]]";

        LOG.debug("Enter test expression.");
        engine.enterCommand("nTest := " + testExpression + ";");
        LOG.debug("Enter test values.");
        engine.enterCommand("nTestVals := " + testValues + ";");
        LOG.debug("Start evaluation.");
        engine.enterCommand("numResults := SpecialNumericalTesterTimeLimit(10.0, nTest, nTestVals, 10);");
    }

}
