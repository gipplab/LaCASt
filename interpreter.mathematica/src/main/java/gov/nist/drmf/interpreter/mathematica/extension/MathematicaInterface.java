package gov.nist.drmf.interpreter.mathematica.extension;

import com.wolfram.jlink.Expr;
import com.wolfram.jlink.KernelLink;
import com.wolfram.jlink.MathLinkException;
import com.wolfram.jlink.MathLinkFactory;
import gov.nist.drmf.interpreter.common.cas.IComputerAlgebraSystemEngine;
import gov.nist.drmf.interpreter.common.exceptions.ComputerAlgebraSystemEngineException;
import gov.nist.drmf.interpreter.mathematica.common.Commands;
import gov.nist.drmf.interpreter.mathematica.config.MathematicaConfig;
import gov.nist.drmf.interpreter.mathematica.evaluate.SymbolicEquivalenceChecker;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
                LOG.error("Cannot instantiate Mathematica interface", e);
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
        LOG.warn("Ignore force GC for Mathematica");
    }

    @Override
    public String buildList(List<String> list) {
        String ls = list.toString();
        return ls.substring(1, ls.length()-1);
    }

    public void checkIfEvaluationIsInRange(String command, int lowerLimit, int upperLimit) throws ComputerAlgebraSystemEngineException {
        try {
            String res = mathematicaInterface.evaluate(command);
            LOG.debug("Generated test cases: " + res);
            int nT = Integer.parseInt(res);
            if ( nT >= upperLimit ) throw new IllegalArgumentException("Too many test combinations.");
            else if ( nT <= lowerLimit ) throw new IllegalArgumentException("Not enough test combinations.");
            // res should be an integer, testing how many test commands there are!
        } catch (MathLinkException | NumberFormatException e) {
            throw new ComputerAlgebraSystemEngineException(e);
        }
    }
}
