package gov.nist.drmf.interpreter.semantic;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.ArrayList;

/**
 * Created by jrp4 on 1/3/17.
 */
public class StyFileParser {

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
