package gov.nist.drmf.interpreter.generic.eval;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Andre Greiner-Petter
 */
public class TexTableEntryTest {

    @Test
    public void zeroIdTest() {
        TexTableEntry e = new TexTableEntry(0);
        assertEquals("AA", e.buildID(""));
    }

    @Test
    public void oneIdTest() {
        TexTableEntry e = new TexTableEntry(1);
        assertEquals("AB", e.buildID(""));
    }

    @Test
    public void fiftyIdTest() {
        TexTableEntry e = new TexTableEntry(50);
        assertEquals("BY", e.buildID(""));
    }

    @Test
    public void edgeLowerIdTest() {
        TexTableEntry e = new TexTableEntry(25);
        assertEquals("AZ", e.buildID(""));
    }

    @Test
    public void edgeUpperIdTest() {
        TexTableEntry e = new TexTableEntry(26);
        assertEquals("BA", e.buildID(""));
    }

    @Test
    public void edgeMaxIdTest() {
        TexTableEntry e = new TexTableEntry((int)Math.pow(26, 2)-1);
        assertEquals("ZZ", e.buildID(""));
    }
}
