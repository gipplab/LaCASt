module interpreter.common {
    requires mlp;
    requires com.google.gson;

    requires org.apache.logging.log4j;

    requires org.junit.jupiter.api;
    requires org.junit.platform.commons;

    requires com.fasterxml.jackson.annotation;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.dataformat.yaml;
    requires org.hamcrest;

    exports gov.nist.drmf.interpreter.common;

    opens gov.nist.drmf.interpreter.common to org.junit.platform.commons;
    opens gov.nist.drmf.interpreter.common.replacements to org.junit.platform.commons, com.fasterxml.jackson.databind;
}