package gov.nist.drmf.interpreter.common.replacements;

import gov.nist.drmf.interpreter.common.meta.DLMF;
import gov.nist.drmf.interpreter.common.replacements.ReplacementConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Andre Greiner-Petter
 */
public class DLMFReplacementTest {
    private static ReplacementConfig CONFIG;

    @BeforeAll
    static void setup() {
        CONFIG = ReplacementConfig.getInstance();
    }

    @Test
    void iePiTest() {
        String input = "1 + i + e + \\pi";
        String replaced = CONFIG.replace(input, "1.1#E1");
        assertEquals("1 + \\iunit + \\expe + \\cpi", replaced);

        replaced = CONFIG.replace(input, null);
        assertEquals(input, replaced);
    }

    @Test
    void iTest() {
        String input = "i^2";
        String replaced = CONFIG.replace(input, "1.2#E3");
        assertEquals("\\iunit^2", replaced);
    }

    @Test
    @DLMF("22.2.2")
    void kTest() {
        String input = "k^{\\prime}=\\frac{1}{2}";
        String replaced = CONFIG.replace(input, "22.2.2");
        assertEquals("\\sqrt{1-k^2}=\\frac{1}{2}", replaced);
    }

    @Test
    void kSqTest() {
        String input = "{k^{\\prime}}^{2}=\\frac{1}{2}";
        String replaced = CONFIG.replace(input, "22.2.2");
        assertEquals("{1-k^2}=\\frac{1}{2}", replaced);
    }

    @Test
    void zetaTest() {
        String input = "\\zeta=\\frac{1}{2}";
        String replaced = CONFIG.replace(input, "9.6#E1");
        assertEquals("{\\frac{2}{3} z^{\\frac{3}{2}}}=\\frac{1}{2}", replaced);

        replaced = CONFIG.replace(input, "9.13#E1");
        assertEquals(input, replaced);
    }

    @Test
    @DLMF("9.2.9")
    void wronskianReplacementTest() {
        String input = "\\Wronskian\\left\\{\\AiryAi@{z e^{-2\\pi \\tfrac{i}{3}}}, \\AiryAi@{z e^{2\\pi \\tfrac{i}{3}}}\\right\\}";
        String replaced = CONFIG.replace(input, "9.2.9");
        assertEquals("\\Wronskian@{\\AiryAi@{z \\expe^{-2\\cpi \\tfrac{\\iunit}{3}}}, \\AiryAi@{z \\expe^{2\\cpi \\tfrac{\\iunit}{3}}}}", replaced);
    }
}
