package gov.nist.drmf.interpreter.mlp.moi;

/**
 * Represents the directed edge in an {@link MOIDependencyGraph} with the
 * attribute {@link DependencyPattern}.
 *
 * @see MOIDependencyGraph
 * @see MOINode
 * @see DependencyPattern
 * @author Andre Greiner-Petter
 */
public class MOIDependency {

    private final MOINode source;
    private final MOINode sink;
    private final DependencyPattern attribute;

    public MOIDependency(MOINode source, MOINode sink, DependencyPattern attribute) {
        this.source = source;
        this.sink = sink;
        this.attribute = attribute;
    }

    public MOINode getSource() {
        return source;
    }

    public MOINode getSink() {
        return sink;
    }

    public DependencyPattern getAttribute() {
        return attribute;
    }
}
