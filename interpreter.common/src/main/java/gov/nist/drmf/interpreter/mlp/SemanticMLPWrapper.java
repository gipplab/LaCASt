package gov.nist.drmf.interpreter.mlp;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

/**
 * A semantic version of the {@link MLPWrapper}. This class
 * automatically loads the lexicon of the semantic DLMF macros.
 *
 * @author Andre Greiner-Petter
 */
public class SemanticMLPWrapper extends MLPWrapper {
    private static final Logger LOG = LogManager.getLogger(SemanticMLPWrapper.class.getName());

    /**
     * A standard instance to ensure a better performance
     */
    private static SemanticMLPWrapper standardInstance;

    public SemanticMLPWrapper() throws IOException {
        super();
        MacrosLexicon.init();
        addLexicon( MacrosLexicon.getDLMFMacroLexicon() );
    }

    /**
     * Provide access to the standard instance of the PoM-Tagger. It increases the performances
     * if you keep the number of MLP instances low.
     * @return the standard instance of the this class
     */
    public static SemanticMLPWrapper getStandardInstance() {
        if ( standardInstance == null ) {
            try {
                standardInstance = new SemanticMLPWrapper();
            } catch (IOException ioe) {
                LOG.fatal("Unable to create semantic PoM-tagger instance.");
            }
        }
        return standardInstance;
    }
}
