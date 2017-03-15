package gov.nist.drmf.interpreter.maple.grammar.lexicon;

import gov.nist.drmf.interpreter.common.GlobalPaths;
import mlp.LexiconFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;

/**
 *
 * Created by AndreG-P on 01.03.2017.
 */
public class MapleLexicon {
    private static final String SEPARATOR = "::";

    // HashMaps are faster than TreeMaps here
    // A key looks like <func_name>::<number of arguments>
    private HashMap<String, MapleFunction> function_map;

    MapleLexicon(){
        function_map = new HashMap<>();
    }

    void addFunction( MapleFunction function ){
        function_map.put( function.key, function );
    }

    public MapleFunction getFunction( String maple_name, int num_of_args ){
        return function_map.get( buildKey(maple_name, num_of_args) );
    }

    static String buildKey( String name, int num_of_args ){
        return name + SEPARATOR + num_of_args;
    }

    HashMap<String, MapleFunction> getFunctionMap(){
        return function_map;
    }

    @Override
    public String toString(){
        String nl = System.lineSeparator();
        String output = "LEXICON!" + nl;
        for ( String key : function_map.keySet() ){
            output += MapleFunction.toStorage(function_map.get(key));
            output += nl;
        }
        return output;
    }

    private static MapleLexicon lexicon;

    public static void init() throws IOException {
        lexicon = MapleLexiconFactory.loadLexicon( GlobalPaths.PATH_MAPLE_FUNCTIONS_LEXICON_FILE );
    }

    public static MapleLexicon getLexicon(){
        return lexicon;
    }
}
