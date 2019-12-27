package gov.nist.drmf.interpreter.mathematica;

import com.wolfram.jlink.Expr;
import com.wolfram.jlink.KernelLink;
import com.wolfram.jlink.MathLinkException;
import com.wolfram.jlink.MathLinkFactory;
import gov.nist.drmf.interpreter.common.constants.GlobalPaths;
import gov.nist.drmf.interpreter.mathematica.common.Commands;
import gov.nist.drmf.interpreter.mathematica.config.MathematicaConfig;
import gov.nist.drmf.interpreter.mathematica.evaluate.SymbolicEquivalenceChecker;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Andre Greiner-Petter
 */
public class MathematicaInterface {
    private static final Logger LOG = LogManager.getLogger(MathematicaInterface.class.getName());

    private KernelLink mathKernel;

    private static MathematicaInterface mathematicaInterface;

    private SymbolicEquivalenceChecker evalChecker;

    private MathematicaInterface( KernelLink kernel ) {
        this.mathKernel = kernel;
        this.evalChecker = new SymbolicEquivalenceChecker(kernel);
    }

    KernelLink getMathKernel() {
        return mathKernel;
    }

    private static String[] getDefaultArguments(Path mathPath) {
        return new String[]{
                "-linkmode", "launch",
                "-linkname", mathPath.toString(), "-mathlink"
        };
    }

    public String evaluate(String input) throws MathLinkException {
        Expr expression = evaluateToExpression(input);
        return expression.toString();
    }

    public Expr evaluateToExpression(String input) throws MathLinkException {
        LOG.debug("Evaluate: " + input);
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
//        Expr exs = evaluateToExpression(extract);

        Expr[] argsExp = exs.args();
        Set<String> output = new HashSet<>();

        for ( int i = 0; i < argsExp.length; i++ ) {
            Expr arg = argsExp[i];
//            String element = Commands.FULL_FORM.build(arg.toString());
//            output.add(mathKernel.evaluateToOutputForm(element, 0));
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

    /**
     * Initiates mathematica interface. This function skips if there is already an instance
     * available.
     * @throws MathLinkException if an interface cannot instantiated
     */
    private static void init() throws MathLinkException {
        if ( mathematicaInterface != null ) return; // already instantiated

        LOG.debug("Instantiating mathematica interface");
        Path mathPath = MathematicaConfig.loadMathematicaPath();
        String[] args = getDefaultArguments(mathPath);

        KernelLink mathKernel = MathLinkFactory.createKernelLink(args);
        // we don't care about the answer for instantiation, so skip it
        mathKernel.discardAnswer();

        // set encoding to avoid UTF-8 chars of greek letters
        MathematicaConfig.setCharacterEncoding(mathKernel);
        mathematicaInterface = new MathematicaInterface(mathKernel);
        LOG.info("Successfully instantiated mathematica interface");
    }

    /**
     * Get the Mathematica instance
     * @return instance of mathematica interface
     */
    public static MathematicaInterface getInstance() {
        if ( mathematicaInterface != null ) return mathematicaInterface;
        else {
            try {
                init();
                return mathematicaInterface;
            } catch (MathLinkException e) {
                LOG.error("Cannot instantiate Mathematica interface", e);
                return null;
            }
        }
    }

}
