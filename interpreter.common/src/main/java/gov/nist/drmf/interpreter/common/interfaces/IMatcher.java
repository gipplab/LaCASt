package gov.nist.drmf.interpreter.common.interfaces;

public interface IMatcher<T> {
    /**
     * Returns true if the given {@param expression} matches
     * this object. False otherwise.
     * @param expression that may or may not match this object.
     * @return true if the given expression matches this object.
     */
    boolean match(T expression);
}
