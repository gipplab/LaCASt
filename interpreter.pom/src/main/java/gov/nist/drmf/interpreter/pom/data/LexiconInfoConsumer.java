package gov.nist.drmf.interpreter.pom.data;

import gov.nist.drmf.interpreter.pom.LineAnalyzer;

import java.util.function.Consumer;

public interface LexiconInfoConsumer extends Consumer<String[]> {
    void setLineAnalyzer(LineAnalyzer lineAnalyzer);
}
