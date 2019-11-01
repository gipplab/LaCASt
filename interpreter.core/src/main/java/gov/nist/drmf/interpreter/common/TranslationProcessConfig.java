package gov.nist.drmf.interpreter.common;

import gov.nist.drmf.interpreter.common.constants.Keys;
import gov.nist.drmf.interpreter.common.symbols.BasicFunctionsTranslator;
import gov.nist.drmf.interpreter.common.symbols.Constants;
import gov.nist.drmf.interpreter.common.symbols.GreekLetters;
import gov.nist.drmf.interpreter.common.symbols.SymbolTranslator;

import java.io.IOException;

/**
 * @author Andre Greiner-Petter
 */
public class TranslationProcessConfig {
    private String FROM_LANGUAGE;
    private String TO_LANGUAGE;

    private GreekLetters greekLettersTranslator;
    private Constants constantsTranslator;
    private BasicFunctionsTranslator basicFunctionsTranslator;
    private SymbolTranslator symbolTranslator;

    private boolean isInit = false;

    public TranslationProcessConfig(String from_language, String to_language) {
        this.FROM_LANGUAGE      = from_language;
        this.TO_LANGUAGE        = to_language;

        greekLettersTranslator  = new GreekLetters(from_language, to_language);
        constantsTranslator     = new Constants(Keys.KEY_DLMF, to_language);
        basicFunctionsTranslator= new BasicFunctionsTranslator(to_language);
        symbolTranslator        = new SymbolTranslator(from_language, to_language);
    }

    public void init() throws IOException {
        if ( isInit ) return;

        greekLettersTranslator.init();
        constantsTranslator.init();
        basicFunctionsTranslator.init();
        symbolTranslator.init();
        isInit = true;
    }

    public GreekLetters getGreekLettersTranslator() {
        return greekLettersTranslator;
    }

    public Constants getConstantsTranslator() {
        return constantsTranslator;
    }

    public BasicFunctionsTranslator getBasicFunctionsTranslator() {
        return basicFunctionsTranslator;
    }

    public SymbolTranslator getSymbolTranslator() {
        return symbolTranslator;
    }

    public String getFROM_LANGUAGE() {
        return FROM_LANGUAGE;
    }

    public String getTO_LANGUAGE() {
        return TO_LANGUAGE;
    }
}
