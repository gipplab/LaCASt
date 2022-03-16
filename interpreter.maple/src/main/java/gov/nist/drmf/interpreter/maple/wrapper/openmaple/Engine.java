package gov.nist.drmf.interpreter.maple.wrapper.openmaple;

import gov.nist.drmf.interpreter.maple.wrapper.MapleException;

/**
 * com.maplesoft.openmaple.Engine
 *
 * @author Andre Greiner-Petter
 */
public interface Engine {
    Algebraic evaluate(String input) throws MapleException;

    void restart() throws MapleException;
}
