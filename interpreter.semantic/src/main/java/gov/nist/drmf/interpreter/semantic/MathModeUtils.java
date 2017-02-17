package gov.nist.drmf.interpreter.semantic;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Created by jrp4 on 11/29/16.
 */
public class MathModeUtils {
    public static HashMap<String, String> mathMode = new HashMap<String, String>() {{ //initialize map with mathmode starting/ending strings
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

    public static String[] textMode = new String[]{"\\hbox{", "\\mbox{", "\\text{"};

    public static boolean doesEnter(String latex) {
        for (String key : mathMode.keySet()) {
            if (latex.startsWith(key)) {
                return true;
            }
        }
        return false;
    }

    public static boolean doesExit(String latex) {
        for (String str : textMode) {
            if (latex.startsWith(str)) {
                return true;
            }
        }
        return false;
    }

    public static String firstDelim(String latex, boolean enter) {
        String min = "";
        for (String key : (enter ? mathMode.keySet() : new HashSet<>(Arrays.asList(textMode)))) {
            int i = latex.indexOf(key);
            if (min.equals("") || i != -1 && (i <= latex.indexOf(min) || !latex.contains(min))) {
                min = key;
                if (i == 0) {
                    return min;
                }
            }
        }
        return min;
    }

    public static int skipEscaped(String latex) {
        if (latex.startsWith("\\")) {
            for (String key : mathMode.keySet()) {
                if (key.matches("[^a-zA-Z]+") && latex.substring(1).startsWith(key)) {
                    return 2;
                }
            }
        }
        return 0;
    }
}
