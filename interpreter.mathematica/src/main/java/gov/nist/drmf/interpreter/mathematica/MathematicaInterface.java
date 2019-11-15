package gov.nist.drmf.interpreter.mathematica;

import com.wolfram.jlink.Expr;
import com.wolfram.jlink.KernelLink;
import com.wolfram.jlink.MathLinkException;
import com.wolfram.jlink.MathLinkFactory;
import gov.nist.drmf.interpreter.mathematica.config.MathematicaConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;

/**
 * @author Andre Greiner-Petter
 */
public class MathematicaInterface {
    private static final Logger LOG = LogManager.getLogger(MathematicaInterface.class.getName());

    private KernelLink mathKernel;

    private static MathematicaInterface mathematicaInterface;

    private MathematicaInterface( KernelLink kernel ) {
        this.mathKernel = kernel;
    }

    private static String[] getDefaultArguments(Path mathPath) {
        return new String[]{
                "-linkmode", "launch",
                "-linkname", mathPath.toString(), "-mathlink"
        };
    }

//    public String evaluate(String input) throws MathLinkException {
//        mathKernel.evaluate(input);
//        mathKernel.waitForAnswer();
//        Expr expression = mathKernel.getExpr();
//    }

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
