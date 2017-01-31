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
 * Created by jrp4 on 1/3/17.
 */
public class StyFileParser {

    private static final Set<String> definitions = new HashSet<>(Arrays.asList(
            "\\defSpecFun",
            "\\newcommand",
            "\\renewcommand",
            "\\DeclareRobustCommand"
    ));

    private static final Set<Character> startDelims = new HashSet<>(Arrays.asList(
            '{',
            '['
    ));

    private static final Set<Character> endDelims = new HashSet<>(Arrays.asList(
            '}',
            ']'
    ));

    public static void main(String[] args) { //for testing
        System.out.println(parse(Paths.get("/home/cyz1/texer/lib/DLMFfcns.sty")));
        System.out.println(parse(Paths.get("/home/cyz1/texer/lib/DRMFfcns.sty")));
        System.out.println(parse(Paths.get("/home/cyz1/texvcjs/lib/texvc.sty")));
    }

    private static Macro parseMacro(String content) {
        int count = 0;
        int sav = 0;
        ArrayList<String> params = new ArrayList<String>();
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
        return new Macro(params);
    }

    public static ArrayList<Macro> parse(Path styFile) {
        //read from file
        try {
            String content = new String(Files.readAllBytes(styFile));
            int min = Integer.MAX_VALUE;
            for (String s : definitions) { //removes beginning text
                if (content.indexOf(s) >= 0) {
                    min = Math.min(min, content.indexOf(s));
                }
            }
            return parse(content.substring(min));
        } catch (IOException e) {
            System.out.println("File " + styFile.toString() + " not found.");
        }
        return null;
    }

    public static ArrayList<Macro> parse(String content) {
        String[] replacements = content.split("\n");
        ArrayList<Macro> macros = new ArrayList<Macro>();
        for (int i = 0; i < replacements.length; i++) {
            String temp = replacements[i];
            for (int j = i+1; replacements[i].indexOf('{') != -1 && replacements.length > j && definitions.contains(replacements[i].substring(0,replacements[i].indexOf('{'))) &&
                    replacements[j].length() != 0 && replacements[j].charAt(0) != '%' &&
                    !definitions.contains(replacements[j].substring(0,replacements[j].indexOf('{') == -1 ? 0 : replacements[j].indexOf('{'))); j++) {
                temp += replacements[j];
                i++;
            }
            if (temp.indexOf('{') != -1 ? definitions.contains(temp.substring(0, temp.indexOf('{'))) : false) {
                macros.add(parseMacro(temp));
            }
        }
        return macros;
    }

}
