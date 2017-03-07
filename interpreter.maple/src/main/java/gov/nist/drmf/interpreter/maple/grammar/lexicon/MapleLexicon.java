package gov.nist.drmf.interpreter.maple.grammar.lexicon;

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

    public MapleLexicon(){
        function_map = new HashMap<>();
    }

    public void addFunction( MapleFunction function ){
        function_map.put( function.key, function );
    }

    public MapleFunction getFunction( String maple_name, int num_of_args ){
        return function_map.get( buildKey(maple_name, num_of_args) );
    }

    static String buildKey( String name, int num_of_args ){
        return name + SEPARATOR + num_of_args;
    }
}
