package gov.nist.drmf.interpreter.pom.moi;

import gov.nist.drmf.interpreter.common.exceptions.NotMatchableException;
import gov.nist.drmf.interpreter.pom.SemanticMLPWrapper;
import gov.nist.drmf.interpreter.pom.extensions.PrintablePomTaggedExpression;
import mlp.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Andre Greiner-Petter
 */
public class MOIDependencyGraph implements IMOIGraph {
    private static final Logger LOG = LogManager.getLogger(MOIDependencyGraph.class.getName());

    private static final SemanticMLPWrapper mlp = SemanticMLPWrapper.getStandardInstance();

    // all vertices of this graph
    private final HashMap<String, MOINode<?>> vertices;
    private final HashMap<Connection, MOIDependency> edges;

    public MOIDependencyGraph() {
        this.vertices = new HashMap<>();
        this.edges = new HashMap<>();
    }

    @Override
    public MOINode<Void> addNode(String id, String moi) throws ParseException, NotMatchableException {
        return addNode(id, moi, null);
    }

    @Override
    public <T> MOINode<T> addNode(String id, String moi, T annotation) throws ParseException, NotMatchableException {
        return addNode(id, mlp.parse(moi), annotation);
    }

    /**
     * Adds a node to the graph with the given annotation.
     * @param id the unique node ID
     * @param moi the mathematical object of interest
     * @param annotation the annotation object (or null)
     * @param <T> the annotation class or {@link Void} in case of null-annotation
     * @return the added Node
     */
    public <T> MOINode<T> addNode(String id, PrintablePomTaggedExpression moi, T annotation) throws NotMatchableException {
        MOINode<T> node = new MOINode<>(id, new MathematicalObjectOfInterest(moi), annotation);
        LOG.debug("Add node for moi: " + moi.getTexString());
        updateDependencies(node);
        vertices.put(node.getId(), node);
        return node;
    }

    /**
     * Updates all dependencies for the given node (in- and outgoing edges are updated/generated)
     * @param node the node
     */
    private void updateDependencies(MOINode<?> node) throws NotMatchableException {
        for (MOINode<?> ref : vertices.values() ) {
            Set<MOIDependency> dependencies = node.setupDependency(ref);
            for ( MOIDependency dependency : dependencies ) {
                this.edges.put(
                        new Connection(
                                dependency.getSource().getId(),
                                dependency.getSink().getId()),
                        dependency
                );
            }
        }
    }

    @Override
    public MOINode<?> removeNode(String id) {
        MOINode<?> node = vertices.remove(id);
        if ( node == null ) return null;

        Collection<MOIDependency> outgoingEdges = node.getOutgoingDependencies();
        for ( MOIDependency out : outgoingEdges ) {
            MOINode<?> outNode = out.getSink();
            Collection<MOIDependency> outNodeInEdges = outNode.getIngoingDependencies();
            MOIDependency reverseDep = this.edges.remove(new Connection(outNode.getId(), node.getId()));
            if ( reverseDep != null ) outNodeInEdges.remove(reverseDep);
            this.edges.remove(new Connection(node.getId(), outNode.getId()));
        }
        outgoingEdges.clear();

        Collection<MOIDependency> ingoingEdges = node.getIngoingDependencies();
        for ( MOIDependency in : ingoingEdges ) {
            MOINode<?> inNode = in.getSource();
            Collection<MOIDependency> inNodeOutEdges = inNode.getOutgoingDependencies();
            MOIDependency reverseDep = this.edges.remove(new Connection(node.getId(), inNode.getId()));
            if ( reverseDep != null ) inNodeOutEdges.remove(reverseDep);
            this.edges.remove(new Connection(inNode.getId(), node.getId()));
        }
        ingoingEdges.clear();

        return node;
    }

    @Override
    public boolean containsNode(String id) {
        return vertices.containsKey(id);
    }

    @Override
    public MOINode<?> getNode(String id) {
        return vertices.get(id);
    }

    public Map<String, MOINode<?>> getVerticesMap() {
        return vertices;
    }

    public Collection<MOINode<?>> getVertices() {
        return vertices.values();
    }

    public Collection<MOINode<?>> getSinks() {
        return vertices.values().stream().filter(INode::isSink).collect(Collectors.toSet());
    }

    public Collection<MOINode<?>> getSources() {
        return vertices.values().stream().filter(INode::isSource).collect(Collectors.toSet());
    }

    private static class Connection {
        final String in, out;

        private Connection(String in, String out) {
            this.in = in;
            this.out = out;
        }
    }
}
