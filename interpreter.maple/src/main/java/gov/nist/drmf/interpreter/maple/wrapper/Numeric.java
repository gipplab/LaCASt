package gov.nist.drmf.interpreter.maple.wrapper;

/**
 * @author Andre Greiner-Petter
 */
public class Numeric extends Algebraic implements IMapleInstance {
    private static final String CLAZZ_NAME = "Numeric";
    private static final String CLAZZ_PATH = "com.maplesoft.openmaple." + CLAZZ_NAME;

    private final com.maplesoft.openmaple.Numeric numeric;

    Numeric(com.maplesoft.openmaple.Numeric numeric) {
        super(numeric);
        this.numeric = numeric;
    }

    public int intValue() throws MapleException {
        try {
            return numeric.intValue();
        } catch (com.maplesoft.externalcall.MapleException e) {
            throw new MapleException(e);
        }
    }

    public double doubleValue() throws MapleException {
        try {
            return numeric.doubleValue();
        } catch (com.maplesoft.externalcall.MapleException e) {
            throw new MapleException(e);
        }
    }

    @Override
    public String getClassPath() {
        return CLAZZ_PATH;
    }

    public static boolean isInstance(Object obj) {
        if ( obj instanceof Numeric ) return true;
        return OpenMapleWrapperHelper.isInstance(obj, CLAZZ_PATH);
    }

    public static Numeric cast(Object obj) throws ClassCastException {
        if ( obj instanceof Numeric ) return (Numeric) obj;
        return new Numeric((com.maplesoft.openmaple.Numeric) obj);
    }
}
