package gov.nist.drmf.interpreter.common.symbols;

import gov.nist.drmf.interpreter.common.constants.GlobalPaths;

import java.io.IOException;

/**
 * @author Andre Greiner-Petter
 */
public class BasicFunctionsTranslator extends AbstractJSONLoader {
    public static final String
            KEY_LANGUAGES = "Languages",
            KEY_FUNCTIONS = "Functions";

    public static final String
            KEY_NAME = "MLP";

    public static final String POSITION_MARKER = "$";

    private String TO;

    public BasicFunctionsTranslator(
            String TO
    ){
        this.TO = TO;
    }

    public void init() throws IOException {
        super.init(
                GlobalPaths.PATH_BASIC_FUNCTIONS,
                KEY_LANGUAGES,
                KEY_FUNCTIONS
        );
    }

    /**
     * This function function only returns the abstract
     * pattern for the given function name. As an example
     * given "square root" would return sqrt($0) for Maple.
     *
     * Use {@link #translate(String[], String)} to fill the
     * arguments automatically.
     *
     * @param function_name name of the function
     * @return just the abstract pattern of the function
     */
    @Override
    public String translate( String function_name ){
        return super.translate(KEY_NAME, TO, function_name);
    }

    /**
     * Translated a given function name and put all arguments
     * into the right position.
     * @param args the arguments for the translated function
     * @param function_name the name of the function
     * @return translated function with arguments in correct position
     */
    public String translate( String[] args, String function_name ){
        String pattern = translate(function_name);
        if ( pattern == null ) return null;
        for ( int i = 0; i < args.length; i++ ){
            pattern = pattern.replace(POSITION_MARKER + Integer.toString(i), args[i]);
        }
        return pattern;
    }
}
