package gov.nist.drmf.interpreter.roundtrip;

import gov.nist.drmf.interpreter.MapleTranslator;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.function.Executable;

import java.util.LinkedList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *
 * Created by AndreG-P on 06.03.2017.
 */
public abstract class AbstractRoundTrip {
    MapleTranslator translator;

    Iterable<DynamicTest> createFromMapleTestList( String[] tests, String[] names ){
        List<DynamicTest> list = new LinkedList<>();
        for ( int i = 0; i < tests.length; i++ ){
            Executable exc = createFromMapleTestCase( tests[i] );
            list.add( DynamicTest.dynamicTest( names[i], exc) );
        }
        return list;
    }

    private Executable createFromMapleTestCase( String maple_1 ){
        return () -> {
            String maple_2 = translator.oneCycleRoundTripTranslationFromMaple( maple_1 );
            String message = "Not symbolically equivalent! Expected: " + maple_1 + System.lineSeparator();
            message += "But get: " + maple_2;
            assertTrue(
                    translator.getMapleSimplifier().simplificationTester( maple_1, maple_2 ),
                    message
            );
        };
    }
}
