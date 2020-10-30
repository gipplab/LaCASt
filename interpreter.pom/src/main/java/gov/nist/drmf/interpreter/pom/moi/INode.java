package gov.nist.drmf.interpreter.pom.moi;

import java.util.Collection;

public interface INode<T> {

    Collection<T> getIngoingDependencies();

    Collection<T> getOutgoingDependencies();

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
