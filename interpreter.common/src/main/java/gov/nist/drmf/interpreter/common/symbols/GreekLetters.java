package gov.nist.drmf.interpreter.common.symbols;

import gov.nist.drmf.interpreter.common.TranslationInformation;
import gov.nist.drmf.interpreter.common.constants.GlobalPaths;
import gov.nist.drmf.interpreter.common.exceptions.TranslationException;
import gov.nist.drmf.interpreter.common.interfaces.ITranslator;

import java.io.IOException;

/**
 * This class translates a given greek letter from one language
 * to another. The default languages are LaTeX, Maple and Mathematica.
 *
 * You can add support for more languages (CAS) when you extend the
 * GreekLetters.json file in the libs directory. The key values can
 * be found in {@link GlobalPaths}.
 *
 * @author Andre Greiner-Petter
 */
public class GreekLetters extends GenericTranslationMapper implements ITranslator {
    public static final String
            KEY_LANGUAGES = "Greek Letter Languages",
            KEY_LETTERS = "Greek Letters";

    private static GenericTranslationMapper translationMapper;

    private final String FROM, TO;

    /**
     * Reads all greek symbols from GreekLettersAndConstants.json.
     */
    public GreekLetters(
            String FROM, String TO){
        this.FROM = FROM;
        this.TO = TO;
    }

    public void init() throws IOException {
        if ( translationMapper == null ) {
            translationMapper = new GenericTranslationMapper();
            translationMapper.init(
                    GlobalPaths.PATH_GREEK_LETTERS_AND_CONSTANTS_FILE,
                    KEY_LANGUAGES,
                    KEY_LETTERS
            );
        }
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
