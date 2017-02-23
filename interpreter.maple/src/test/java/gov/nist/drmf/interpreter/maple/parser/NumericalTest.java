package gov.nist.drmf.interpreter.maple.parser;

import com.maplesoft.openmaple.Engine;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Created by AndreG-P on 23.02.2017.
 */
public class NumericalTest {

    private Engine t;

    public NumericalTest( Engine t ){
        this.t = t;
    }

    @Test
    public void simpleTest(){
        System.out.println("SimpleTest " + (3.5*Math.pow(10, -2)));
        assertTrue(true);
    }
}
