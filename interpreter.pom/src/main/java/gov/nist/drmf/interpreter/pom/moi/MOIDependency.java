package gov.nist.drmf.interpreter.pom.moi;

/**
 * Represents the directed edge in an {@link MOIDependencyGraph} with the
 * attribute {@link DependencyPattern}.
 *
 * @see MOIDependencyGraph
 * @see MOINode
 * @see DependencyPattern
 * @author Andre Greiner-Petter
 */
public class MOIDependency<T> {

    private final MOINode<T> source;
    private final MOINode<T> sink;
    private DependencyPattern attribute;

    /**
     * Keep Kryo happy for serialization
     */
    private MOIDependency() {
        this(null, null, null);
    }

    public MOIDependency(MOINode<T> source, MOINode<T> sink) {
        this.source = source;
        this.sink = sink;
    }

    public MOIDependency(MOINode<T> source, MOINode<T> sink, DependencyPattern attribute) {
        this(source, sink);
        this.attribute = attribute;
    }

    public MOINode<T> getSource() {
        return source;
    }

    public MOINode<T> getSink() {
        return sink;
    }

    /**
     * Could be null!
     * @return the dependency pattern that matched
     */
    public DependencyPattern getAttribute() {
        return attribute;
    }
}
