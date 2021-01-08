package gov.nist.drmf.interpreter.mathematica.extension;

import com.wolfram.jlink.Expr;
import com.wolfram.jlink.MathLinkException;
import gov.nist.drmf.interpreter.common.cas.ICASEngineSymbolicEvaluator;
import gov.nist.drmf.interpreter.common.exceptions.ComputerAlgebraSystemEngineException;
import gov.nist.drmf.interpreter.mathematica.evaluate.SymbolicEquivalenceChecker;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Duration;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * @author Andre Greiner-Petter
 */
public class MathematicaSimplifier implements ICASEngineSymbolicEvaluator<Expr> {
    private static final Logger LOG = LogManager.getLogger(MathematicaSimplifier.class.getName());

    private final MathematicaInterface mathematicaInterface;
    private final SymbolicEquivalenceChecker miEquiChecker;

    private Duration timeout = Duration.ofSeconds(-1);

    public MathematicaSimplifier() {
        this.mathematicaInterface = MathematicaInterface.getInstance();
        this.miEquiChecker = mathematicaInterface.getEvaluationChecker();
    }

    @Override
    public void setTimeout(double timeoutInSeconds) {
        this.timeout = Duration.ofMillis( (int)(timeoutInSeconds * 1_000) );
    }

    @Override
    public void disableTimeout() {
        this.timeout = Duration.ofSeconds(-1);
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
    public boolean isTrue(Expr in) {
        return in.trueQ();
    }

    @Override
    public boolean isAsExpected(Expr in, double expect) {
        return miEquiChecker.isNumber(in, expect) || miEquiChecker.isTrue(in);
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
            Expr res;
            if ( assumption == null )
                res = miEquiChecker.fullSimplify(expression, timeout);
            else res = miEquiChecker.fullSimplify(expression, assumption, timeout);
            return res;
        } catch (MathLinkException e) {
            throw new ComputerAlgebraSystemEngineException(e);
        }
    }

    @Override
    public boolean wasAborted(Expr result) {
        return result.toString().matches(Pattern.quote(MathematicaInterface.MATH_ABORTION_SIGNAL));
    }
}
