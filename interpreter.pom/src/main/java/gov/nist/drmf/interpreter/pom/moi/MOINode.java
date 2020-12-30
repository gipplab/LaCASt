package gov.nist.drmf.interpreter.pom.moi;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Represents a node in an {@link MOIDependencyGraph}. A node is a single MOI which may have
 * ingoing and outgoing edges. A node can be annotated with an extra object or a list.
 *
 * @see MOIDependencyGraph
 * @see MOINode
 * @see DependencyPattern
 * @author Andre Greiner-Petter
 */
public class MOINode<T> implements INode<MOIDependency<T>> {
    // the id of the node
    private final String id;

    // the actual MOI element
    private final MathematicalObjectOfInterest moi;

    // the ingoing and outgoing nodes
    private final LinkedList<MOIDependency<T>> ingoing;
    private final LinkedList<MOIDependency<T>> outgoing;

    // an annotation
    private T annotation;

    /**
     * Keep Kryo happy for serialization
     */
    private MOINode() {
        this("-1", null, null);
    }

    public MOINode(String id, MathematicalObjectOfInterest moi, T annotation) {
        this.id = id;
        this.moi = moi;
        this.ingoing = new LinkedList<>();
        this.outgoing = new LinkedList<>();
        this.annotation = annotation;
    }

    public String getId() {
        return id;
    }

    public MathematicalObjectOfInterest getNode() {
        return moi;
    }

    /**
     * Get all dependent nodes (along ingoing edges).
     * @return sorted list (by layer) of dependent nodes
     */
    public List<MOINode<T>> getDependencyNodes() {
        List<MOINode<T>> dependants = new LinkedList<>();
        for ( MOIDependency<T> dependency : this.ingoing ) {
            dependants.add( dependency.getSource() );
        }

        return dependants;
    }

    @Override
    public Collection<MOIDependency<T>> getIngoingDependencies() {
        return ingoing;
    }

    @Override
    public Collection<MOIDependency<T>> getOutgoingDependencies() {
        return outgoing;
    }

    void addIngoingDependency(MOIDependency<T> ingoingDependency) {
        this.ingoing.add(ingoingDependency);
    }

    void addOutgoingDependency(MOIDependency<T> outgoingDependency) {
        this.outgoing.add(outgoingDependency);
    }

    /**
     * Checks if this node only depends on single identifiers. e.g., P_n^{(\alpha,\beta)}(x) include no other
     * complex MOI but only the single identifiers P, n, \alpha, \beta, and x. Of course this result only returns
     * true based on the graph structure not on actual MOI analysis. That means, that \cos{\sin{x}} also only depends
     * on single identifiers if \sin{x} does not appear in the graph and hence there is no dependency to \cos.
     *
     * If there are no ingoing dependencies false will be returned.
     *
     * @return true if all ingoing dependencies are single identifiers and the list of ingoing identifiers is not empty.
     */
    public boolean dependsOnlyOnIdentifier() {
        if ( ingoing.isEmpty() ) return false;
        return getIngoingDependencies().stream()
                .map( MOIDependency::getSource )
                .map( MOINode::getNode )
                .allMatch( MathematicalObjectOfInterest::isIdentifier );
    }

    /**
     * Returns true if this is an annotated node otherwise false.
     * @return true if the node is annotated otherwise false
     */
    public boolean hasAnnotation() {
        return annotation != null;
    }

    /**
     * Retrieve the annotation of this node. Might be null
     * @return the annotation of the node or null
     */
    public T getAnnotation() {
        return annotation;
    }

    /**
     * Sets or overwrites the existing annotation
     * @param annotation the new annotation
     */
    public void setAnnotation(T annotation) {
        this.annotation = annotation;
    }

    /**
     * Setup dependencies between this node and the given node. Might not change anything, if
     * neither this node does not match the given node or vice versa.
     * @param node the other node to setup dependencies with
     */
    public Set<MOIDependency<T>> setupDependency(MOINode<T> node) {
        Set<MOIDependency<T>> dependencies = new HashSet<>();
        DependencyPattern dependency = moi.match(node.moi);
        if ( Objects.nonNull(dependency) ) {
            MOIDependency<T> moiDep = new MOIDependency<>(this, node, dependency);
            outgoing.add(moiDep);
            node.ingoing.add(moiDep);
            dependencies.add(moiDep);
        }

        // reverse
        dependency = node.moi.match(moi);
        if ( Objects.nonNull(dependency) ) {
            MOIDependency<T> moiDep = new MOIDependency<>(node, this, dependency);
            ingoing.add(moiDep);
            node.outgoing.add(moiDep);
            dependencies.add(moiDep);
        }

        return dependencies;
    }
}
