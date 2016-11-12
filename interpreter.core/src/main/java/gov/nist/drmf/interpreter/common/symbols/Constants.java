package gov.nist.drmf.interpreter.common.symbols;

import gov.nist.drmf.interpreter.common.GlobalConstants;

/**
 * This class contains all constants in CAS and the corresponding
 * CAS representations if one exists.
 *
 * Created by Andre Greiner-Petter on 02.11.2016.
 */
public class Constants extends SingleSymbolTranslator {
    /**
     * Reads from GreekLettersAndConstantsFile and store data.
     */
    private Constants(){
        super(
            GlobalConstants.PATH_GREEK_LETTERS_AND_CONSTANTS_FILE,
            SingleSymbolTranslator.KEY_CONSTANT_LANGUAGES,
            SingleSymbolTranslator.KEY_CONSTANTS
        );
    }

    // the unique constants class
    private static Constants obj;

    /**
     * Returns the unique object from this class.
     * @return the unique Constants object.
     */
    public static Constants getConstantsInstance(){
        return obj;
    }

    /**
     * Initialize the constants. Loading and extract all
     * information from the JSON file.
     * @see GlobalConstants#PATH_GREEK_LETTERS_AND_CONSTANTS_FILE
     */
    public static void init(){
        if ( obj == null )
            obj = new Constants();
    }
}
