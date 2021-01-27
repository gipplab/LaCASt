package gov.nist.drmf.interpreter.common;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * @author Andre Greiner-Petter
 */
public class InformationLogger {
    private final String new_line = System.lineSeparator();

    private final HashMap<String, String> gen_info_map;

    private final HashMap<String, String> macro_info_map;

    public InformationLogger(){
        gen_info_map = new HashMap<>();
        macro_info_map = new HashMap<>();
    }

    public InformationLogger(InformationLogger logger) {
        gen_info_map = new HashMap<>(logger.gen_info_map);
        macro_info_map = new HashMap<>(logger.macro_info_map);
    }

    public void clear() {
        gen_info_map.clear();
        macro_info_map.clear();
    }

    public void addGeneralInfo( String key, String info ){
        if ( gen_info_map.containsKey(key) ) return;
        gen_info_map.put(key, info);
    }

    public void addMacroInfo(String macro_name, String info){
        if ( macro_info_map.containsKey(macro_name) ) return;
        macro_info_map.put(macro_name, info);
    }

    public boolean isEmpty() {
        return gen_info_map.isEmpty() && macro_info_map.isEmpty();
    }

    public boolean containsMacroInformation(String macro) {
        return macro_info_map.containsKey(macro);
    }

    public boolean containsInformation(String element) {
        return macro_info_map.containsKey(element) || gen_info_map.containsKey(element);
    }

    public String getInformation(String element) {
        return macro_info_map.computeIfAbsent(element, key -> gen_info_map.get(element));
    }

    public Map<String, String> getGeneralTranslationInformation() {
        return gen_info_map;
    }

    public Map<String, String> getMacroTranslationInformation() {
        return macro_info_map;
    }

    @Override
    public String toString(){
        StringBuilder info = new StringBuilder("Information about the conversion process:" + new_line);
        for ( String key : macro_info_map.keySet() ){
            info.append(key).append(": ").append(macro_info_map.get(key)).append(new_line).append(new_line);
        }

        LinkedList<String> list = new LinkedList<>(gen_info_map.keySet());
        Collections.sort(list);
        while ( !list.isEmpty() ){
            String key = list.remove(0);
            info.append(key).append(": ").append(gen_info_map.get(key)).append(new_line);
        }
        return info.toString();
    }
}
