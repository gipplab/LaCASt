package gov.nist.drmf.interpreter.common.grammar;

/**
 * @author Andre Greiner-Petter
 */
public enum LimDirections {
    LEFT, RIGHT, NONE;

    public String getKey() {
        if ( this.equals(NONE) ) return "";
        return name().toLowerCase();
    }

    public static LimDirections getDirection(String key) {
        for ( LimDirections l : LimDirections.values() ){
            if ( l.name().toLowerCase().equals(key) ) return l;
        }
        return null;
    }
}
