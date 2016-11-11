package gov.nist.drmf.interpreter.common.letters;

import gov.nist.drmf.interpreter.common.GlobalConstants;

/**
 * This class translates a given greek letter from one language
 * to another. The default languages are LaTeX, Maple and Mathematica.
 *
 * You can add support for more languages (CAS) when you extend the
 * GreekLetters.json file in the libs directory. The key values can
 * be found in {@link gov.nist.drmf.interpreter.common.GlobalConstants}.
 *
 * @author Andre Greiner-Petter
 */
public class GreekLetters extends SingleSymbolTranslator {
    /**
     * Reads all greek letters from GreekLettersAndConstants.json.
     */
    private GreekLetters(){
        super(
                GlobalConstants.PATH_GREEK_LETTERS_AND_CONSTANTS_FILE,
                SingleSymbolTranslator.KEY_GREEK_LANGUAGES,
                SingleSymbolTranslator.KEY_GREEK_LETTERS
        );
    }

    // the unique greek letters obj
    private static GreekLetters obj = new GreekLetters();

    /**
     * Returns the unique object from this class.
     * @return the unique GreekLetters object.
     */
    public static GreekLetters getGreekLetterInstance(){
        return obj;
    }
}
