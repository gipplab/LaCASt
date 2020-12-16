package gov.nist.drmf.interpreter.common.interfaces;

/**
 * @author Andre Greiner-Petter
 */
@FunctionalInterface
public interface IMapStringFunction<T> {
    String get(T object);
}
