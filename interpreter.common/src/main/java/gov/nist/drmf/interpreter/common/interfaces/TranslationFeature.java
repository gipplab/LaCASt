package gov.nist.drmf.interpreter.common.interfaces;

public interface TranslationFeature<T> {
    T preProcess(T obj);

    static <T> TranslationFeature<T> combine(TranslationFeature<T>... features) {
        return obj -> {
            for ( TranslationFeature<T> feature : features ) {
                obj = feature.preProcess(obj);
            }
            return obj;
        };
    }
}
