package gov.nist.drmf.interpreter.common.symbols;

import gov.nist.drmf.interpreter.common.TranslationInformation;
import gov.nist.drmf.interpreter.common.constants.GlobalPaths;
import gov.nist.drmf.interpreter.common.exceptions.TranslationException;
import gov.nist.drmf.interpreter.common.interfaces.ITranslator;

import java.io.IOException;

/**
 * @author Andre Greiner-Petter
 */
public class SymbolTranslator extends GenericTranslationMapper implements ITranslator {
    public static final String
            KEY_LANGUAGES = "Symbol Languages",
            KEY_SYMBOLS = "Symbols";

    public static final String
            KEY_NAME = "MLP";

    private static GenericTranslationMapper translationMapper;

    private final String FROM;
    private final String TO;

    public SymbolTranslator(
            String FROM, String TO
    ){
        this.FROM = FROM;
        this.TO = TO;
    }

    public static GenericTranslationMapper getGenericMapper() throws IOException {
        if ( translationMapper == null ) init();
        return translationMapper;
    }

    public static void init() throws IOException {
        if ( translationMapper == null ) {
            translationMapper = new GenericTranslationMapper();
            translationMapper.init(
                    GlobalPaths.PATH_BASIC_FUNCTIONS,
                    KEY_LANGUAGES,
                    KEY_SYMBOLS
            );
        }
    }

    @Override
    public String getSourceLanguage() {
        return FROM;
    }

    @Override
    public String getTargetLanguage() {
        return TO;
    }

    @Override
    public String translate( String symbol ) {
        return translationMapper.translate( FROM, TO, symbol );
    }

    @Override
    public String translate(String from_language, String to_language, String symbol){
        return translationMapper.translate(from_language, to_language, symbol);
    }

    @Override
    public TranslationInformation translateToObject(String expression) throws TranslationException {
        return new TranslationInformation(expression, translate(expression));
    }

    public String translateFromMLPKey(String symbol){
        return translationMapper.translate( KEY_NAME, TO, symbol );
    }
}
