package gov.nist.drmf.interpreter.maple.grammar.lexicon;

import gov.nist.drmf.interpreter.common.constants.Keys;

import java.util.HashMap;

/**
 * Created by AndreG-P on 13.03.2017.
 */
public enum MapleHeader {
    Function( Keys.KEY_MAPLE ),
    DLMF_Pattern( Keys.KEY_DLMF ),
    Link( Keys.KEY_MAPLE + Keys.KEY_LINK_SUFFIX ),
    Branch_Cuts( Keys.KEY_MAPLE + "-" + Keys.FEATURE_BRANCH_CUTS ),
    Constraints( Keys.KEY_MAPLE + "-" + Keys.FEATURE_CONSTRAINTS ),
    Num_Of_Vars( Keys.NUM_OF_VARS ),
    Comment( Keys.KEY_MAPLE + "-" + Keys.KEY_DLMF + Keys.KEY_COMMENT_SUFFIX ),
    Alternatives( Keys.KEY_DLMF + Keys.KEY_ALTERNATIVE_SUFFX );

    private static class Holder{
        static HashMap< String, MapleHeader > map = new HashMap<>();
    }

    String key;

    MapleHeader(String key){
        this.key = key;
        Holder.map.put( key, this );
    }

    public static MapleHeader getHeader( String key ){
        return Holder.map.get( key );
    }
}
