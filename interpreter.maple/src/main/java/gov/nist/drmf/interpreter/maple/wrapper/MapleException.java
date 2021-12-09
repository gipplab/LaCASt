package gov.nist.drmf.interpreter.maple.wrapper;

/**
 * @author Andre Greiner-Petter
 */
public class MapleException extends Exception {
    public MapleException( Throwable e ) {
        super(e);
    }

    public MapleException( String msg ) {
        super(msg);
    }
}
