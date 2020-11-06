package gov.nist.drmf.interpreter.generic.elasticsearch;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Andre Greiner-Petter
 */
@AssumeElasticsearchAvailability
public class ElasticSearchConnectorTests {
    private static ElasticSearchConnector es;

    @BeforeAll
    static void setup() throws IOException {
        es = ElasticSearchConnector.getDefaultInstance();
        es.indexDLMFDatabase();
    }

    @Test
    void searchLeviCivitaTest() throws IOException {
        List<MacroResult> results = es.searchMacroDescription("Levi Civita Symbol");
        assertNotNull(results);
        assertTrue( results.size() > 1 );
        assertEquals( "LeviCivitasym", results.get(0).getMacro().getName() );
        assertTrue( results.get(0).getScore() > 5.0 );
    }
}
