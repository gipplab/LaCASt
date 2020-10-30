package gov.nist.drmf.interpreter.pom.moi;

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
    public void buildSimpleMOIDependencyGraphTest() throws ParseException {
        HashMap<String, String> library = new HashMap<>();
        library.put("1", "f(x)");
        library.put("2", "g(x)");
        library.put("3", "f(z) + g(z)");

        MOIDependencyGraph graph = MOIDependencyGraphBuilder.generateGraph(library);
        Collection<MOINode<?>> verts = graph.getVertices();
        assertNotNull(verts);
        assertEquals(3, verts.size());

        Map<String, MOINode<?>> vertMap = graph.getVerticesMap();
        MOINode<?> f = vertMap.get("1");
        MOINode<?> g = vertMap.get("2");
        MOINode<?> fg = vertMap.get("3");

        assertNotNull(f);
        assertNotNull(g);
        assertNotNull(fg);

        assertTrue(f.isSource());
        assertTrue(f.getIngoingDependencies().isEmpty());
        assertEquals(1, f.getOutgoingDependencies().size());

        assertTrue(g.isSource());
        assertTrue(g.getIngoingDependencies().isEmpty());
        assertEquals(1, g.getOutgoingDependencies().size());

        assertTrue(fg.isSink());
        assertTrue(fg.getOutgoingDependencies().isEmpty());
        assertEquals(2, fg.getIngoingDependencies().size());
    }

    @Test
    public void singleIdentifierMOIGraphTest() throws ParseException {
        HashMap<String, String> library = new HashMap<>();
        library.put("1", "z");
        library.put("2", "g(z)");

        MOIDependencyGraph graph = MOIDependencyGraphBuilder.generateGraph(library);
        Collection<MOINode<?>> verts = graph.getVertices();
        assertNotNull(verts);
        assertEquals(2, verts.size());

        Map<String, MOINode<?>> vertMap = graph.getVerticesMap();
        MOINode<?> f = vertMap.get("1");
        MOINode<?> g = vertMap.get("2");

        assertNotNull(f);
        assertNotNull(g);

        assertEquals(1, f.getOutgoingDependencies().size());
        assertEquals(1, g.getIngoingDependencies().size());

        assertEquals(0, f.getIngoingDependencies().size());
        assertEquals(0, g.getOutgoingDependencies().size());
    }
}
