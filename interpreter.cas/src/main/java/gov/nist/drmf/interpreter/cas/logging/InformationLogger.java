package gov.nist.drmf.interpreter.cas.logging;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;

/**
 * @author Andre Greiner-Petter
 */
public class InformationLogger {
    private final String new_line = System.lineSeparator();

    private HashMap<String, String> gen_info_map;

    private HashMap<String, String> macro_info_map;

    public InformationLogger(){
        gen_info_map = new HashMap<>();
        macro_info_map = new HashMap<>();
    }

    public void addGeneralInfo( String key, String info ){
        if ( gen_info_map.containsKey(key) ) return;
        gen_info_map.put(key, info);
    }

    public void addMacroInfo(String macro_name, String info){
        if ( macro_info_map.containsKey(macro_name) ) return;
        macro_info_map.put(macro_name, info);
    }

    public String toString(){
        String info = "Information about the conversion process:" + new_line;
        for ( String key : macro_info_map.keySet() ){
            info += key + ": " + macro_info_map.get(key) + new_line + new_line;
        }

        LinkedList<String> list = new LinkedList<>(gen_info_map.keySet());
        Collections.sort(list);
        while ( !list.isEmpty() ){
            String key = list.remove(0);
            info += key + ": " + gen_info_map.get(key) + new_line;
        }
        return info;
    }
}
