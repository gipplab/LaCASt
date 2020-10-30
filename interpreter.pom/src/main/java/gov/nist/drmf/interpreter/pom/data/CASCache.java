package gov.nist.drmf.interpreter.pom.data;

import java.util.HashMap;

/**
 * Created by AndreG-P on 04.04.2017.
 */
public class CASCache {

    private HashMap<String, CASFunctionMetaInfo> map;

    public CASCache(){
        map = new HashMap<>();
    }

    public void add( String name, int var_num, CASFunctionMetaInfo info ){
        map.put( getID(name, var_num), info );
        if ( var_num > 1 ) {
            map.putIfAbsent( getID(name, -1), info );
        }
    }

    public CASFunctionMetaInfo get(String name, int var_num ){
        if ( var_num > 1 )
            return map.computeIfAbsent( getID(name, var_num), k -> map.get(getID(name, -1)) );
        else return map.get(getID(name, var_num));
    }

    private String getID(String name, int varNum) {
        return name + (varNum <= 1 ? "" : varNum);
    }

    public void clear(){
        map.clear();
        System.gc();
    }
}
