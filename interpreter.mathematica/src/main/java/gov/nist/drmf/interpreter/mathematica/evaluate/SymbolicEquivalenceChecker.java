package gov.nist.drmf.interpreter.mathematica.evaluate;

import com.wolfram.jlink.Expr;
import com.wolfram.jlink.ExprFormatException;
import com.wolfram.jlink.KernelLink;
import com.wolfram.jlink.MathLinkException;
import gov.nist.drmf.interpreter.mathematica.common.Commands;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author Andre Greiner-Petter
 */
public class SymbolicEquivalenceChecker {
    private static final Logger LOG = LogManager.getLogger(SymbolicEquivalenceChecker.class.getName());

    private final KernelLink engine;

    public SymbolicEquivalenceChecker( KernelLink engine ){
        this.engine = engine;
    }

    public boolean fullSimplifyDifference( String LHS, String RHS, String assumtpions ) throws MathLinkException {
        String eq = LHS + " - (" + RHS + ")";
        String simplify = Commands.FULL_SIMPLIFY.build(eq);
        String test = Commands.ASSUMING.build(assumtpions, simplify);

        engine.evaluate(test);
        engine.waitForAnswer();
        return testZero();
    }

    public boolean fullSimplifyDifference( String LHS, String RHS ) throws MathLinkException {
        String eq = LHS + " - (" + RHS + ")";
        String simplify = Commands.FULL_SIMPLIFY.build(eq);

        engine.evaluate(simplify);
        engine.waitForAnswer();
        return testZero();
    }

    private boolean testZero() throws MathLinkException {
        Expr expr = engine.getExpr();
        if ( expr.numberQ() ) {
            try {
                double d = expr.asDouble();
                return d == 0;
            } catch (ExprFormatException e) {
                LOG.info("Not equal! " + expr.toString());
            }
        }
        return false;
    }
}
