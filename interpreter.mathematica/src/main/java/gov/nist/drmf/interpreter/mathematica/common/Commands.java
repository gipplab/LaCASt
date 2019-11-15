package gov.nist.drmf.interpreter.mathematica.common;

import java.util.regex.Pattern;

/**
 * @author Andre Greiner-Petter
 */
public enum Commands {
    FULL_FORM("FullForm[XXX-1]", 1),
    EXTRACT_VARIABLES("Reduce`FreeVariables[XXX-1]", 1),
    ASSUMING("Assuming[XXX-1, XXX-2]", 2),
    FULL_SIMPLIFY("FullSimplify[XXX-1]", 1);


    private static final String PLACE_HOLDER = "XXX";
    private String cmd;
    private int numOfArgs;

    Commands(String cmd, int numOfArgs) {
        this.cmd = cmd;
        this.numOfArgs = numOfArgs;
    }

    public String build( String... expr ) {
        if ( expr.length != numOfArgs )
            throw new IllegalArgumentException("Invalid number of arguments");

        String b = cmd;
        for ( int i = 1; i <= expr.length; i++ ) {
            String arg = expr[i-1];
            arg = arg.replace("\\", "\\\\");
            b = b.replaceAll(PLACE_HOLDER+"\\-"+i, arg);
        }

        return b;
    }
}
