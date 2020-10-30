package gov.nist.drmf.interpreter.mlp.moi;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.function.Function;

/**
 * Represents a node in an {@link MOIDependencyGraph}. A node is a single MOI which may have
 * ingoing and outgoing edges. A node can be annotated with an extra object or a list.
 *
 * @see MOIDependencyGraph
 * @see MOINode
 * @see DependencyPattern
 * @author Andre Greiner-Petter
 */
public class MOINode<T> implements INode<MOIDependency> {
    // the id of the node
    private final String id;

    // the actual MOI element
    private final MathematicalObjectOfInterest moi;

    // the ingoing and outgoing nodes
    private final LinkedList<MOIDependency> ingoing;
    private final LinkedList<MOIDependency> outgoing;

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

    /**
     * This converts an annotated node to another annotated node by the given annotation converter.
     * @param reference the reference node that should be copied
     * @param annotationConverter the annotation converter to convert the old annotation to the new
     * @param <S> the new annotation class
     */
    public <S> MOINode(MOINode<S> reference, Function<S, T> annotationConverter) {
        this.id = reference.id;
        this.moi = reference.moi;
        this.ingoing = new LinkedList<>(reference.ingoing);
        this.outgoing = new LinkedList<>(reference.outgoing);
        this.annotation = annotationConverter.apply(reference.annotation);
    }

    public String getId() {
        return id;
    }

    public MathematicalObjectOfInterest getNode() {
        return moi;
    }

    @Override
    public Collection<MOIDependency> getIngoingDependencies() {
        return ingoing;
    }

    @Override
    public Collection<MOIDependency> getOutgoingDependencies() {
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
    public Set<MOIDependency> setupDependency(MOINode<?> node) {
        Set<MOIDependency> dependencies = new HashSet<>();
        DependencyPattern dependency = moi.match(node.moi);
        if ( dependency != null ) {
            MOIDependency moiDep = new MOIDependency(this, node, dependency);
            outgoing.add(moiDep);
            node.ingoing.add(moiDep);
            dependencies.add(moiDep);
        }

        // reverse
        dependency = node.moi.match(moi);
        if ( dependency != null ) {
            MOIDependency moiDep = new MOIDependency(node, this, dependency);
            ingoing.add(moiDep);
            node.outgoing.add(moiDep);
            dependencies.add(moiDep);
        }

        return dependencies;
    }
}
