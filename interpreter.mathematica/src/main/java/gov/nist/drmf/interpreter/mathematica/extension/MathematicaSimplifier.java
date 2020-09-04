package gov.nist.drmf.interpreter.mathematica.extension;

import com.wolfram.jlink.Expr;
import com.wolfram.jlink.MathLinkException;
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

    public MathematicaSimplifier() {
        this.mathematicaInterface = MathematicaInterface.getInstance();
        this.miEquiChecker = mathematicaInterface.getEvaluationChecker();
    }

    @Override
    public void setTimeout(int timeoutInSeconds) {
        LOG.warn("Timeout for Mathematica is not implemented yet...");
    }

    @Override
    public Expr simplify(String expr, Set<String> requiredPackages) throws ComputerAlgebraSystemEngineException {
        logReqPackages(requiredPackages);
        try {
            return miEquiChecker.fullSimplify(expr);
        } catch (MathLinkException e) {
            throw new ComputerAlgebraSystemEngineException(e);
        }
    }

    @Override
    public Expr simplify(String expr, String assumption, Set<String> requiredPackages) throws ComputerAlgebraSystemEngineException {
        logReqPackages(requiredPackages);
        try {
            return miEquiChecker.fullSimplify(expr, assumption);
        } catch (MathLinkException e) {
            throw new ComputerAlgebraSystemEngineException(e);
        }
    }

    private void logReqPackages(Set<String> requiredPackages) {
        if ( requiredPackages != null && !requiredPackages.isEmpty() ) {
            LOG.warn("Mathematica interface does not support required packages yet. They will be ignored!");
        }
    }

    @Override
    public boolean isAsExpected(Expr in, String expect) {
        try {
            return miEquiChecker.testZero(in);
        } catch (MathLinkException e) {
            LOG.error("Cannot check if expression is zero " + in.toString());
            return false;
        }
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
