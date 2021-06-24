package gov.nist.drmf.interpreter.generic;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.wikidata.wdtk.wikibaseapi.apierrors.MediaWikiApiErrorException;

import java.io.IOException;

/**
 * @author Andre Greiner-Petter
 */
public class SemanticEnhancedDocumentBuilderTest {
    @Test
    @Disabled // just a dummy test for local purposes or private playing
    public void wikidataGeneratorTest() throws MediaWikiApiErrorException, IOException {
        SemanticEnhancedDocumentBuilder builder = SemanticEnhancedDocumentBuilder.getDefaultBuilder();
        builder.enhanceWikidataItem("Q371631");
    }
}
