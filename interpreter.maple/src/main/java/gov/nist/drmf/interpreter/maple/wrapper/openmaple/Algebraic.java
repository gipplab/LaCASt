package gov.nist.drmf.interpreter.maple.wrapper.openmaple;

import gov.nist.drmf.interpreter.maple.wrapper.MapleException;

/**
 * @author Andre Greiner-Petter
 */
public interface Algebraic {
    boolean isNULL() throws MapleException;

    String toString();
}
