package gov.nist.drmf.interpreter.common.symbols;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import gov.nist.drmf.interpreter.common.GlobalConstants;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * An abstract class to handle single symbols like greek symbols and
 * constants. It loads a JSON file with information about constants
 * and greek symbols and stores them in an efficient way.
 *
 * It provides to translate a given symbol from one representation
 * to another.
 * @see #translate(String, String, String)
 *
 * @author Andre Greiner-Petter
 */
public abstract class SingleSymbolTranslator {
    /**
     * Storage System:
     *  directory[0] <- all symbols in language 0
     *  directory[1] <- all symbols in language 1 in same
     *                  order as in directory[0]...
     *
     *  word_map[0]  <- maps a letter to the corresponding position
     *                  in the directory.
     *                  For instance if this world_map[0] contains
     *                  "Alpha" -> 5 that means you can find this letter in
     *                  directory[0][5] and its translations in
     *                  directory[1][5], directory[2][5] and so on.
     *
     *  lang_map     <- maps the language name to the index. For instance
     *                  "LaTeX" -> 1 means directory[1] contains all LaTeX
     *                  symbols.
     */

    // the language index map
    private HashMap<String, Integer> lang_map;

    // the word indices map
    private HashMap<String, Integer>[] word_map;

    // the dictionary contains all symbols
    private String[][] dictionary;

    // The key strings in the GreekLetters.json
    protected static String
            KEY_GREEK_LANGUAGES     = "Greek Letter Languages",
            KEY_GREEK_LETTERS       = "Greek Letters",
            KEY_CONSTANT_LANGUAGES  = "Constants Languages",
            KEY_CONSTANTS           = "Constants";

    /**
     * Constructor constructs
     */
    protected SingleSymbolTranslator(
            Path letters_json_path,
            String key_languages,
            String key_entries
    ){
        init(letters_json_path, key_languages, key_entries);
    }

    /**
     * Initialize the class by loading all greek symbols from a given json file.
     * @param letters_json_path GreekLetters.json
     */
    private void init(
            Path letters_json_path,
            String key_languages,
            String key_entries
    ){
        try {
            List<String> lines = Files.readAllLines(letters_json_path);
            String file = "";
            while ( !lines.isEmpty() ) file += lines.remove(0);
            JsonParser parser =  new JsonParser();
            JsonElement tree = parser.parse(file);
            JsonObject mainObj = tree.getAsJsonObject();
            JsonArray langs = mainObj.get(key_languages).getAsJsonArray();

            lang_map = new HashMap<>();
            //noinspection unchecked
            word_map = new HashMap[langs.size()];
            for ( int i = 0; i < langs.size(); i++ ){
                String lang = langs.get(i).getAsString();
                lang_map.put(lang, i);
                word_map[i] = new HashMap<>();
            }

            JsonObject lettersObj = mainObj.get(key_entries).getAsJsonObject();
            Set<Map.Entry<String, JsonElement>> letters = lettersObj.entrySet();
            dictionary = new String[langs.size()][letters.size()];

            int idx = 0;
            for ( Map.Entry<String, JsonElement> letter : letters ){
                JsonObject letterObj = letter.getValue().getAsJsonObject();
                Set<Map.Entry<String, JsonElement>> versions = letterObj.entrySet();
                for ( Map.Entry<String, JsonElement> l : versions ){
                    //System.out.println(l);
                    int lang_idx = lang_map.get(l.getKey());
                    String word = l.getValue().getAsString();
                    dictionary[lang_idx][idx] = word;
                    word_map[lang_idx].put(word, idx);
                }
                idx++;
            }
        } catch ( IOException ioe ){
            System.err.println( "Unable to load greek symbols and constants from json directory in: " +
                    letters_json_path.toString());
        }
    }

    /**
     * Translates a given symbol from a given language to another given language.
     * The given symbol must be in language {@param from_language}. The string
     * languages must be the same as in GreekLettersAndConstants.json. Take a look at
     * {@link GlobalConstants#KEY_LATEX} for example.
     * @param from_language the given letter must be in this language
     * @param to_language another language
     * @param symbol letter to translate
     * @return the given letter in to_language
     */
    public String translate(String from_language, String to_language, String symbol){
        int lang1_idx = lang_map.get(from_language);
        int lang2_idx = lang_map.get(to_language);
        int word_idx = word_map[lang1_idx].get(symbol);
        return dictionary[lang2_idx][word_idx];
    }
}
