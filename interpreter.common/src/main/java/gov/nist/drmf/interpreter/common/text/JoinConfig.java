package gov.nist.drmf.interpreter.common.text;

import java.util.function.Function;

/**
 * @author Andre Greiner-Petter
 */
public class JoinConfig<V> {

    private final String joiner;

    private Iterable<V> iter;

    private final Function<V, String> mapper;

    private int max = -1;

    private String maxMessage = "";

    public JoinConfig(String joiner, Iterable<V> iter, Function<V, String> mapper) {
        this.joiner = joiner;
        this.iter = iter;
        this.mapper = mapper;
    }

    public JoinConfig<V> setMax(int max) {
        this.max = max;
        return this;
    }

    public JoinConfig<V> setMaxMessage(String maxMessage) {
        this.maxMessage = maxMessage;
        return this;
    }

    public JoinConfig<V> setIter(Iterable<V> iter) {
        this.iter = iter;
        return this;
    }

    public String getJoiner() {
        return joiner;
    }

    public Iterable<V> getIter() {
        return iter;
    }

    public Function<V, String> getMapper() {
        return mapper;
    }

    public int getMax() {
        return max;
    }

    public String getMaxMessage() {
        return maxMessage;
    }
}
