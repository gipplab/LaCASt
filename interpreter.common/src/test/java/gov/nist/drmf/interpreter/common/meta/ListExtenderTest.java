package gov.nist.drmf.interpreter.common.meta;

import org.junit.jupiter.api.Test;

import java.util.LinkedList;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Andre Greiner-Petter
 */
public class ListExtenderTest {

    @Test
    void addIfNotExistTest() {
        LinkedList<String> l1 = new LinkedList<>();
        LinkedList<String> l2 = new LinkedList<>();

        l1.add( "1" );
        l1.add( "2" );
        l1.add( "3" );

        l2.add( "3" );
        l2.add( "4" );
        ListExtender.addIfNotExist(l1, l2);
        assertEquals(4, l1.size());
        assertEquals("1", l1.get(0));
        assertEquals("2", l1.get(1));
        assertEquals("3", l1.get(2));
        assertEquals("4", l1.get(3));
    }

    @Test
    void conditionalAddTest() {
        LinkedList<String> l1 = new LinkedList<>();
        LinkedList<String> l2 = new LinkedList<>();

        l1.add( "1" );
        l1.add( "2" );
        l1.add( "3" );

        l2.add( "3" );
        l2.add( "4" );
        ListExtender.addAll(l1, l2, l1::contains);
        assertEquals(4, l1.size());
        assertEquals("1", l1.get(0));
        assertEquals("2", l1.get(1));
        assertEquals("3", l1.get(2));
        assertEquals("3", l1.get(3));
    }
}
