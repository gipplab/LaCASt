package gov.nist.drmf.interpreter.common.latex;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Andre Greiner-Petter
 */
public class CaseSplitter {
    private static final Pattern PM_PATTERN = Pattern.compile("\\\\(pm|mp)(?![a-zA-Z])");

    private CaseSplitter(){}

    public static List<String> splitPMSymbols(String latex) {
        if ( latex == null ) return null;
        StringBuilder firstCase = new StringBuilder();
        StringBuilder secondCase = new StringBuilder();
        StringBuilder refSb = new StringBuilder();
        Matcher m = PM_PATTERN.matcher(latex);
        while ( m.find() ) {
            m.appendReplacement(refSb, "");
            if ( m.group(1).matches("pm") ) {
                firstCase.append(refSb.toString()).append("+");
                secondCase.append(refSb.toString()).append("-");
            } else {
                firstCase.append(refSb.toString()).append("-");
                secondCase.append(refSb.toString()).append("+");
            }
            refSb = new StringBuilder();
        }
        m.appendTail(refSb);
        firstCase.append(refSb.toString());
        secondCase.append(refSb.toString());

        String first = firstCase.toString();
        String second = secondCase.toString();
        if ( first.equals(second) ) return List.of(first);
        else return List.of(first, second);
    }
}
