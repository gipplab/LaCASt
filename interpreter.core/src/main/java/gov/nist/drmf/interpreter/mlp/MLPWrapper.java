package gov.nist.drmf.interpreter.mlp;

import gov.nist.drmf.interpreter.mlp.extensions.MacrosLexicon;
import mlp.ParseException;
import mlp.PomParser;
import mlp.PomTaggedExpression;

import java.io.IOException;

import static gov.nist.drmf.interpreter.examples.MLP.GLOBAL_LEXICON_PATH;

/**
 * @author Andre Greiner-Petter
 */
public class MLPWrapper {

    private PomParser parser;

    private static MLPWrapper wrapper;

    private MLPWrapper(){}

    private void init() throws IOException {
        parser = new PomParser(GLOBAL_LEXICON_PATH);
        MacrosLexicon.init();
        parser.addLexicons( MacrosLexicon.getDLMFMacroLexicon() );
    }

    public static MLPWrapper getWrapperInstance(){
        if( wrapper == null ){
            wrapper = new MLPWrapper();
            try { wrapper.init(); }
            catch ( IOException ioe ){
                ioe.printStackTrace();
                return null;
            }
        }
        return wrapper;
    }

    public PomTaggedExpression parse(String latex) throws ParseException {
        return parser.parse(latex);
    }
}
