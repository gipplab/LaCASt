package gov.nist.drmf.interpreter.maple.wrapper.openmaple;

import gov.nist.drmf.interpreter.maple.wrapper.MapleException;

/**
 * Interface for:
 * com.maplesoft.openmaple.List
 *
 * @author Andre Greiner-Petter
 */
public interface MapleList extends Algebraic, java.util.List<Algebraic> {
    int length() throws MapleException;

    Algebraic get(int i);

    Algebraic select(int i) throws MapleException;

    java.util.List<Algebraic> subList(int fromIndex, int toIndex);
}
