package gov.nist.drmf.interpreter.mlp;

import gov.nist.drmf.interpreter.common.constants.GlobalPaths;
import mlp.Lexicon;
import mlp.LexiconFactory;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Created by AndreG-P on 09.03.2017.
 */
public class MacrosLexicon {

    public static final String SIGNAL_ENTRY = "Symbol: ";

    public static final String SIGNAL_FEATURESET = "Feature Set:";

    public static final String SIGNAL_LINE = "-";

    public static final String SIGNAL_INLINE = "\\|\\|";

    private static Lexicon dlmf_macros_lexicon;

    private static boolean executed = false;

    public static synchronized void init() throws IOException {
        init(GlobalPaths.DLMF_MACROS_LEXICON);
    }

    public static synchronized void init(Path lexiconPath) throws IOException {
        if ( executed ) return;

        dlmf_macros_lexicon = LexiconFactory.createLexicon(
                lexiconPath,
                SIGNAL_ENTRY,
                SIGNAL_FEATURESET,
                SIGNAL_LINE,
                SIGNAL_INLINE
        );

        executed = true;
    }

    public static Lexicon getDLMFMacroLexicon(){
        return dlmf_macros_lexicon;
    }
}
