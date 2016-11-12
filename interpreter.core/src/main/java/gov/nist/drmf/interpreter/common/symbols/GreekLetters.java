package gov.nist.drmf.interpreter.common.symbols;

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
     * Reads all greek symbols from GreekLettersAndConstants.json.
     */
    private GreekLetters(){
        super(
                GlobalConstants.PATH_GREEK_LETTERS_AND_CONSTANTS_FILE,
                SingleSymbolTranslator.KEY_GREEK_LANGUAGES,
                SingleSymbolTranslator.KEY_GREEK_LETTERS
        );
    }

    // the unique greek symbols obj
    private static GreekLetters obj;

    /**
     * Returns the unique object from this class.
     * @return the unique GreekLetters object.
     */
    public static GreekLetters getGreekLetterInstance(){
        return obj;
    }

    /**
     * Initialize the constants. Loading and extract all
     * information from the JSON file.
     * @see GlobalConstants#PATH_GREEK_LETTERS_AND_CONSTANTS_FILE
     */
    public static void init(){
        if ( obj == null )
            obj = new GreekLetters();
    }
}
