import gov.nist.drmf.interpreter.maple.setup.AssumeMapleAvailabilityCondition;

module interpreter.maple {
    exports gov.nist.drmf.interpreter.maple;
    exports gov.nist.drmf.interpreter.maple.setup;
    exports gov.nist.drmf.interpreter.maple.translation;
    exports gov.nist.drmf.interpreter.maple.listener;
    exports gov.nist.drmf.interpreter.maple.common;

    provides org.junit.jupiter.api.extension.ExecutionCondition with AssumeMapleAvailabilityCondition;

    opens gov.nist.drmf.interpreter.maple.translation to org.junit.platform.commons;
    exports gov.nist.drmf.interpreter.maple.grammar.lexicon to org.junit.platform.commons;

    requires interpreter.common;
    requires transitive maple;
    requires transitive maple.call;
    requires org.junit.jupiter.api;
}