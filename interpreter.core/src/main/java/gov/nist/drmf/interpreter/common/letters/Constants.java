package gov.nist.drmf.interpreter.common.letters;

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
    private static Constants obj = new Constants();

    /**
     * Returns the unique object from this class.
     * @return the unique Constants object.
     */
    public static Constants getConstantsInstance(){
        return obj;
    }
}
