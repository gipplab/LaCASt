package gov.nist.drmf.interpreter.common.latex;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Andre Greiner-Petter
 */
public class CaseSplitterTests {
    @Test
    void splitPmTest() {
        String in = "\\pm 1";
        test(CaseSplitter.splitPMSymbols(in),
                "+ 1", "- 1");
    }

    @Test
    void splitMpTest() {
        String in = "\\mp 1";
        test(CaseSplitter.splitPMSymbols(in),
                "- 1", "+ 1");
    }

    @Test
    void splitPmMpTest() {
        String in = "\\cos t = \\frac{\\cot t}{\\pm\\sqrt{1 + \\cot^2t}} = \\frac{-ma}{\\pm\\sqrt{m^2 a^2 + b^2}}";
        test(CaseSplitter.splitPMSymbols(in),
                "\\cos t = \\frac{\\cot t}{+\\sqrt{1 + \\cot^2t}} = \\frac{-ma}{+\\sqrt{m^2 a^2 + b^2}}",
                "\\cos t = \\frac{\\cot t}{-\\sqrt{1 + \\cot^2t}} = \\frac{-ma}{-\\sqrt{m^2 a^2 + b^2}}");
    }

    @Test
    void noSplitTest() {
        String in = "\\cos t = \\frac{\\cot t}{\\sqrt{1 + \\cot^2t}} = \\frac{-ma}{\\sqrt{m^2 a^2 + b^2}}";
        test(CaseSplitter.splitPMSymbols(in),
                "\\cos t = \\frac{\\cot t}{\\sqrt{1 + \\cot^2t}} = \\frac{-ma}{\\sqrt{m^2 a^2 + b^2}}");
    }

    private static void test(List<String> result, String... expecting) {
        assertEquals(result.size(), expecting.length, result.toString() + " but expected: " + Arrays.toString(expecting));
        for( int i = 0; i < expecting.length; i++ ) {
            assertEquals( expecting[i], result.get(i) );
        }
    }
}
