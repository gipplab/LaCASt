package gov.nist.drmf.interpreter.mlp.moi;

import gov.nist.drmf.interpreter.mlp.extensions.PrintablePomTaggedExpression;

import java.util.Collection;
import java.util.LinkedList;
import java.util.function.Predicate;

/**
 * Represents a node in an {@link MOIDependencyGraph}. A node is a single MOI which may have
 * ingoing and outgoing edges.
 *
 * @see MOIDependencyGraph
 * @see MOINode
 * @see DependencyPattern
 * @author Andre Greiner-Petter
 */
public class MOINode implements INode<MOIDependency> {

    private final String id;

    private final MathematicalObjectOfInterest moi;
    private final LinkedList<MOIDependency> ingoing;
    private final LinkedList<MOIDependency> outgoing;

    public MOINode(String id, MathematicalObjectOfInterest moi) {
        this.id = id;
        this.moi = moi;
        this.ingoing = new LinkedList<>();
        this.outgoing = new LinkedList<>();
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

    public void setupDependency(MOINode node) {
        DependencyPattern dependency = moi.match(node.moi);
        if ( dependency != null ) {
            MOIDependency moiDep = new MOIDependency(this, node, dependency);
            outgoing.add(moiDep);
            node.ingoing.add(moiDep);
        }

        // reverse
        dependency = node.moi.match(moi);
        if ( dependency != null ) {
            MOIDependency moiDep = new MOIDependency(node, this, dependency);
            ingoing.add(moiDep);
            node.outgoing.add(moiDep);
        }
    }
}
