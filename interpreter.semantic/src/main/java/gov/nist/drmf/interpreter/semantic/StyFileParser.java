package gov.nist.drmf.interpreter.semantic;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Files;
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

    private static ArrayList<Macro> parseMacro(String content) {
        int count = 0;
        int sav = 0;
        ArrayList<Macro> macros = new ArrayList<Macro>();
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
            if (count == 0 && definitions.contains(content.substring(i,content.indexOf('{',i)))) {
                macros.add(new Macro(params));
                params.clear();
            }
        }
        return macros;
    }

    public static ArrayList<Macro> parse(Path styFile) {
        //read from file
        try {
            String content = new String(Files.readAllBytes(styFile));
            return parse(content);
        } catch (IOException e) {
            System.out.println("File " + styFile.toString() + " not found.");
        }
        return null;
    }

    public static ArrayList<Macro> parse(String content) {
        String[] replacements = content.split("\n");
        ArrayList<Macro> macros = new ArrayList<Macro>();
        for (int i = 0; i < replacements.length; i++) {
            //parse function definitions
        }
        return macros;
    }

}
