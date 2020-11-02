package gov.nist.drmf.interpreter.pom.moi;

import mlp.ParseException;

public interface IMOIGraph<T> {
    MOINode<T> addNode(String id, String moi) throws ParseException;

    MOINode<T> addNode(String id, String moi, T annotation) throws ParseException;

    MOINode<T> removeNode(String id);

    MOINode<T> getNode(String id);

    boolean containsNode(String id);
}
