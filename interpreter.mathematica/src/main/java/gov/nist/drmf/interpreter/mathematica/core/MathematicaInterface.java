package gov.nist.drmf.interpreter.mathematica.core;

import gov.nist.drmf.interpreter.common.cas.IComputerAlgebraSystemEngine;
import gov.nist.drmf.interpreter.common.exceptions.CASUnavailableException;
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
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
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

    private MathematicaInterface() {
        this.evalChecker = new SymbolicEquivalenceChecker(this);
    }

    /**
     * Initiates mathematica interface. This function skips if there is already an instance
     * available.
     * @throws MathLinkException if an interface cannot instantiated
     * @throws CASUnavailableException if the CAS is unavailable
     */
    private void init() throws MathLinkException, CASUnavailableException {
        if ( mathematicaInterface != null && mathKernel != null) return; // already initiated

        if ( !MathematicaConfig.isMathematicaPresent() ) throw new CASUnavailableException();
        LOG.info("Init mathematica interface");

        // Since version 2.1.0 of J/Link, we can use this property to hack around LD_LIBRARY_PATH requirements
        // when this method is triggered, it already passed the MathematicaConfig#isMathematicaPresent check
        // and thus the JLink path in the config can be assumed valid
        System.setProperty("com.wolfram.jlink.libdir", MathematicaConfig.getJLinkNativePath());

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
            } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException |
                    InstantiationException | InvocationTargetException e) {
                LOG.warn("Unable to access J/Link library and load necessary classes; " + e.getMessage());
                throw new CASUnavailableException("Unable to access Mathematica's interface J/Link library", e);
            }
        } catch (MalformedURLException | ClassNotFoundException | NoSuchMethodException |
                IllegalAccessException | InstantiationException | InvocationTargetException e) {
            LOG.warn("Unable to access J/Link library and load necessary classes; " + e.getMessage());
            throw new CASUnavailableException("Unable to access Mathematica's interface J/Link library", e);
        }

        // set encoding to avoid UTF-8 chars of greek letters
        setCharacterEncoding(mathKernel);
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

    /**
     * Returns the setup arguments for mathlink.
     * @param mathPath the path to the Wolfram Engine executable file
     * @return the arguments to launch the math kernel
     */
    private static String[] getDefaultArguments(Path mathPath) {
        return new String[]{
                "-linkmode", "launch",
                "-linkname", mathPath.toString(), "-mathlink"
        };
    }

    private void setCharacterEncoding(KernelLink engine){
        try {
            engine.evaluate("$CharacterEncoding = \"ASCII\"");
            engine.discardAnswer();
        } catch (MathLinkException mle) {
            LOG.warn("Cannot change character encoding in Mathematica.");
            engine.clearError();
            engine.newPacket();
        }
    }

    /**
     * Get the Mathematica instance
     * @return instance of mathematica interface
     */
    public static MathematicaInterface getInstance() {
        if ( mathematicaInterface == null ) {
            if ( MathematicaConfig.isMathematicaPresent() )
                mathematicaInterface = new MathematicaInterface();
            else {
                LOG.error("Mathematica is not available!");
            }
        }
        return mathematicaInterface;
    }

    /**
     * This method is for testing purposes only, hence the extra core-package.
     * Theoretically, you should not directly work with the kernel
     * @return the engine of Mathematica
     */
    KernelLink getMathKernel() throws MathLinkException {
        init();
        return mathKernel;
    }

    /**
     * Shuts down the connection to the math kernel.
     *
     * Since it is an lazy init concept now, theoretically you are free to fire it back on by calling any other method
     * (which probably makes the explicit shutdown pointless?)
     */
    public void shutdown() {
        if ( mathKernel != null ) mathKernel.close();
        mathKernel = null;
        MathematicaInterface.mathematicaInterface = null;
    }

    /**
     * The only real math kernel connection method! Apart from direct calls via {@link #getMathKernel()},
     * this is the only method that communicates with the kernel.
     * @param input the command that will be entered into mathematica
     * @return the expression wrapper
     * @throws MathLinkException if the connection to the kernel was lost midway
     * @throws CASUnavailableException if mathematica is not available on the system
     */
    private Expr internalRecoveryEvaluate(String input) throws MathLinkException, CASUnavailableException {
        try {
            init();
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
        return internalRecoveryEvaluate(input);
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
     * This method should not be used since it does not work reliable! On top of it, mathematica does not want to
     * return greek letters in their input form.
     *
     * @param expression formula
     * @return the set of free variables extracted by the undocumented "Reduce`FreeVariables" method.
     * @throws MathLinkException link to mathematica kernel is broken
     */
    public Set<String> getVariables(String expression) throws MathLinkException {
        String extract = Commands.EXTRACT_VARIABLES.build(expression);
        extract = "Map[ToString[#, InputForm, CharacterEncoding -> \"ASCII\"] &, "+extract+"]";
        Expr exs = evaluateToExpression(extract);

        Expr[] argsExp = exs.args();
        Set<String> output = new HashSet<>();

        for (Expr arg : argsExp) {
            String el = arg.toString();
            if ( el.matches("\".*\"") ) output.add(el.substring(1, el.length()-1));
            else output.add(el);
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
