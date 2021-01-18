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
public class MOIDependencyGraph<T> implements IMOIGraph<T> {
    private static final Logger LOG = LogManager.getLogger(MOIDependencyGraph.class.getName());

    private static final SemanticMLPWrapper mlp = SemanticMLPWrapper.getStandardInstance();

    // all vertices of this graph
    private final HashMap<String, MOINode<T>> vertices;
    private final HashMap<Connection, MOIDependency<T>> edges;

    public MOIDependencyGraph() {
        this.vertices = new HashMap<>();
        this.edges = new HashMap<>();
    }

    @Override
    public MOINode<T> addNode(String id, String moi) throws ParseException, NotMatchableException {
        return addNode(id, moi, null);
    }

    @Override
    public MOINode<T> addNode(String id, String moi, T annotation) throws ParseException, NotMatchableException {
        if ( containsNode(id) ) return getNode(id);
        return addNode(id, moi, mlp.parse(moi), annotation);
    }

    /**
     * Adds a node to the graph with the given annotation.
     * @param id the unique node ID
     * @param latex the original latex string that was used to generate the MOI
     * @param moi the mathematical object of interest
     * @param annotation the annotation object (or null)
     * @return the added Node
     */
    public MOINode<T> addNode(String id, String latex, PrintablePomTaggedExpression moi, T annotation) throws NotMatchableException {
        // the node already exists, so no need to add a new one, the user do not need to know that
        // because there is no difference between two identical nodes in the graph or just one of them
        if ( containsNode(id) ) {
            LOG.debug("Avoiding node duplications and return the existing node in the graph.");
            return getNode(id);
        }

        LOG.info("Add new MOI node to graph: " + moi.getTexString());
        MOINode<T> node = new MOINode<>(id, new MathematicalObjectOfInterest(latex, moi), annotation);
        LOG.info("Setup dependencies for new node");
        updateDependencies(node);
        vertices.put(node.getId(), node);
        return node;
    }

    /**
     * Adds a node to the existing graph without adding any dependencies in between.
     * If a node exists with the same ID this method does nothing.
     * @param node the node to add
     */
    protected void addNode(MOINode<T> node) {
        if ( containsNode(node.getId()) ) return;
        vertices.put(node.getId(), node);
    }

    /**
     * Updates all dependencies for the given node (in- and outgoing edges are updated/generated)
     * @param node the node
     */
    private void updateDependencies(MOINode<T> node) throws NotMatchableException {
        for (MOINode<T> ref : vertices.values() ) {
            Set<MOIDependency<T>> dependencies = node.setupDependency(ref);
            for ( MOIDependency<T> dependency : dependencies ) {
                this.edges.put(
                        new Connection(
                                dependency.getSourceNode().getId(),
                                dependency.getSinkNode().getId()),
                        dependency
                );
            }
        }
    }

    protected void addDependency(MOINode<T> source, MOINode<T> sink) {
        if ( source == null || sink == null ) return;
        MOIDependency<T> dependency = new MOIDependency<>(source, sink);
        this.edges.put(
                new Connection(
                        dependency.getSourceNode().getId(),
                        dependency.getSinkNode().getId()),
                dependency
        );
        source.addOutgoingDependency(dependency);
        sink.addIngoingDependency(dependency);
    }

    @Override
    public MOINode<T> removeNode(String id) {
        MOINode<T> node = vertices.remove(id);
        if ( node == null ) return null;

        Collection<? extends IDependency<T>> outgoingEdges = node.getOutgoingDependencies();
        for ( IDependency<T> out : outgoingEdges ) {
            MOINode<T> outNode = (MOINode<T>) out.getSink();
            Collection<? extends IDependency<T>> outNodeInEdges = outNode.getIngoingDependencies();
            outNodeInEdges.remove(out);
            this.edges.remove(new Connection(node.getId(), outNode.getId()));
        }
        outgoingEdges.clear();

        Collection<? extends IDependency<T>> ingoingEdges = node.getIngoingDependencies();
        for ( IDependency<T> in : ingoingEdges ) {
            MOINode<T> inNode = (MOINode<T>) in.getSource();
            Collection<? extends IDependency<T>> inNodeOutEdges = inNode.getOutgoingDependencies();
            inNodeOutEdges.remove(in);
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
    public MOINode<T> getNode(String id) {
        return vertices.get(id);
    }

    public Map<String, MOINode<T>> getVerticesMap() {
        return vertices;
    }

    public Collection<MOINode<T>> getVertices() {
        return vertices.values();
    }

    public Collection<MOINode<T>> getSinks() {
        return vertices.values().stream().filter(INode::isSink).collect(Collectors.toSet());
    }

    public Collection<MOINode<T>> getSources() {
        return vertices.values().stream().filter(INode::isSource).collect(Collectors.toSet());
    }
}
