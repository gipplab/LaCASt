package gov.nist.drmf.interpreter.common.symbols;

import gov.nist.drmf.interpreter.common.GlobalPaths;

import java.io.IOException;

/**
 * @author Andre Greiner-Petter
 */
public class SymbolTranslator extends AbstractJSONLoader {
    public static final String
            KEY_LANGUAGES = "Symbol Languages",
            KEY_SYMBOLS = "Symbols";

    public static final String
            KEY_NAME = "MLP";

    public static final String POSITION_MARKER = "$";

    private String FROM;
    private String TO;

    public SymbolTranslator(
            String FROM, String TO
    ){
        this.FROM = FROM;
        this.TO = TO;
    }

    public void init() throws IOException {
        super.init(
                GlobalPaths.PATH_BASIC_FUNCTIONS,
                KEY_LANGUAGES,
                KEY_SYMBOLS
        );
    }

    @Override
    public String translate( String symbol ) {
        return translate( FROM, TO, symbol );
    }

    public String translateFromMLPKey(String symbol){
        return translate( KEY_NAME, TO, symbol );
    }
}
