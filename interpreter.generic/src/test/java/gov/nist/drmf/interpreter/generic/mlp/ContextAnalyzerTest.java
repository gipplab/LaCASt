package gov.nist.drmf.interpreter.generic.mlp;

import gov.nist.drmf.interpreter.common.tests.Resource;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Andre Greiner-Petter
 */
public class ContextAnalyzerTest {
    @Resource("simpleWikitest.xml")
    void simpleWikitextTest(String text) throws IOException {
        Document document = ContextAnalyzer.getDocument(text);
        assertTrue( document instanceof WikitextDocument );
    }
}
