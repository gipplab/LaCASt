package gov.nist.drmf.interpreter.maple;

import com.maplesoft.openmaple.Engine;
import gov.nist.drmf.interpreter.maple.parser.NumericalTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Created by AndreG-P on 23.02.2017.
 */
public class ParserTests {
    private static Engine engine;

    @BeforeAll
    public static void setup(){

    }

    @Nested
    class InnerNumericalTests extends NumericalTest {
        InnerNumericalTests(){ super(engine); }
    }
}
