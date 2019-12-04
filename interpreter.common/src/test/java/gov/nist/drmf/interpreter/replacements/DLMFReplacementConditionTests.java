package gov.nist.drmf.interpreter.replacements;

import gov.nist.drmf.interpreter.common.meta.DLMF;
import gov.nist.drmf.interpreter.common.replacements.DLMFReplacementCondition;
import gov.nist.drmf.interpreter.common.replacements.IReplacementCondition;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Andre Greiner-Petter
 */
public class DLMFReplacementConditionTests {
    @Test
    void illegalPatternTest() {
        assertThrows( IllegalArgumentException.class, ()->new DLMFReplacementCondition("ab.e1") );
    }

    @Test
    void matchTest(){
        DLMFReplacementCondition d1 = new DLMFReplacementCondition("10.3");
        DLMFReplacementCondition d2 = new DLMFReplacementCondition("10.3#E3");

        assertTrue( d1.match(d2) );
        assertFalse( d2.match(d1) );
        assertEquals( 0, d1.compareTo(d2) );
    }

    @Test
    void matchFailTest() {
        DLMFReplacementCondition d1 = new DLMFReplacementCondition("11.3");
        DLMFReplacementCondition d2 = new DLMFReplacementCondition("10.3#E3");

        assertFalse( d1.match(d2) );
        assertFalse( d2.match(d1) );
        assertTrue( d1.compareTo(d2) > 0 );
        assertTrue( d2.compareTo(d1) < 0 );
    }

    @Test
    void rangeTest(){
        DLMFReplacementCondition d1 = new DLMFReplacementCondition("10.1#E1");
        DLMFReplacementCondition d2 = new DLMFReplacementCondition("10.3#E3");
        DLMFReplacementCondition d3 = new DLMFReplacementCondition("11.5#E5");

        assertTrue(IReplacementCondition.withinRange(d1, d3, d2));
        assertTrue(IReplacementCondition.withinRange(d3, d1, d2));
        assertTrue(IReplacementCondition.withinRange(d1, d1, d1));
        assertFalse(IReplacementCondition.withinRange(d2, d3, d1));
        assertFalse(IReplacementCondition.withinRange(d1, d2, d3));
        assertFalse(IReplacementCondition.withinRange(d2, d2, d3));
    }
}
