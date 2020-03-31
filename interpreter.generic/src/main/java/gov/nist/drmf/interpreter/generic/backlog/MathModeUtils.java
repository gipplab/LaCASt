package gov.nist.drmf.interpreter.generic.backlog;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * Java class containing utility methods for parsing LaTeX
 */
public class MathModeUtils {

    /**
     * Initializes map with math mode starting and ending strings
     */
    public static HashMap<String, String> mathMode = new HashMap<String, String>() {{
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

    /**
     * Initializes text mode starting strings
     */
    public static String[] textMode = new String[]{"\\hbox{", "\\mbox{", "\\text{"};

    /**
     * Checks if LaTeX string enters math mode
     * @param latex
     * @return
     */
    public static boolean doesEnter(String latex) {
        for (String key : mathMode.keySet()) {
            if (latex.startsWith(key)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if LaTeX string exits math mode
     * @param latex
     * @return
     */
    public static boolean doesExit(String latex) {
        for (String str : textMode) {
            if (latex.startsWith(str)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets the first delimiter for math mode if enter is true and text mode if enter is false
     * @param latex
     * @param enter
     * @return
     */
    public static String firstDelim(String latex, boolean enter) {
        String min = "";
        Set<String> set = enter ? mathMode.keySet() : new HashSet<>(Arrays.asList(textMode));
        for (String key : set) {
            int i = latex.indexOf(key);
            if (check(min, i, latex)) {
                min = key;
                if (i == 0) {
                    return min;
                }
            }
        }
        return min;
    }

    private static boolean check(String min, int i, String latex) {
        return min.equals("") || i != -1 && (i <= latex.indexOf(min) || !latex.contains(min));
    }

    /**
     * Returns number of indices at the beginning of the LaTeX string that are escaped
     * @param latex
     * @return
     */
    public static int skipEscaped(String latex) {
        if (!latex.startsWith("\\")) return 0;

        for (String key : mathMode.keySet()) {
            if (key.matches("[^a-zA-Z]+") && latex.substring(1).startsWith(key)) {
                return 2;
            }
        }

        return 0;
    }
}
