package gov.nist.drmf.interpreter.mathematica.common;

import java.util.regex.Pattern;

/**
 * @author Andre Greiner-Petter
 */
public enum Commands {
    FULL_FORM("FullForm[XXX-1]", 1),
    EXTRACT_VARIABLES("Reduce`FreeVariables[Normal[XXX-1]]", 1),
    ASSUMING("Assuming[XXX-1, XXX-2]", 2),
    FULL_SIMPLIFY("FullSimplify[XXX-1]", 1),
    FULL_SIMPLIFY_ASSUMPTION("FullSimplify[XXX-1, And[XXX-2]]", 2),
    LENGTH_OF_LIST("Length[XXX-1]", 1),
    NUMERICAL_TEST("numericalAutoTest[Normal[XXX-1], XXX-2, XXX-3]", 3),
    FILTER_TEST_CASES("filterTestCases[XXX-1, XXX-2, XXX-3]", 3),
    FILTER_ASSUMPTIONS("filterAssumptions[XXX-1, XXX-2]", 2),
    FILTER_GLOBAL_ASSUMPTIONS("filterGlobalAssumptions[XXX-1, XXX-2, XXX-3]", 3),
    CREATE_TEST_CASES("createTestCases[XXX-1, XXX-2, XXX-3, XXX-4, XXX-5, XXX-6]", 6),
    COMPLEMENT("Complement[XXX-1, XXX-2]", 2);


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
