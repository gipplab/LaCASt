package gov.nist.drmf.interpreter.common.text;

import org.junit.jupiter.api.Test;

import java.util.LinkedList;
import java.util.List;
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

    @Test
    void joinerTest() {
        List<String> testList = new LinkedList<>();
        testList.add("a");
        testList.add("b");
        testList.add("c");

        String output = TextUtility.join(", ", testList, (a) -> a);
        String stringJoinOutput = String.join(", ", testList);
        assertEquals(stringJoinOutput, output);
    }

    @Test
    void joinerMaxTest() {
        List<String> testList = new LinkedList<>();
        testList.add("a");
        testList.add("b");
        testList.add("c");

        String output = TextUtility.join(", ", testList, (a) -> a, 2, "and so on");
        assertEquals("a, b, and so on", output);
    }
}
