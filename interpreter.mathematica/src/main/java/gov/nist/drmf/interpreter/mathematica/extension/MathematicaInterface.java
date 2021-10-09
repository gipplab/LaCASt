package gov.nist.drmf.interpreter.mathematica.extension;

import gov.nist.drmf.interpreter.common.cas.IComputerAlgebraSystemEngine;
import gov.nist.drmf.interpreter.common.exceptions.ComputerAlgebraSystemEngineException;
import gov.nist.drmf.interpreter.common.replacements.LogManipulator;
import gov.nist.drmf.interpreter.mathematica.common.Commands;
import gov.nist.drmf.interpreter.mathematica.config.MathematicaConfig;
import gov.nist.drmf.interpreter.mathematica.evaluate.SymbolicEquivalenceChecker;
import gov.nist.drmf.interpreter.mathematica.wrapper.Expr;
import gov.nist.drmf.interpreter.mathematica.wrapper.KernelLink;
import gov.nist.drmf.interpreter.mathematica.wrapper.MathLinkException;
import gov.nist.drmf.interpreter.mathematica.wrapper.MathLinkFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Andre Greiner-Petter
 */
public final class MathematicaInterface implements IComputerAlgebraSystemEngine {
    private static final Logger LOG = LogManager.getLogger(MathematicaInterface.class.getName());

    public static final String MATH_ABORTION_SIGNAL = "$Aborted";

    /**
     * The kernel
     */
    private KernelLink mathKernel;

    private static MathematicaInterface mathematicaInterface;

    private final SymbolicEquivalenceChecker evalChecker;

    private MathematicaInterface() throws MathLinkException {
        init();
        this.evalChecker = new SymbolicEquivalenceChecker(this);
    }

    /**
     * Initiates mathematica interface. This function skips if there is already an instance
     * available.
     * @throws MathLinkException if an interface cannot instantiated
     */
    private void init() throws MathLinkException {
        if ( mathematicaInterface != null ) return; // already instantiated

        LOG.info("Instantiating mathematica interface");
        Path mathPath = MathematicaConfig.loadMathematicaPath();
        assert mathPath != null;
        String[] args = getDefaultArguments(mathPath);

        KernelLink mathKernel;
        try {
            mathKernel = MathLinkFactory.createKernelLink(args);
            // we don't care about the answer for instantiation, so skip it
            mathKernel.discardAnswer();
        } catch (MathLinkException mle) {
            LOG.warn("Unable to connect with Wolfram kernel. Try to recover and activate license first.");
            try {
                initLicense(mathPath, MathematicaConfig.loadMathematicaLicense());
                mathKernel = MathLinkFactory.createKernelLink(args);
                // we don't care about the answer for instantiation, so skip it
                mathKernel.discardAnswer();
            } catch ( IOException | InterruptedException e ) {
                LOG.warn("Unable to activate Wolfram license.", e);
                throw mle;
            }
        }

        // set encoding to avoid UTF-8 chars of greek letters
        MathematicaConfig.setCharacterEncoding(mathKernel);
        this.mathKernel = mathKernel;
        LOG.info("Successfully instantiated mathematica interface");
    }

    private void initLicense(Path mathPath, String license) throws IOException, InterruptedException {
        LOG.info("Try to activate Wolfram license.");
        ProcessBuilder procBuilder = new ProcessBuilder(mathPath.toAbsolutePath().toString(), "-activate", license);
        Process proc = procBuilder.start();
        proc.waitFor();
        if ( proc.exitValue() != 0 ) {
            LOG.error("Unable to activate Wolfram license.");
        } else {
            LOG.info("Successfully activated Wolfram license. Ready to initiate.");
        }
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
                if ( MathematicaConfig.isMathematicaMathPathAvailable() && MathematicaConfig.isSystemEnvironmentVariableProperlySet() ) {
                    mathematicaInterface = new MathematicaInterface();
                }
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

    public Expr internalEnterCommand(String command) throws ComputerAlgebraSystemEngineException {
        try {
            return evaluateToExpression(command);
        } catch (MathLinkException e) {
            throw new ComputerAlgebraSystemEngineException(e);
        }
    }

    @Override
    public String enterCommand(String command) throws ComputerAlgebraSystemEngineException {
        return internalEnterCommand(command).toString();
    }

    public String evaluate(String input) throws MathLinkException {
        Expr expression = evaluateToExpression(input);
        return expression.toString();
    }

    public Expr evaluateToExpression(String input) throws MathLinkException {
        try {
            mathKernel.evaluate(input);
            mathKernel.waitForAnswer();
            return mathKernel.getExpr();
        } catch (MathLinkException mle) {
            String msg = mle.getMessage();
            if ( msg != null && msg.toLowerCase().contains("lost") ) {
                LOG.error("Lost mathematica kernel connection. Try to recover by re-initiating kernel.");
                shutdown();
                init();
            }
            mathKernel.evaluate(input);
            mathKernel.waitForAnswer();
            return mathKernel.getExpr();
        }
    }

    public Expr evaluateToExpression(String input, Duration timeout) throws MathLinkException {
        input = wrapInTimeout(input, timeout);
        return evaluateToExpression(input);
    }

    public static String wrapInTimeout(String input, Duration timeout) {
        if ( timeout != null && !timeout.isNegative() ) {
            String timeoutStr = "" + timeout.getSeconds();
            if ( timeout.toMillisPart() > 0 ) timeoutStr += "." + timeout.toMillisPart();
            input = Commands.TIME_CONSTRAINED.build( input, timeoutStr );
        }
        return input;
    }

    /**
     * This method isn't fail-safe. Use {@link #evaluateToExpression(String)} in combination of
     * {@link Commands#build(String...)} instead.
     * @param input command
     * @return returns the full output form of the input
     */
    protected String convertToFullForm(String input) {
        String fullf = Commands.FULL_FORM.build(input);
        return mathKernel.evaluateToOutputForm(fullf, 0);
    }

    public Set<String> getVariables(String expression) throws MathLinkException {
        String extract = Commands.EXTRACT_VARIABLES.build(expression);
        Expr exs = evaluateToExpression("ToString["+extract+", CharacterEncoding -> \"ASCII\"]");

        Expr[] argsExp = exs.args();
        Set<String> output = new HashSet<>();

        for (Expr arg : argsExp) {
            output.add(arg.toString());
        }

        return output;
    }

    public void extractAndStoreVariables(String varName, String expression) throws MathLinkException {
        String cmd = Commands.EXTRACT_VARIABLES.build(expression);
        cmd = varName + " := " + cmd + ";";
        evaluate(cmd);
    }

    public SymbolicEquivalenceChecker getEvaluationChecker() {
        return evalChecker;
    }

    public void shutdown() {
        mathKernel.close();
        MathematicaInterface.mathematicaInterface = null;
    }

    @Override
    public void forceGC() {
        try {
            LOG.debug("Clear native Mathematica system cache");
            evaluate(Commands.CLEAR_CACHE.build());
        } catch (MathLinkException e) {
            LOG.error("Unable to clear system cache in Mathematica", e);
        }
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
}
