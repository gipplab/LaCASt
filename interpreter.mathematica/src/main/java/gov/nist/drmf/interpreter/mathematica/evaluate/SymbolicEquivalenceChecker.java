package gov.nist.drmf.interpreter.mathematica.evaluate;

import com.wolfram.jlink.Expr;
import com.wolfram.jlink.ExprFormatException;
import com.wolfram.jlink.MathLinkException;
import gov.nist.drmf.interpreter.mathematica.common.Commands;
import gov.nist.drmf.interpreter.mathematica.extension.MathematicaInterface;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Duration;

/**
 * @author Andre Greiner-Petter
 */
public class SymbolicEquivalenceChecker {
    private static final Logger LOG = LogManager.getLogger(SymbolicEquivalenceChecker.class.getName());

    private final MathematicaInterface mathematica;

    public SymbolicEquivalenceChecker(MathematicaInterface mathematica){
        this.mathematica = mathematica;
    }

    public boolean fullSimplifyDifference( String LHS, String RHS, String assumption ) throws MathLinkException {
        String eq = LHS + " - (" + RHS + ")";
        Expr ex = fullSimplify(eq, assumption);
        return testZero(ex);
    }

    public boolean fullSimplifyDifference( String LHS, String RHS ) throws MathLinkException {
        String eq = LHS + " - (" + RHS + ")";
        Expr ex = fullSimplify(eq);
        return testZero(ex);
    }

    public Expr fullSimplify(String test) throws MathLinkException {
        return fullSimplify(test, null, Duration.ofSeconds(-1));
    }

    public Expr fullSimplify(String test, Duration timeout) throws MathLinkException {
        return fullSimplify(test, null, timeout);
    }

    public Expr fullSimplify(String test, String assumption) throws MathLinkException {
        return fullSimplify( test, assumption, Duration.ofSeconds(-1) );
    }

    private String latestTestExpression = "";

    public String getLatestTestExpression() {
        return latestTestExpression;
    }

    public Expr fullSimplify(String test, String assumption, Duration timeout) throws MathLinkException {
        latestTestExpression = "";
        String expr = assumption == null ?
                Commands.FULL_SIMPLIFY.build(test) :
                Commands.FULL_SIMPLIFY_ASSUMPTION.build(test, assumption);
        latestTestExpression = expr;

        LOG.debug("Start simplification: " + expr);
        return mathematica.evaluateToExpression(expr, timeout);
    }

    public boolean testZero(Expr expr) {
        return isNumber(expr, 0);
    }

    public boolean isNumber(Expr expr, double number) {
        if ( expr != null && expr.numberQ() ) {
            try {
                double d = expr.asDouble();
                return d == number;
            } catch (ExprFormatException e) {
                LOG.info("Not equal! " + expr.toString());
            }
        }
        return false;
    }

    public boolean isTrue(Expr expr) {
        if ( expr == null ) return false;
        return expr.trueQ();
    }
}
