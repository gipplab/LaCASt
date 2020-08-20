package gov.nist.drmf.interpreter.common;

import gov.nist.drmf.interpreter.common.constants.Keys;
import gov.nist.drmf.interpreter.common.symbols.Constants;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.aggregator.ArgumentsAccessor;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.junit.jupiter.params.provider.EnumSource;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

/**
 * @author Andre Greiner-Petter
 */
public class ConstantTest {
    private static Constants g;

    private static Map<Integer, String> casMapping;

    @BeforeAll
    public static void init(){
        g = new Constants("","");
        try { g.init(); }
        catch ( IOException ioe ){
            System.err.println(ioe.getMessage());
            ioe.printStackTrace();
            fail("Exception during initialization.");
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @CsvFileSource(resources = "/MathConstants.csv", numLinesToSkip = 0)
    public void greekLetterMappingTest(ArgumentsAccessor arguments) {
        if ( arguments.getString(0).equals("Name") ) {
            casMapping = new HashMap<>();
            for ( int i = 2; i < arguments.size(); i++ ) {
                casMapping.put(i, arguments.getString(i));
            }
            return;
        }

        String latex = arguments.getString(1);
        for ( int i = 2; i < arguments.size(); i++ ) {
            if ( arguments.get(i) == null || arguments.getString(i).isBlank() ) continue;

            String cas = casMapping.get(i);
            String message = arguments.get(0) + " (CAS: " + cas + ")";
            assertEquals(arguments.getString(i), g.translate(Keys.KEY_DLMF, cas, latex), "Unable to forward translate " + message);
            assertEquals(latex, g.translate(cas, Keys.KEY_DLMF, arguments.getString(i)), "Unable to backward translate " + message);
        }
    }
}
