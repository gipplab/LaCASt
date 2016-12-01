package gov.nist.drmf.interpreter.common.symbols;

import gov.nist.drmf.interpreter.common.GlobalConstants;

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

    public void init(){
        super.init(
                GlobalConstants.PATH_BASIC_FUNCTIONS,
                KEY_LANGUAGES,
                KEY_SYMBOLS
        );
    }

    @Override
    public String translate(String symbol) {
        if ( symbol.startsWith("\\") )
            symbol = symbol.substring(1);
        return translate( FROM, TO, symbol );
    }
}