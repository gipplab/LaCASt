package gov.nist.drmf.interpreter.common.symbols;

import gov.nist.drmf.interpreter.common.GlobalPaths;

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
public class GreekLetters extends AbstractJSONLoader {
    public static final String
            KEY_LANGUAGES = "Greek Letter Languages",
            KEY_LETTERS = "Greek Letters";

    private String FROM, TO;

    /**
     * Reads all greek symbols from GreekLettersAndConstants.json.
     */
    public GreekLetters(
            String FROM, String TO){
        this.FROM = FROM;
        this.TO = TO;
    }

    public void init(){
        super.init(
                GlobalPaths.PATH_GREEK_LETTERS_AND_CONSTANTS_FILE,
                KEY_LANGUAGES,
                KEY_LETTERS
        );
    }

    @Override
    public String translate( String symbol ) {
        return super.translate( FROM, TO, symbol );
    }
}
