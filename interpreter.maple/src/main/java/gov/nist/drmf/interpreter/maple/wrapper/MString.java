package gov.nist.drmf.interpreter.maple.wrapper;

/**
 * @author Andre Greiner-Petter
 */
public class MString extends Algebraic {
    private static final String CLAZZ_NAME = "MString";
    private static final String CLAZZ_PATH = "com.maplesoft.openmaple." + CLAZZ_NAME;

    private final com.maplesoft.openmaple.MString mstring;

    MString(com.maplesoft.openmaple.MString mstring) {
        super(mstring);
        this.mstring = mstring;
    }

    public String stringValue() throws MapleException {
        try {
            return mstring.stringValue();
        } catch (com.maplesoft.externalcall.MapleException e) {
            throw new MapleException(e);
        }
    }

    public static boolean isInstance(Object obj) {
        if ( obj instanceof MString ) return true;
        return OpenMapleWrapperHelper.isInstance(obj, CLAZZ_PATH);
    }

    public static MString cast(Object obj) throws ClassCastException {
        if ( obj instanceof MString ) return (MString) obj;
        return new MString((com.maplesoft.openmaple.MString) obj);
    }
}
