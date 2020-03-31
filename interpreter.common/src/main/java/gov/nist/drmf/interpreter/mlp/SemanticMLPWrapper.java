package gov.nist.drmf.interpreter.mlp;

import gov.nist.drmf.interpreter.mlp.extensions.MacrosLexicon;

import java.io.IOException;

/**
 * A semantic version of the {@link MLPWrapper}. This class
 * automatically loads the lexicon of the semantic DLMF macros.
 *
 * @author Andre Greiner-Petter
 */
public class SemanticMLPWrapper extends MLPWrapper {
    public SemanticMLPWrapper() throws IOException {
        super();
        MacrosLexicon.init();
        addLexicon( MacrosLexicon.getDLMFMacroLexicon() );
    }
}
