package gov.nist.drmf.interpreter.mlp.moi;

import mlp.ParseException;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Andre Greiner-Petter
 */
public class MOIDependencyGraphTests {

    @Test
    public void buildSimpleMOIDependencyGraph() throws ParseException {
        HashMap<String, String> library = new HashMap<>();
        library.put("1", "f(x)");
        library.put("2", "g(x)");
        library.put("3", "f(z) + g(z)");

        MOIDependencyGraph graph = MOIDependencyGraph.generateGraph(library);
        Collection<MOINode> verts = graph.getVertices();
        assertNotNull(verts);
        assertEquals(3, verts.size());

        Map<String, MOINode> vertMap = graph.getVerticesMap();
        MOINode f = vertMap.get("1");
        MOINode g = vertMap.get("2");
        MOINode fg = vertMap.get("3");

        assertNotNull(f);
        assertNotNull(g);
        assertNotNull(fg);

        assertTrue(f.isSource());
        assertTrue(f.getIngoingDependencies().isEmpty());
        assertTrue(g.isSource());
        assertTrue(g.getIngoingDependencies().isEmpty());

        assertTrue(fg.isSink());
        assertTrue(fg.getOutgoingDependencies().isEmpty());
    }

}
