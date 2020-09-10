package gov.nist.drmf.interpreter.maple.extension;

import com.maplesoft.externalcall.MapleException;
import com.maplesoft.openmaple.Algebraic;
import gov.nist.drmf.interpreter.common.exceptions.ComputerAlgebraSystemEngineException;
import gov.nist.drmf.interpreter.maple.setup.AssumeMapleAvailability;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * @author Andre Greiner-Petter
 */
@AssumeMapleAvailability
public class MapleInterfaceTest {

    private static MapleInterface maple;

    @BeforeAll
    static void setup() {
        maple = MapleInterface.getUniqueMapleInterface();
    }

    @Test
    void setGlobalAssumptionTest() throws ComputerAlgebraSystemEngineException, MapleException {
        maple.setGlobalAssumptions("a > 0");
        Algebraic a = maple.evaluate("int(x^a, x = 0..1);");
        assertEquals("1/(a+1)", a.toString());
        maple.evaluate("a := 'a';"); // unassume
    }

    @Test
    void withoutAssumptionTest() throws MapleException {
        Algebraic a = maple.evaluate("int(x^a, x = 0..1);");
        assertNotEquals("1/(a+1)", a.toString());
    }

}
