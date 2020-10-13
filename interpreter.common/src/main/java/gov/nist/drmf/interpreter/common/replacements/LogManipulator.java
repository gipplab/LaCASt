package gov.nist.drmf.interpreter.common.replacements;

/**
 * @author Andre Greiner-Petter
 */
public final class LogManipulator {
    private LogManipulator(){}

    public static String shortenOutput(String msg, int maxCases) {
        int pos = msg.length();
        int bracketCounter = 1;
        int caseCounter = 0;

        for ( int i = 2; i < msg.length() && caseCounter < maxCases; i++ ) {
            char c = msg.charAt(i);
            if ( '{' == c || '[' == c ) bracketCounter++;
            else if ( '}' == c || ']' == c ) bracketCounter--;
            else continue;

            if ( bracketCounter == 0 ) {
                caseCounter++;
                pos = i+2;
            }
        }

        if ( pos < msg.length() ) {
            return msg.substring(0, pos) + " ...";
        } else return msg;
    }
}
