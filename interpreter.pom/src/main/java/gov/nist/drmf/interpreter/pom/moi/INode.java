package gov.nist.drmf.interpreter.pom.moi;

import java.util.Collection;
import java.util.stream.Collectors;

public interface INode<T> {
    default Collection<INode<T>> getIngoingNodes() {
        return getIngoingDependencies().stream().map(IDependency::getSource).collect(Collectors.toList());
    }

    default Collection<INode<T>> getOutgoingNodes() {
        return getOutgoingDependencies().stream().map(IDependency::getSink).collect(Collectors.toList());
    }

    Collection<? extends IDependency<T>> getIngoingDependencies();

    Collection<? extends IDependency<T>> getOutgoingDependencies();

    default boolean isSource() {
        return getIngoingDependencies().isEmpty();
    }

    default boolean isSink() {
        return getOutgoingDependencies().isEmpty();
    }

    default boolean isIsolated() {
        return getIngoingDependencies().isEmpty() && getOutgoingDependencies().isEmpty();
    }
}
