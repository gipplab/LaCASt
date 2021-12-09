package gov.nist.drmf.interpreter.maple.wrapper;

/**
 * @author Andre Greiner-Petter
 */
public class OpenMapleWrapperHelper {
    public static boolean isInstance(Object obj, String clazzPath) {
        try {
            return Class
                    .forName(clazzPath)
                    .isInstance(obj);
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    static Algebraic delegateOpenMapleObject(Object obj) {
        if ( obj instanceof com.maplesoft.openmaple.List )
            return new MapleList((com.maplesoft.openmaple.List) obj);
        else if ( obj instanceof com.maplesoft.openmaple.Numeric )
            return new Numeric((com.maplesoft.openmaple.Numeric) obj);
        else if ( obj instanceof com.maplesoft.openmaple.MString )
            return new MString((com.maplesoft.openmaple.MString) obj);
        else return new Algebraic(obj);
    }
}
