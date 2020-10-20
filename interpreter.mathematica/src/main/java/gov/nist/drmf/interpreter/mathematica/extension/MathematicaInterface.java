package gov.nist.drmf.interpreter.mathematica.extension;

import com.wolfram.jlink.Expr;
import com.wolfram.jlink.KernelLink;
import com.wolfram.jlink.MathLinkException;
import com.wolfram.jlink.MathLinkFactory;
import gov.nist.drmf.interpreter.common.cas.IAbortEvaluator;
import gov.nist.drmf.interpreter.common.cas.IComputerAlgebraSystemEngine;
import gov.nist.drmf.interpreter.common.exceptions.ComputerAlgebraSystemEngineException;
import gov.nist.drmf.interpreter.common.replacements.LogManipulator;
import gov.nist.drmf.interpreter.mathematica.common.Commands;
import gov.nist.drmf.interpreter.mathematica.config.MathematicaConfig;
import gov.nist.drmf.interpreter.mathematica.evaluate.SymbolicEquivalenceChecker;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Andre Greiner-Petter
 */
public class MathematicaInterface implements IComputerAlgebraSystemEngine<Expr> {
    private static final Logger LOG = LogManager.getLogger(MathematicaInterface.class.getName());

    /**
     * The kernel
     */
    private KernelLink mathKernel;

    private static MathematicaInterface mathematicaInterface;

    private SymbolicEquivalenceChecker evalChecker;

    private MathematicaInterface() throws MathLinkException {
        init();
    }

    /**
     * Initiates mathematica interface. This function skips if there is already an instance
     * available.
     * @throws MathLinkException if an interface cannot instantiated
     */
    private void init() throws MathLinkException {
        if ( mathematicaInterface != null ) return; // already instantiated

        LOG.debug("Instantiating mathematica interface");
        Path mathPath = MathematicaConfig.loadMathematicaPath();
        String[] args = getDefaultArguments(mathPath);

        KernelLink mathKernel = MathLinkFactory.createKernelLink(args);
        // we don't care about the answer for instantiation, so skip it
        mathKernel.discardAnswer();

        // set encoding to avoid UTF-8 chars of greek letters
        MathematicaConfig.setCharacterEncoding(mathKernel);
        this.mathKernel = mathKernel;
        this.evalChecker = new SymbolicEquivalenceChecker(mathKernel);
        LOG.info("Successfully instantiated mathematica interface");
    }

    private static String[] getDefaultArguments(Path mathPath) {
        return new String[]{
                "-linkmode", "launch",
                "-linkname", mathPath.toString(), "-mathlink"
        };
    }

    /**
     * Get the Mathematica instance
     * @return instance of mathematica interface
     */
    public static MathematicaInterface getInstance() {
        if ( mathematicaInterface != null ) return mathematicaInterface;
        else {
            try {
                mathematicaInterface = new MathematicaInterface();
                return mathematicaInterface;
            } catch (MathLinkException e) {
                LOG.error("Cannot instantiate Mathematica interface: " + e.getMessage());
                mathematicaInterface = null;
                return null;
            }
        }
    }

    /**
     * For tests
     * @return the engine of Mathematica
     */
    KernelLink getMathKernel() {
        return mathKernel;
    }

    @Override
    public Expr enterCommand(String command) throws ComputerAlgebraSystemEngineException {
        try {
            return evaluateToExpression(command);
        } catch (MathLinkException e) {
            throw new ComputerAlgebraSystemEngineException(e);
        }
    }

    public String evaluate(String input) throws MathLinkException {
        Expr expression = evaluateToExpression(input);
        return expression.toString();
    }

    public Expr evaluateToExpression(String input) throws MathLinkException {
        mathKernel.evaluate(input);
        mathKernel.waitForAnswer();
        return mathKernel.getExpr();
    }

    public String convertToFullForm(String input) {
        String fullf = Commands.FULL_FORM.build(input);
        return mathKernel.evaluateToOutputForm(fullf, 0);
    }

    public Set<String> getVariables(String expression) throws MathLinkException {
        String extract = Commands.EXTRACT_VARIABLES.build(expression);
        Expr exs = evaluateToExpression("ToString["+extract+", CharacterEncoding -> \"ASCII\"]");

        Expr[] argsExp = exs.args();
        Set<String> output = new HashSet<>();

        for ( int i = 0; i < argsExp.length; i++ ) {
            Expr arg = argsExp[i];
            output.add(arg.toString());
        }

        return output;
    }

    public void extractAndStoreVariables(String varName, String expression) throws MathLinkException {
        String cmd = Commands.EXTRACT_VARIABLES.build(expression);
        cmd = varName + " := " + cmd + ";";
        evaluate(cmd);
    }

    private static Pattern inPattern = Pattern.compile("^(.*?) \\[Element] (.*)$");

    @Override
    public void setGlobalAssumptions(String... assumptions) throws ComputerAlgebraSystemEngineException {
        for ( int i = 0; i < assumptions.length; i++ ) {
            if ( assumptions[i].contains("Integers") )
                assumptions[i] = assumptions[i].replace("Integers", "PositiveIntegers");
            Matcher m = inPattern.matcher(assumptions[i]);
            if ( m.matches() ) assumptions[i] = "Element[" + m.group(1) + ", " + m.group(2) + "]";
        }
        String cmd = String.join(" && ", assumptions);
        try {
            String result = evaluate("$Assumptions = " + cmd);
            LOG.info("Setup global assumptions: " + result);
        } catch (MathLinkException e) {
            LOG.error("Unable to set global assumptions in Mathematica. Assumptions: " + Arrays.toString(assumptions));
            throw new ComputerAlgebraSystemEngineException(e);
        }
    }

    public SymbolicEquivalenceChecker getEvaluationChecker() {
        return evalChecker;
    }

    public void shutdown() {
        mathKernel.close();
        this.evalChecker = null;
        MathematicaInterface.mathematicaInterface = null;
    }

    @Override
    public void forceGC() {
        LOG.trace("Ignore force GC for Mathematica");
    }

    @Override
    public String buildList(List<String> list) {
        String ls = list.toString();
        return ls.substring(1, ls.length()-1);
    }

    public int checkIfEvaluationIsInRange(String command, int lowerLimit, int upperLimit) throws ComputerAlgebraSystemEngineException {
        try {
            Expr res = mathematicaInterface.evaluateToExpression(command);
            return checkIfEvaluationIsInRange(res, lowerLimit, upperLimit);
        } catch (MathLinkException | NumberFormatException e) {
            throw new ComputerAlgebraSystemEngineException(e);
        }
    }

    public int checkIfEvaluationIsInRange(Expr res, int lowerLimit, int upperLimit) throws ComputerAlgebraSystemEngineException {
        try {
            int nT = res.length();
            LOG.info("Sample of generated test cases [Total: "+nT+"]: " + LogManipulator.shortenOutput(res.toString(), 5));
            if ( nT >= upperLimit ) throw new IllegalArgumentException("Too many test combinations.");
            else if ( nT <= lowerLimit ) throw new IllegalArgumentException("Not enough test combinations.");
            return nT;
            // res should be an integer, testing how many test commands there are!
        } catch (NumberFormatException e) {
            throw new ComputerAlgebraSystemEngineException(e);
        }
    }

    public static Thread getAbortionThread(IAbortEvaluator<Expr> simplifier, int timeout) {
        return new Thread(() -> {
            boolean interrupted = false;
            try {
                Thread.sleep(timeout);
            } catch ( InterruptedException ie ) {
                LOG.debug("Interrupted, no abortion necessary.");
                interrupted = true;
            }

            if ( !interrupted ) {
                LOG.debug("Register an abortion request. Forward it to mathematica engine.");
                simplifier.abort();
            }
        });
    }
}
