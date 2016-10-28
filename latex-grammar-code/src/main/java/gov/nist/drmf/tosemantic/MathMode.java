package gov.nist.drmf.tosemantic;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by cyz1 on 10/25/16.
 */
public class MathMode {
    private static HashMap<String, String> mathMode = new HashMap<String, String>() {{ //initialize map with mathmode starting/ending strings
        put("\\[","\\]");
        put("\\(","\\)");
        put("\\begin{equation}", "\\end{equation}");
        put("\\begin{equation*}", "\\end{equation*}");
        put("\\begin{align}", "\\end{align}");
        put("\\begin{align*}", "\\end{align*}");
        put("\\begin{multiline}", "\\end{multiline}");
        put("\\begin{multiline*}", "\\end{multiline*}");
        put("$", "$");
        put("$$", "$$");
    }};

    private static boolean doesEnter(String latex) {

    }

    private static boolean doesExit(String latex) {

    }

    private static String firstDelim(String latex, boolean enter) {

    }

    private static boolean startsWith(String string, String substring) {
        return string.lastIndexOf(substring, 0) == 0;
    }

    private static int skipEscaped(String latex) {

    }

    private static int parseMath(String latex, int start, ArrayList<int[]> ranges) {

    }

    private static int parseNonMath(String latex, int start, ArrayList<int[]> ranges) {
        String delim = firstDelim(latex, false);
        if (!startsWith(latex, delim)) {
            delim = "";
        }
        int level = 0;
        int i = delim.length();
        while (i < latex.length()) {
            i += skipEscaped(latex.substring(i));
            if (doesEnter(latex.substring(i))) {
                i += parseMath(latex.substring(i), start + i, ranges);
            } else if (latex.charAt(i) == '{') {
                level++;
            } else if (latex.charAt(i) == '}') {
                if (level == 0 && !delim.equals("")) {
                    i++;
                    return i;
                } else {
                    level--;
                }
            }
            i++;
        }
        return i;
    }

    public static ArrayList<String> findMathSections(String latex) {
        ArrayList<String> sections = new ArrayList<String>();
        ArrayList<int[]> ranges = new ArrayList<int[]>();
        parseNonMath(latex, 0, ranges);
        for (int i = 0; i < ranges.size(); i++) {
            sections.add(latex.substring(ranges.get(i)[0], ranges.get(i)[1]));
        }
        return sections;
    }
}
