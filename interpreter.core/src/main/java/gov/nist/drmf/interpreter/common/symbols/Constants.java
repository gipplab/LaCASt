package gov.nist.drmf.interpreter.common.symbols;

import gov.nist.drmf.interpreter.common.GlobalPaths;

/**
 * This class contains all constants in CAS and the corresponding
 * CAS representations if one exists.
 *
 * Created by Andre Greiner-Petter on 02.11.2016.
 */
public class Constants extends AbstractJSONLoader {
    public static final String
            KEY_LANGUAGES = "Constants Languages",
            KEY_CONSTANTS = "Constants";

    private String FROM, TO;

    /**
     * Reads from GreekLettersAndConstantsFile and store data.
     */
    public Constants(String FROM, String TO){
        this.FROM = FROM;
        this.TO = TO;
    }

    /**
     *
     */
    public void init(){
        super.init(
                GlobalPaths.PATH_GREEK_LETTERS_AND_CONSTANTS_FILE,
                KEY_LANGUAGES,
                KEY_CONSTANTS
        );
    }

    @Override
    public String translate( String symbol ) {
        return super.translate( FROM, TO, symbol );
    }
}
