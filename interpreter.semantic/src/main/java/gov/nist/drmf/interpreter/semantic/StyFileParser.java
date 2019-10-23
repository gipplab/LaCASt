package gov.nist.drmf.interpreter.semantic;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Java class for parsing a .sty file
 */
public class StyFileParser {

    /**
     * String constants for .sty files
     */
    private static final Set<String> definitions = new HashSet<>(Arrays.asList(
            "\\defSpecFun",
            "\\newcommand",
            "\\renewcommand",
            "\\DeclareRobustCommand"
    ));

    /**
     * Start delimiters in .sty files
     */
    private static final Set<Character> startDelims = new HashSet<>(Arrays.asList(
            '{',
            '['
    ));

    /**
     * End delimiters in .sty files
     */
    private static final Set<Character> endDelims = new HashSet<>(Arrays.asList(
            '}',
            ']'
    ));

    /**
     * Generates a Macro given the string for a single macro
     * @param content
     * @return
     */
    private static Macro parseMacro(String content) {
        int count = 0;
        int sav = 0;
        ArrayList<String> params = new ArrayList<>();
        params.add(content.substring(1,content.indexOf('{')));
        for (int i = 0; i < content.length(); i++) {
            if (startDelims.contains(content.charAt(i))) {
                if (count == 0) {
                    sav = i + 1;
                }
                count++;
            } else if (endDelims.contains(content.charAt(i))) {
                count--;
                if (count == 0) {
                    params.add(content.substring(sav, i));
                }
            }
        }
        for (int i = 0; i < params.size(); i++) { //remove blanks
            if (params.get(i).equals("")) {
                params.remove(i);
                i--;
            }
        }
        return new Macro(params);
    }

    /**
     * Opens the .sty file and returns an ArrayList of Macros
     * @param styFile
     * @return
     */
    public static ArrayList<Macro> parse(Path styFile) {
        //read from file
        try {
            String content = new String(Files.readAllBytes(styFile));
            int min = Integer.MAX_VALUE;
            for (String s : definitions) { //removes beginning text
                if (content.contains(s)) {
                    min = Math.min(min, content.indexOf(s));
                }
            }
            return parse(content.substring(min));
        } catch (IOException e) {
            System.out.println("File " + styFile.toString() + " not found.");
        }
        return null;
    }

    /**
     * Creates an ArrayList of Macro objects given a string of macros from a .sty file
     * @param content
     * @return
     */
    public static ArrayList<Macro> parse(String content) {
        String[] replacements = content.split("\n");
        ArrayList<Macro> macros = new ArrayList<>();
        for (int i = 0; i < replacements.length; i++) {
            String temp = replacements[i];
            for (int j = i+1; replacements[i].indexOf('{') != -1 && replacements.length > j && definitions.contains(replacements[i].substring(0,replacements[i].indexOf('{'))) &&
                    replacements[j].length() != 0 && replacements[j].charAt(0) != '%' &&
                    !definitions.contains(replacements[j].substring(0,replacements[j].indexOf('{') == -1 ? 0 : replacements[j].indexOf('{'))); j++) {
                temp += replacements[j];
                i++;
            }
            if (temp.indexOf('{') != -1 && definitions.contains(temp.substring(0, temp.indexOf('{')))) {
                macros.add(parseMacro(temp));
            }
        }
        return macros;
    }
}
