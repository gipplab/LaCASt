package gov.nist.drmf.interpreter.pom.moi;

import mlp.ParseException;

public interface IMOIGraph {
    MOINode<Void> addNode(String id, String moi) throws ParseException;

    <T> MOINode<T>addNode(String id, String moi, T annotation) throws ParseException;

    MOINode<?> removeNode(String id);

    MOINode<?> getNode(String id);

    boolean containsNode(String id);
}
