package gov.nist.drmf.interpreter.pom.moi;

import gov.nist.drmf.interpreter.common.interfaces.IMapStringFunction;
import mlp.ParseException;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
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

        MOIDependencyGraph<Void> graph = MOIDependencyGraphBuilder.generateGraph(library);
        Collection<MOINode<Void>> verts = graph.getVertices();
        assertNotNull(verts);
        assertEquals(3, verts.size());

        Map<String, MOINode<Void>> vertMap = graph.getVerticesMap();
        MOINode<Void> f = vertMap.get("1");
        MOINode<Void> g = vertMap.get("2");
        MOINode<Void> fg = vertMap.get("3");

        List<MOINode<Void>> dependencyNodes = fg.getDependencyNodes();
        assertEquals(2, dependencyNodes.size());
        assertTrue(dependencyNodes.contains(f));
        assertTrue(dependencyNodes.contains(g));

        assertFalse(f.dependsOnlyOnIdentifier());
        assertFalse(g.dependsOnlyOnIdentifier());
        assertFalse(fg.dependsOnlyOnIdentifier());

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

        assertEquals(1, graph.getSinks().size());
        assertEquals(2, graph.getSources().size());

        assertTrue(graph.containsNode("3"));
        graph.removeNode("3");
        assertFalse(graph.containsNode("3"));
        assertEquals(0, f.getOutgoingDependencies().size());
        assertEquals(0, g.getOutgoingDependencies().size());
        assertEquals(0, fg.getIngoingDependencies().size());

        assertEquals(2, graph.getSinks().size());
        assertEquals(2, graph.getSources().size());
    }

    @Test
    public void singleIdentifierMOIGraphTest() throws ParseException {
        HashMap<String, String> library = new HashMap<>();
        library.put("1", "g(z)");
        library.put("2", "z");

        MOIDependencyGraph<Void> graph = MOIDependencyGraphBuilder.generateGraph(library);
        Collection<MOINode<Void>> verts = graph.getVertices();
        assertNotNull(verts);
        assertEquals(2, verts.size());

        Map<String, MOINode<Void>> vertMap = graph.getVerticesMap();
        MOINode<Void> g = vertMap.get("1");
        MOINode<Void> f = vertMap.get("2");

        assertNotNull(f);
        assertNotNull(g);

        assertFalse(f.hasAnnotation());
        assertFalse(g.hasAnnotation());

        assertFalse(f.dependsOnlyOnIdentifier());
        assertTrue(g.dependsOnlyOnIdentifier());

        assertEquals(1, f.getOutgoingDependencies().size());
        assertEquals(1, g.getIngoingDependencies().size());

        assertEquals(0, f.getIngoingDependencies().size());
        assertEquals(0, g.getOutgoingDependencies().size());

        assertEquals(1, graph.getSinks().size());
        assertEquals(1, graph.getSources().size());

        assertTrue(graph.containsNode("2"));
        graph.removeNode("2");
        assertFalse(graph.containsNode("2"));
        assertEquals(0, f.getOutgoingDependencies().size());
        assertEquals(0, f.getIngoingDependencies().size());

        assertEquals(1, verts.size());

        assertEquals(0, g.getOutgoingDependencies().size());
        assertEquals(0, g.getIngoingDependencies().size());

        // since f is no longer part of the graph, the remaining g is both, source and sink
        assertEquals(1, graph.getSinks().size());
        assertEquals(1, graph.getSources().size());
    }

    @Test
    public void nonMatchableMOIGraphTest() throws ParseException {
        HashMap<String, String> library = new HashMap<>();
        library.put("1", "a b");

        MOIDependencyGraph<Void> graph = MOIDependencyGraphBuilder.generateGraph(library);
        Collection<MOINode<Void>> verts = graph.getVertices();
        assertNotNull(verts);
        assertEquals(1, verts.size());

        MOINode<Void> node = graph.getNode("1");
        assertNull(node.getAnnotation());
    }

    @Test
    public void annotatedMOIGraphTest() throws ParseException {
        HashMap<String, Integer> library = new HashMap<>();
        library.put("1", 123);

        MOIDependencyGraph<Integer> graph = MOIDependencyGraphBuilder.generateAnnotatedGraph(library, Object::toString);
        Collection<MOINode<Integer>> verts = graph.getVertices();
        assertNotNull(verts);
        assertEquals(1, verts.size());
        MOINode<Integer> node = graph.getNode("1");
        assertTrue(node.hasAnnotation());
        assertEquals(123, node.getAnnotation());
    }
}
