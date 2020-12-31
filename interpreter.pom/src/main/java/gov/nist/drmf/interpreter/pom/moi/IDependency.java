package gov.nist.drmf.interpreter.pom.moi;

/**
 * @author Andre Greiner-Petter
 */
public interface IDependency<T> {
    INode<T> getSource();

    INode<T> getSink();
}
