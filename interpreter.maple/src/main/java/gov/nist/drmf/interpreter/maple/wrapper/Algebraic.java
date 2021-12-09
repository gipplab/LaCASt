package gov.nist.drmf.interpreter.maple.wrapper;

/**
 * @author Andre Greiner-Petter
 */
public class Algebraic implements IMapleInstance {
    private static final String CLAZZ_NAME = "Algebraic";
    private static final String CLAZZ_PATH = "com.maplesoft.openmaple." + CLAZZ_NAME;

    private final com.maplesoft.openmaple.Algebraic alg;

    Algebraic(Object alg) {
        this.alg = (com.maplesoft.openmaple.Algebraic) alg;
    }

    public boolean isNULL() throws MapleException {
        try {
            return this.alg.isNULL();
        } catch (Exception e) {
            throw new MapleException(e);
        }
    }

    @Override
    public String toString() {
        return this.alg.toString();
    }

    @Override
    public String getClassPath() {
        return CLAZZ_PATH;
    }
}
