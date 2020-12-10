package gov.nist.drmf.interpreter.generic.mlp;

import com.formulasearchengine.mathosphere.mlp.pojos.Relation;
import gov.nist.drmf.interpreter.generic.elasticsearch.AssumeElasticsearchAvailability;
import gov.nist.drmf.interpreter.generic.mlp.struct.MLPDependencyGraph;
import gov.nist.drmf.interpreter.generic.mlp.struct.MOIAnnotation;
import gov.nist.drmf.interpreter.pom.moi.MOINode;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Andre Greiner-Petter
 */
@AssumeElasticsearchAvailability
public class WikitextDocumentTest {
    @Test
    void simpleWikitextTest() throws IOException {
        String text = getResourceContent("simpleWikitest.xml");
        Document document = new WikitextDocument(text);

        MLPDependencyGraph semanticGraph = document.getMOIDependencyGraph();
        Collection<MOINode<MOIAnnotation>> nodes = semanticGraph.getVertices();
        assertEquals(1, nodes.size());

        List<MOINode<MOIAnnotation>> nodesList = new LinkedList<>(nodes);
        MOINode<MOIAnnotation> moi = nodesList.get(0);

        assertEquals("P_n^{(\\alpha, \\beta)} (x)", moi.getNode().getOriginalLaTeX());
        assertEquals(0, moi.getIngoingDependencies().size());
        assertEquals(0, moi.getOutgoingDependencies().size());

        List<Relation> relations = moi.getAnnotation().getAttachedRelations();
        assertEquals(2, relations.size());

        Collections.sort(relations);
        assertEquals("Jacobi polynomial", relations.get(0).getDefinition());
        assertEquals("Carl Gustav Jacob Jacobi", relations.get(1).getDefinition());
    }

    private String getResourceContent(String resourceFilename) throws IOException {
        return IOUtils.toString(this.getClass().getResourceAsStream(resourceFilename), StandardCharsets.UTF_8);
    }
}
