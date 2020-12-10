package gov.nist.drmf.interpreter.generic.mlp;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Andre Greiner-Petter
 */
public class ContextAnalyzerTest {
    @Test
    void simpleWikitextTest() throws IOException {
        String text = getResourceContent("simpleWikitest.xml");
        Document document = ContextAnalyzer.getDocument(text);
        assertTrue( document instanceof WikitextDocument );
    }

    private String getResourceContent(String resourceFilename) throws IOException {
        return IOUtils.toString(this.getClass().getResourceAsStream(resourceFilename), StandardCharsets.UTF_8);
    }
}
