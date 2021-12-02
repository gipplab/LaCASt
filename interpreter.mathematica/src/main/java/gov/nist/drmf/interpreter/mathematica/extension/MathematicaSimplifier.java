package gov.nist.drmf.interpreter.mathematica.extension;


import gov.nist.drmf.interpreter.common.cas.AbstractCasEngineSymbolicEvaluator;
import gov.nist.drmf.interpreter.common.eval.EvaluatorType;
import gov.nist.drmf.interpreter.common.exceptions.ComputerAlgebraSystemEngineException;
import gov.nist.drmf.interpreter.mathematica.core.MathematicaInterface;
import gov.nist.drmf.interpreter.mathematica.evaluate.SymbolicEquivalenceChecker;
import gov.nist.drmf.interpreter.mathematica.wrapper.Expr;
import gov.nist.drmf.interpreter.mathematica.wrapper.MathLinkException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Andre Greiner-Petter
 */
public class MathematicaSimplifier extends AbstractCasEngineSymbolicEvaluator<Expr> {
    private static final Logger LOG = LogManager.getLogger(MathematicaSimplifier.class.getName());

    private final MathematicaInterface mathematicaInterface;
    private final SymbolicEquivalenceChecker miEquiChecker;

    private Duration timeout = Duration.ofSeconds(-1);

    public MathematicaSimplifier() {
        this.mathematicaInterface = MathematicaInterface.getInstance();
        assert mathematicaInterface != null;
        this.miEquiChecker = mathematicaInterface.getEvaluationChecker();
    }

    @Override
    public void setTimeout(EvaluatorType type, double timeLimit) {
        if ( EvaluatorType.SYMBOLIC.equals(type) ) this.setTimeout(timeLimit);
    }

    @Override
    public void setTimeout(double timeoutInSeconds) {
        this.timeout = Duration.ofMillis( (int)(timeoutInSeconds * 1_000) );
    }

    @Override
    public void disableTimeout(EvaluatorType type) {
        if ( EvaluatorType.SYMBOLIC.equals(type) )
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

    @Override
    public String getLatestTestExpression() {
        return miEquiChecker.getLatestTestExpression();
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

    private static final Pattern inPattern = Pattern.compile("^(.*?) \\\\\\[Element] (.*)$");

    @Override
    public void setGlobalSymbolicAssumptions(List<String> assumptions) throws ComputerAlgebraSystemEngineException {
        assumptions.replaceAll(in -> {
            if ( in.contains("Integers") ) in = in.replace("Integers", "PositiveIntegers");
            Matcher m = inPattern.matcher(in);
            if ( m.matches() ) in = "Element[" + m.group(1) + ", " + m.group(2) + "]";
            return in;
        });

        String cmd = String.join(" && ", assumptions);
        try {
            String result = mathematicaInterface.evaluate("$Assumptions = " + cmd);
            LOG.info("Setup global assumptions: " + cmd + "; returned: " + result);
        } catch (MathLinkException e) {
            LOG.error("Unable to set global assumptions in Mathematica. Assumptions: " + assumptions);
            throw new ComputerAlgebraSystemEngineException(e);
        }
    }

    public void resetGlobalAssumptions() throws ComputerAlgebraSystemEngineException {
        try {
            LOG.debug("Unset global assumptions ($Assumptions)");
            mathematicaInterface.evaluate("$Assumptions = True");
        }
        catch ( MathLinkException e ) {
            LOG.error("Unable to reset global assumptions in Mathematica.");
            throw new ComputerAlgebraSystemEngineException(e);
        }
    }
}
