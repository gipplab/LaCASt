package gov.nist.drmf.interpreter.semantic;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Created by cyz1 on 10/25/16.
 */
public class MathModeFinder {
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

    private static String[] textMode = new String[]{"\\hbox{", "\\mbox{", "\\text{"};

    private static boolean doesEnter(String latex) {
        for (String key : mathMode.keySet()) {
            if (latex.startsWith(key)) {
                return true;
            }
        }
        return false;
    }

    private static boolean doesExit(String latex) {
        for (String str : textMode) {
            if (latex.startsWith(str)) {
                return true;
            }
        }
        return false;
    }

    private String firstDelim(String latex, boolean enter) {
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


    private int skipEscaped(String latex) {
        if (latex.startsWith("\\")) {
            for (String key : mathMode.keySet()) {
                if (key.matches("[^a-zA-Z]+") && latex.substring(1).startsWith(key)) {
                    return 2;
                }
            }
        }
        return 0;
    }

    private int parseMath(String latex, int start, ArrayList<int[]> ranges) {
        String delim = firstDelim(latex, true);
        int i = delim.length();
        int begin = start + i;
        while (i < latex.length()) {
            i += skipEscaped(latex.substring(i));
            if (doesExit(latex.substring(i))) {
                if (begin != start + i) {
                    ranges.add(new int[]{begin, start + i});
                }
                i += parseNonMath(latex.substring(i),start + i, ranges);
                begin = start + i;
            }
            if (latex.substring(i).startsWith(mathMode.get(delim))) {
                if (begin != start + i) {
                    ranges.add(new int[]{begin, start + i});
                }
                return i + mathMode.get(delim).length()-1;
            }
            i++;
        }
        return i;
    }

    private int parseNonMath(String latex, int start, ArrayList<int[]> ranges) {
        String delim = firstDelim(latex, false);
        if (!latex.startsWith(delim)) {
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

    private ArrayList<String> findMathSections(String latex) {
        ArrayList<String> sections = new ArrayList<>();
        ArrayList<int[]> ranges = new ArrayList<>();
        parseNonMath(latex, 0, ranges);
        for (int[] nums : ranges) {
            sections.add(latex.substring(nums[0], nums[1]));
        }
        return sections;
    }

    public ArrayList<String> findMathSections(Path path) {
        try {
            String content = new String(Files.readAllBytes(path));
            return findMathSections(content);
        } catch (IOException e) {
            System.out.println("File " + path.getFileName() + " not found.");
        }
        return null;
    }

    public static void main(String[] args) {

    }
}
