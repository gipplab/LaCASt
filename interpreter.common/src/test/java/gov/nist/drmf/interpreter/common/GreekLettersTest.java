package gov.nist.drmf.interpreter.common;

import gov.nist.drmf.interpreter.common.constants.Keys;
import gov.nist.drmf.interpreter.common.symbols.GreekLetters;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.aggregator.ArgumentsAccessor;
import org.junit.jupiter.params.provider.CsvFileSource;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Created by Andre Greiner-Petter on 10.11.2016.
 */
public class GreekLettersTest {
    private static GreekLetters g;

    private static Map<Integer, String> casMapping;

    @BeforeAll
    static void init(){
        g = new GreekLetters("","");
        try { g.init(); }
        catch ( IOException ioe ){
            System.err.println(ioe.getMessage());
            ioe.printStackTrace();
            fail("Exception during initialization.");
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @CsvFileSource(resources = "/GreekLetterTestCases.csv", numLinesToSkip = 0)
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
            assertEquals(arguments.getString(i), g.translate(Keys.KEY_LATEX, cas, latex), "Unable to forward translate " + message);
            assertEquals(latex, g.translate(cas, Keys.KEY_LATEX, arguments.getString(i)), "Unable to backward translate " + message);
        }
    }
}
