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
    private final T annotation;

    /**
     * Keep Kryo happy for serialization
     */
    private MOINode() {
        this("-1", null);
    }

    public MOINode(String id, MathematicalObjectOfInterest moi) {
        this(id, moi, null);
    }

    public MOINode(String id, MathematicalObjectOfInterest moi, T annotation) {
        this.id = id;
        this.moi = moi;
        this.ingoing = new LinkedList<>();
        this.outgoing = new LinkedList<>();
        this.annotation = annotation;
    }

//    public <S> MOINode(MOINode<S> reference, Function<S, T> annotationConverter) {
//        this.id = reference.id;
//        this.moi = reference.moi;
//        this.ingoing = new LinkedList<T>(reference.ingoing);
//        this.outgoing = new LinkedList<T>(reference.outgoing);
//        this.annotation = annotationConverter.apply(reference.annotation);
//    }

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
        List<MOINode<T>> directDependencies = this.ingoing.stream()
                .map( MOIDependency::getSource ).collect(Collectors.toList());

        List<MOINode<T>> nodes = new LinkedList<>(directDependencies);
        directDependencies.forEach( node -> nodes.addAll(node.getDependencyNodes()) );
        return nodes;
    }

    @Override
    public Collection<MOIDependency<T>> getIngoingDependencies() {
        return ingoing;
    }

    @Override
    public Collection<MOIDependency<T>> getOutgoingDependencies() {
        return outgoing;
    }

    public boolean dependsOnlyOnIdentifier() {
        return getIngoingDependencies().stream()
                .map( MOIDependency::getSource )
                .map( MOINode::getNode )
                .anyMatch(m -> !m.isIdentifier() );
    }

    public boolean hasAnnotation() {
        return annotation != null;
    }

    public T getAnnotation() {
        return annotation;
    }

    /**
     * Setup dependencies between this node and the given node. Might not change anything, if
     * neither this node does not match the given node or vice versa.
     * @param node the other node to setup dependencies with
     */
    public Set<MOIDependency<T>> setupDependency(MOINode<T> node) {
        Set<MOIDependency<T>> dependencies = new HashSet<>();
        DependencyPattern dependency = moi.match(node.moi);
        if ( dependency != null ) {
            MOIDependency<T> moiDep = new MOIDependency<>(this, node, dependency);
            outgoing.add(moiDep);
            node.ingoing.add(moiDep);
            dependencies.add(moiDep);
        }

        // reverse
        dependency = node.moi.match(moi);
        if ( dependency != null ) {
            MOIDependency<T> moiDep = new MOIDependency<>(node, this, dependency);
            ingoing.add(moiDep);
            node.outgoing.add(moiDep);
            dependencies.add(moiDep);
        }

        return dependencies;
    }
}
