import gov.nist.drmf.interpreter.common.tests.AssumeMLPAvailabilityCondition;

module interpreter.common {
    // exports common package
    exports gov.nist.drmf.interpreter.common;
    exports gov.nist.drmf.interpreter.common.constants;
    exports gov.nist.drmf.interpreter.common.exceptions;
    exports gov.nist.drmf.interpreter.common.grammar;
    exports gov.nist.drmf.interpreter.common.meta;
    exports gov.nist.drmf.interpreter.common.replacements;
    exports gov.nist.drmf.interpreter.common.symbols;
    exports gov.nist.drmf.interpreter.common.tests;

    exports gov.nist.drmf.interpreter.mlp;
    exports gov.nist.drmf.interpreter.mlp.extensions;

    provides org.junit.jupiter.api.extension.ExecutionCondition with AssumeMLPAvailabilityCondition;

    // allow reflection for jackson and for tests
    opens gov.nist.drmf.interpreter.common.replacements to org.junit.platform.commons, com.fasterxml.jackson.databind;
    opens gov.nist.drmf.interpreter.common to org.junit.platform.commons;

    // requirements (internal)
    requires transitive mlp;

    // requirements general
    requires transitive org.apache.logging.log4j;

    // module specific requirements
    requires com.google.gson;
    requires com.fasterxml.jackson.annotation;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.dataformat.yaml;

    // packages to extend test cases
    requires org.hamcrest;
    requires org.junit.jupiter.api;
}