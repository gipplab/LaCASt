package gov.nist.drmf.interpreter.maple.wrapper.openmaple;

import gov.nist.drmf.interpreter.maple.wrapper.MapleException;

/**
 * @author Andre Greiner-Petter
 */
public interface Numeric extends Algebraic {
    int intValue() throws MapleException;

    double doubleValue() throws MapleException;
}
