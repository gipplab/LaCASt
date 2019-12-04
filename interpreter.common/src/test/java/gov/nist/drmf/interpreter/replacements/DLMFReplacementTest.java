package gov.nist.drmf.interpreter.replacements;

import gov.nist.drmf.interpreter.common.meta.DLMF;
import gov.nist.drmf.interpreter.common.replacements.DLMFReplacementConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Andre Greiner-Petter
 */
public class DLMFReplacementTest {
    private static DLMFReplacementConfig CONFIG;

    @BeforeAll
    static void setup() throws IOException {
        CONFIG = DLMFReplacementConfig.getInstance();
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
        assertEquals("1-k^2=\\frac{1}{2}", replaced);
    }

    @Test
    void zetaTest() {
        String input = "\\zeta=\\frac{1}{2}";
        String replaced = CONFIG.replace(input, "9.6#E1");
        assertEquals("{\\frac{2}{3} z^{\\frac{3}{2}}}=\\frac{1}{2}", replaced);

        replaced = CONFIG.replace(input, "9.13#E1");
        assertEquals(input, replaced);
    }
}
