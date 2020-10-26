package gov.nist.drmf.interpreter.mlp.moi;

import gov.nist.drmf.interpreter.mlp.SemanticMLPWrapper;
import gov.nist.drmf.interpreter.mlp.extensions.PrintablePomTaggedExpression;
import mlp.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Andre Greiner-Petter
 */
public class MOIDependencyGraph {
    private static final Logger LOG = LogManager.getLogger(MOIDependencyGraph.class.getName());

    private static final SemanticMLPWrapper mlp = SemanticMLPWrapper.getStandardInstance();

    private final HashMap<String, MOINode> vertices;

    public MOIDependencyGraph() {
        this.vertices = new HashMap<>();
    }

    public void addNode(String id, String moi) throws ParseException {
        try {
            addNode(id, mlp.parse(moi));
        } catch (Exception e) {
            LOG.warn("Unable to generate MOI node for: " + moi, e);
        }
    }

    public void addNode(String id, PrintablePomTaggedExpression moi) {
        MOINode node = new MOINode(id, new MathematicalObjectOfInterest(moi));
        LOG.debug("Add node for moi: " + moi.getTexString());
        updateDependencies(node);
        vertices.put(node.getId(), node);
    }

    private void updateDependencies(MOINode node) {
        for (MOINode ref : vertices.values() ) {
            node.setupDependency(ref);
        }
    }

    public Map<String, MOINode> getVerticesMap() {
        return vertices;
    }

    public Collection<MOINode> getVertices() {
        return vertices.values();
    }

    public Collection<MOINode> getSinks() {
        return vertices.values().stream().filter(INode::isSink).collect(Collectors.toSet());
    }

    public Collection<MOINode> getSources() {
        return vertices.values().stream().filter(INode::isSource).collect(Collectors.toSet());
    }

    public static MOIDependencyGraph generateGraph(HashMap<String, String> mathNodeLibrary) throws ParseException {
        MOIDependencyGraph graph = new MOIDependencyGraph();
        LOG.info("Generate graph with " + mathNodeLibrary.size() + " nodes");

        for (Map.Entry<String, String> mathNode : mathNodeLibrary.entrySet()) {
            graph.addNode(mathNode.getKey(), mathNode.getValue());
        }

        return graph;
    }
}
