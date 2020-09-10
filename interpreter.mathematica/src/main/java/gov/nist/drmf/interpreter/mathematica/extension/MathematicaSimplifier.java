package gov.nist.drmf.interpreter.mathematica.extension;

import com.wolfram.jlink.Expr;
import com.wolfram.jlink.MathLinkException;
import gov.nist.drmf.interpreter.common.cas.IAbortEvaluator;
import gov.nist.drmf.interpreter.common.cas.ICASEngineSymbolicEvaluator;
import gov.nist.drmf.interpreter.common.exceptions.ComputerAlgebraSystemEngineException;
import gov.nist.drmf.interpreter.mathematica.evaluate.SymbolicEquivalenceChecker;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Set;
import java.util.regex.Pattern;

import static gov.nist.drmf.interpreter.mathematica.evaluate.SymbolicEquivalenceChecker.MATH_ABORTION_SIGNAL;

/**
 * @author Andre Greiner-Petter
 */
public class MathematicaSimplifier implements ICASEngineSymbolicEvaluator<Expr> {
    private static final Logger LOG = LogManager.getLogger(MathematicaSimplifier.class.getName());

    private final MathematicaInterface mathematicaInterface;
    private final SymbolicEquivalenceChecker miEquiChecker;

    private int timeout = -1;

    public MathematicaSimplifier() {
        this.mathematicaInterface = MathematicaInterface.getInstance();
        this.miEquiChecker = mathematicaInterface.getEvaluationChecker();
    }

    @Override
    public void setTimeout(double timeoutInSeconds) {
        this.timeout = (int)(1_000*timeoutInSeconds);
//        LOG.warn("Timeout for Mathematica is not implemented yet...");
    }

    public void disableTimeout() {
        this.timeout = -1;
    }

    @Override
    public Expr simplify(String expr, Set<String> requiredPackages) throws ComputerAlgebraSystemEngineException {
        return fullSimplify(expr, null, requiredPackages);
    }

    @Override
    public Expr simplify(String expr, String assumption, Set<String> requiredPackages) throws ComputerAlgebraSystemEngineException {
        return fullSimplify(expr, assumption, requiredPackages);
    }

    private void logReqPackages(Set<String> requiredPackages) {
        if ( requiredPackages != null && !requiredPackages.isEmpty() ) {
            LOG.warn("Mathematica interface does not support required packages yet. They will be ignored!");
        }
    }

    @Override
    public boolean isAsExpected(Expr in, double expect) {
        return miEquiChecker.isNumber(in, expect);
    }

    @Override
    public boolean isConditionallyExpected(Expr in, double expect) {
        String head = in.head().toString();
        if ( head.matches("ConditionalExpression") ) {
            Expr[] args = in.args();
            if ( args.length != 2 ) return false;
            Expr result = args[0];
            return isAsExpected(result, expect);
        }
        return false;
    }

    @Override
    public String getCondition(Expr in) {
        String head = in.head().toString();
        if ( head.matches("ConditionalExpression") ) {
            Expr[] args = in.args();
            if ( args.length != 2 ) return "";
            return args[1].toString();
        }
        return "";
    }

    private Expr fullSimplify(String expression, String assumption, Set<String> requiredPackages) throws ComputerAlgebraSystemEngineException {
        logReqPackages(requiredPackages);
        try {
            Expr res = null;
            Thread abortionThread = null;
            if ( timeout > 0 ) {
                abortionThread = getAbortionThread(this, timeout);
                abortionThread.start();
            }
            if ( assumption == null )
                res = miEquiChecker.fullSimplify(expression);
            else res = miEquiChecker.fullSimplify(expression, assumption);
            if ( abortionThread != null )
                abortionThread.interrupt();
            return res;
        } catch (MathLinkException e) {
            throw new ComputerAlgebraSystemEngineException(e);
        }
    }

    private static Thread getAbortionThread(MathematicaSimplifier simplifier, int timeout) {
        return new Thread(() -> {
            boolean interrupted = false;
            try {
                Thread.sleep(timeout);
            } catch ( InterruptedException ie ) {
                LOG.debug("Interrupted, no abortion necessary.");
                interrupted = true;
            }

            if ( !interrupted ) {
                simplifier.abort();
            }
        });
    }

    @Override
    public void abort() {
        miEquiChecker.abort();
    }

    @Override
    public boolean wasAborted(Expr result) {
        return result.toString().matches(Pattern.quote(MATH_ABORTION_SIGNAL));
    }
}
