package gov.nist.drmf.interpreter;

import com.wolfram.jlink.Expr;
import com.wolfram.jlink.MathLinkException;
import gov.nist.drmf.interpreter.cas.translation.SemanticLatexTranslator;
import gov.nist.drmf.interpreter.common.constants.GlobalPaths;
import gov.nist.drmf.interpreter.common.constants.Keys;
import gov.nist.drmf.interpreter.common.exceptions.ComputerAlgebraSystemEngineException;
import gov.nist.drmf.interpreter.common.exceptions.TranslationException;
import gov.nist.drmf.interpreter.common.grammar.IComputerAlgebraSystemEngine;
import gov.nist.drmf.interpreter.constraints.IConstraintTranslator;
import gov.nist.drmf.interpreter.evaluation.ICASEngineSymbolicEvaluator;
import gov.nist.drmf.interpreter.mathematica.MathematicaInterface;
import gov.nist.drmf.interpreter.mathematica.evaluate.SymbolicEquivalenceChecker;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.regex.Pattern;

/**
 * @author Andre Greiner-Petter
 */
public class MathematicaTranslator implements
        IConstraintTranslator,
        IComputerAlgebraSystemEngine<String>,
        ICASEngineSymbolicEvaluator<Expr>
{
    private static final Logger LOG = LogManager.getLogger(MathematicaTranslator.class.getName());

    public static final String MATH_ABORTION_SIGNAL = "$Aborted";

    /**
     * Interface to Mathematica
     */
    private MathematicaInterface mi;

    private SymbolicEquivalenceChecker miEquiChecker;

    /**
     * The interface to interact with the DLMF LaTeX translator
     */
    private SemanticLatexTranslator dlmfInterface;

    public MathematicaTranslator() {}

    public void init() throws IOException {
        // setup logging
        System.setProperty( Keys.KEY_SYSTEM_LOGGING, GlobalPaths.PATH_LOGGING_CONFIG.toString() );

        mi = MathematicaInterface.getInstance();
        miEquiChecker = mi.getEvaluationChecker();
        LOG.debug("Initialized Mathematica Interface");


        dlmfInterface = new SemanticLatexTranslator( Keys.KEY_MATHEMATICA );
        dlmfInterface.init( GlobalPaths.PATH_REFERENCE_DATA );
        LOG.debug("Initialized DLMF LaTeX Interface.");
    }

    @Override
    public String translate(String expression, String label) throws TranslationException {
        return dlmfInterface.translate(expression, label);
    }

    @Override
    public String enterCommand(String command) throws ComputerAlgebraSystemEngineException {
        try {
            return mi.evaluate(command);
        } catch (MathLinkException e) {
            throw new ComputerAlgebraSystemEngineException(e);
        }
    }

    @Override
    public void forceGC() throws ComputerAlgebraSystemEngineException {
        // nothing to do here, hopefully we don't need to invoke GC for mathematica
    }

    @Override
    public Expr simplify(String expr) throws ComputerAlgebraSystemEngineException {
        try {
            return miEquiChecker.fullSimplify(expr);
        } catch (MathLinkException e) {
            throw new ComputerAlgebraSystemEngineException(e);
        }
    }

    @Override
    public Expr simplify(String expr, String assumption) throws ComputerAlgebraSystemEngineException {
        try {
            return miEquiChecker.fullSimplify(expr, assumption);
        } catch (MathLinkException e) {
            throw new ComputerAlgebraSystemEngineException(e);
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
