package gov.nist.drmf.interpreter.cas.mlp;

import java.util.HashMap;

/**
 * Created by AndreG-P on 04.04.2017.
 */
public class CASCache {

    private HashMap<String, CASInfo> map;

    public CASCache(){
        map = new HashMap<>();
    }

    public void add( String name, int var_num, CASInfo info ){
        map.put( name+":"+var_num, info );
    }

    public CASInfo get( String name, int var_num ){
        return map.get( name+":"+var_num );
    }

    public void clear(){
        map.clear();
        System.gc();
    }
}
