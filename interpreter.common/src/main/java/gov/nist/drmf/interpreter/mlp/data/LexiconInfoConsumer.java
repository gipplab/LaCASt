package gov.nist.drmf.interpreter.mlp.data;

import gov.nist.drmf.interpreter.mlp.LineAnalyzer;

import java.util.function.Consumer;

public interface LexiconInfoConsumer extends Consumer<String[]> {
    void setLineAnalyzer(LineAnalyzer lineAnalyzer);
}
