package gov.nist.drmf.interpreter.common.interfaces;

public interface TranslationFeature<T> {
    T preProcess(T obj);
}
