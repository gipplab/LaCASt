package gov.nist.drmf.interpreter.maple.translation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.maplesoft.externalcall.MapleException;
import com.maplesoft.openmaple.Algebraic;
import com.maplesoft.openmaple.Engine;
import com.maplesoft.openmaple.EngineCallBacks;
import com.maplesoft.openmaple.List;
import gov.nist.drmf.interpreter.common.constants.GlobalPaths;
import gov.nist.drmf.interpreter.maple.setup.AssumeMapleAvailability;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by AndreG-P on 23.02.2017.
 */
@AssumeMapleAvailability
public class EngineTest {
    private static Engine t;
    private static Algebraic
            example_query,
            list,
            proc_alg;

    private static String procedure_list, procedure_order;

    @BeforeAll
    public static void startEngine() throws IOException {
        MapleInterface mi = MapleInterface.getUniqueMapleInterface();

//        assertTrue( Initializer.loadMapleNatives(), "Cannot load maples native libs.");
//
//        String[] args = new String[]{"java"};
//        try {
//            t = new Engine( args, new CallBacks(), null, null );
//        } catch ( MapleException me ){
//            me.printStackTrace();
//            fail("Cannot initialize engine: " + me.getMessage());
//        }

        t = mi.getEngine();

        // loading procedure from file.
        String proc1 = "", proc2 = "";
        // try to collect a stream.
        System.out.println(Paths.get("").toAbsolutePath());

        try ( Stream<String> stream = Files.lines( GlobalPaths.PATH_MAPLE_LIST_PROCEDURE ) ){
            proc1 = stream.collect( Collectors.joining(System.lineSeparator()) );
            stream.close(); // not really necessary
            procedure_list = proc1.split(":=")[0].trim();
        } catch (IOException ioe){
            ioe.printStackTrace();
            fail("Cannot load procedure from file: " + GlobalPaths.PATH_MAPLE_LIST_PROCEDURE );
        }

        try ( Stream<String> stream = Files.lines( GlobalPaths.PATH_MAPLE_TO_INERT_PROCEDURE ) ){
            proc2 = stream.collect( Collectors.joining(System.lineSeparator()) );
            stream.close(); // not really necessary
            procedure_order = proc2.split(":=")[0].trim();
        } catch (IOException ioe){
            ioe.printStackTrace();
            fail("Cannot load procedure from file: " + GlobalPaths.PATH_MAPLE_TO_INERT_PROCEDURE );
        }

        try{
            t.evaluate(proc1);
            t.evaluate(proc2);
            example_query = t.evaluate("int(x,x);");
            list = t.evaluate("convert(ToInert('a+3'), list);");
            proc_alg = t.evaluate( procedure_list + "(" + procedure_order + "('a+3'));");
        } catch ( MapleException me ){
            me.printStackTrace();
            fail("Cannot evaluate an expression.");
        }
    }

    @Test
    public void exampleAlgebraicTest(){
        String sol = example_query.toString();
        assertEquals( "1/2*x^2", sol, "Wrong integral string." );
    }

    @Test
    public void listTest(){
        if ( !(list instanceof List) )
            fail("Assumed a list algebraic object but it isn't." + list);
        try {
            List l = (List)list;
            assertEquals( 2, l.length(), "Wrong length of list.");
        } catch ( Exception e ){
            fail("Exception thrown.");
        }
    }

    @Test
    public void procedureTest(){
        if ( !(proc_alg instanceof List) )
            fail("Assumed a list algebraic object but it isn't. " + proc_alg);
        try {
            List l = (List)proc_alg;
            assertEquals( 3, l.length(), "Wrong length of list after procedure.");
            assertEquals( "_Inert_SUM", l.select(1).toString(), "List is not a sum!" );
            if ( !(l.select(2) instanceof List) )
                fail( "First argument ist not a list." );
            if ( !(l.select(3) instanceof List) )
                fail( "Second argument ist not a list." );
        } catch ( Exception e ){
            fail("Exception thrown.");
        }
    }

    private static class CallBacks implements EngineCallBacks {
        @Override
        public void textCallBack(Object o, int i, String s) throws MapleException {
            // nothing to do here, that's ok
        }

        @Override
        public void errorCallBack(Object o, int i, String s) throws MapleException {
            fail("Error message from Maple. " + s);
        }

        @Override
        public void statusCallBack(Object o, long l, long l1, double v) throws MapleException {
            fail("Status update shouldn't happen.");
        }

        @Override
        public String readLineCallBack(Object o, boolean b) throws MapleException {
            fail("Read line shouldn't happen.");
            return null;
        }

        @Override
        public boolean redirectCallBack(Object o, String s, boolean b) throws MapleException {
            fail("No redirected calls allowed in this test suit.");
            return false;
        }

        @Override
        public String callBackCallBack(Object o, String s) throws MapleException {
            fail("No callback methods are allowed.");
            return null;
        }

        @Override
        public boolean queryInterrupt(Object o) throws MapleException {
            fail("No query interruptions is allowed in this test.");
            return false;
        }

        @Override
        public String streamCallBack(Object o, String s, String[] strings) throws MapleException {
            fail("No streaming is allowed here.");
            return null;
        }
    }
}
