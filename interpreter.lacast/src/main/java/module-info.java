module interpreter.lacast {
    exports gov.nist.drmf.interpreter.cas;
    exports gov.nist.drmf.interpreter.cas.translation;

    // allow testing
    opens gov.nist.drmf.interpreter.cas.logging to org.junit.platform.commons;
    opens gov.nist.drmf.interpreter.cas.blueprints to org.junit.platform.commons;
    opens gov.nist.drmf.interpreter.cas.translation to org.junit.platform.commons;
    opens gov.nist.drmf.interpreter.cas.translation.components to org.junit.platform.commons;

    // requirements (project internal)
    requires interpreter.common;

    // module specific
    requires xstream;
    requires commons.cli;
    requires org.apache.commons.io;
    requires org.apache.commons.text;
}