package gov.nist.drmf.interpreter.common.symbols;

import gov.nist.drmf.interpreter.common.TranslationInformation;
import gov.nist.drmf.interpreter.common.constants.GlobalPaths;
import gov.nist.drmf.interpreter.common.exceptions.TranslationException;
import gov.nist.drmf.interpreter.common.interfaces.ITranslator;

import java.io.IOException;

/**
 * This class contains all constants in CAS and the corresponding
 * CAS representations if one exists.
 *
 * Created by Andre Greiner-Petter on 02.11.2016.
 */
public class Constants extends GenericTranslationMapper implements ITranslator {
    public static final String
            KEY_LANGUAGES = "Constants Languages",
            KEY_CONSTANTS = "Constants";

    private static GenericTranslationMapper translationMapper;

    private final String FROM, TO;

    /**
     * Reads from GreekLettersAndConstantsFile and store data.
     */
    public Constants(String FROM, String TO){
        this.FROM = FROM;
        this.TO = TO;
    }

    public void init() throws IOException {
        if ( translationMapper == null ) {
            translationMapper = new GenericTranslationMapper();
            translationMapper.init(
                    GlobalPaths.PATH_GREEK_LETTERS_AND_CONSTANTS_FILE,
                    KEY_LANGUAGES,
                    KEY_CONSTANTS
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
}
