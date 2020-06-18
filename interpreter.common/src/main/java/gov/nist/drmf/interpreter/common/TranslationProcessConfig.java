package gov.nist.drmf.interpreter.common;

import gov.nist.drmf.interpreter.common.constants.Keys;
import gov.nist.drmf.interpreter.common.exceptions.InitTranslatorException;
import gov.nist.drmf.interpreter.common.symbols.BasicFunctionsTranslator;
import gov.nist.drmf.interpreter.common.symbols.Constants;
import gov.nist.drmf.interpreter.common.symbols.GreekLetters;
import gov.nist.drmf.interpreter.common.symbols.SymbolTranslator;

import java.io.IOException;

/**
 * @author Andre Greiner-Petter
 */
public class TranslationProcessConfig {
    private final String FROM_LANGUAGE;
    private final String TO_LANGUAGE;

    private final GreekLetters greekLettersTranslator;
    private final Constants constantsTranslator;
    private final BasicFunctionsTranslator basicFunctionsTranslator;
    private final SymbolTranslator symbolTranslator;

    private boolean isInit = false;

    public TranslationProcessConfig(String from_language, String to_language) {
        this.FROM_LANGUAGE      = from_language;
        this.TO_LANGUAGE        = to_language;

        greekLettersTranslator  = new GreekLetters(from_language, to_language);
        basicFunctionsTranslator= new BasicFunctionsTranslator(to_language);
        symbolTranslator        = new SymbolTranslator(from_language, to_language);

        // constant translator is called via DLMF rather than LaTeX
        if ( from_language.equals(Keys.KEY_LATEX) ) from_language = Keys.KEY_DLMF;
        if ( to_language.equals(Keys.KEY_LATEX) ) to_language = Keys.KEY_DLMF;
        constantsTranslator     = new Constants(from_language, to_language);
    }

    public void init() throws InitTranslatorException {
        if ( isInit ) return;

        try {
            greekLettersTranslator.init();
            constantsTranslator.init();
            basicFunctionsTranslator.init();
            symbolTranslator.init();
            isInit = true;
        } catch ( IOException ioe ) {
            throw new InitTranslatorException("Unable to initiate basic translation patterns", ioe);
        }
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
