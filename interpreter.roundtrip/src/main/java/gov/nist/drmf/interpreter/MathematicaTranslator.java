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
import gov.nist.drmf.interpreter.evaluation.ICASEngineNumericalEvaluator;
import gov.nist.drmf.interpreter.evaluation.ICASEngineSymbolicEvaluator;
import gov.nist.drmf.interpreter.evaluation.INumericalEvaluationScripts;
import gov.nist.drmf.interpreter.mathematica.MathematicaInterface;
import gov.nist.drmf.interpreter.mathematica.evaluate.SymbolicEquivalenceChecker;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.List;
import java.util.Observable;
import java.util.regex.Pattern;

/**
 * @author Andre Greiner-Petter
 */
public class MathematicaTranslator implements
        IConstraintTranslator,
        IComputerAlgebraSystemEngine<String>,
        ICASEngineSymbolicEvaluator<Expr>,
        ICASEngineNumericalEvaluator<Expr>
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

    @Override
    public String buildList(List<String> list) {
        String ls = list.toString();
        return ls.substring(1, ls.length()-1);
    }

    @Override
    public void update(Observable o, Object arg) {
        // nothing to do here
    }

    /**
     * Mathematica Numerical Tests Workflow:
     *
     * 1) Get Variables in Expression
     * 2) In case of Constraints
     *  2.1) ConstVars from Variable
     *  2.2) Get ConstVars - Value Pairs
     * 3) In Case of special values
     *  3.1) Special Variables from Variables
     *  3.2) Special Variables - Value Pairs
     * 4) Rest of Variables - Value pairs
     * 1) Variables - Values paris
     */

    @Override
    public String storeVariables(String expression, List<String> testValues) {
        return null;
    }

    @Override
    public String storeConstraintVariables(String variableName, List<String> constraintVariables, List<String> constraintValues) {
        // 1) variableName := Complement[variableName, contVars]
        // 2)

        // first, get constraint variables that are actually in var
        // Intersection[list1,list2];
        return null;
    }

    @Override
    public String storeExtraVariables(String variableName, List<String> extraVariables, List<String> extraValues) {
        return null;
    }

    @Override
    public String setConstraints(List<String> constraints) {
        return null;
    }

    @Override
    public String buildTestCases(String constraintsName, String variableNames, String constraintVariableNames, String extraVariableNames, int maxCombis) throws ComputerAlgebraSystemEngineException, IllegalArgumentException {
        return null;
    }

    @Override
    public Expr performNumericalTests(String expression, String testCasesName, String postProcessingMethodName, int precision) throws ComputerAlgebraSystemEngineException {
        return null;
    }

    @Override
    public ResultType getStatusOfResult(Expr results) throws ComputerAlgebraSystemEngineException {
        return null;
    }
}
