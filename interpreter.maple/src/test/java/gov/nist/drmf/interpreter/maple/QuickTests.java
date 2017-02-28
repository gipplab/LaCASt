package gov.nist.drmf.interpreter.maple;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Created by AndreG-P on 28.02.2017.
 */
public class QuickTests {
    @Test
    public void arraysTest(){
        String[] empty = new String[]{};
        assertEquals("[]", Arrays.toString(empty), "Not equal string!" );
    }
}
