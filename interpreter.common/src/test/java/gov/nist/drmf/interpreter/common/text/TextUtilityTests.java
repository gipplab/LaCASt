package gov.nist.drmf.interpreter.common.text;

import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Andre Greiner-Petter
 */
public class TextUtilityTests {
    @Test
    void appendPatternTest() {
        Pattern p = Pattern.compile("x(.*)");
        assertEquals("yz", TextUtility.appendPattern("xyz", p, 1));
    }

    @Test
    void appendPatternGroupTest() {
        Pattern p = Pattern.compile("(de)(fgh)(i)");
        assertEquals("abcdejklmnop", TextUtility.appendPattern("abcdefghijklmnop", p, 1));
        assertEquals("abcfghjklmnop", TextUtility.appendPattern("abcdefghijklmnop", p, 2));
        assertEquals("abcijklmnop", TextUtility.appendPattern("abcdefghijklmnop", p, 3));
    }
}
