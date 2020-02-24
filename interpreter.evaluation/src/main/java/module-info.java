module interpreter.evaluation {
    requires interpreter.common;
    requires interpreter.lacast;
    requires interpreter.maple;
    requires interpreter.mathematica;

    exports gov.nist.drmf.interpreter.evaluation;
    exports gov.nist.drmf.interpreter.evaluation.common;
    exports gov.nist.drmf.interpreter.evaluation.console;
    exports gov.nist.drmf.interpreter.evaluation.core;
    exports gov.nist.drmf.interpreter.evaluation.constraints;

    opens gov.nist.drmf.interpreter.evaluation to org.junit.platform.commons;
    opens gov.nist.drmf.interpreter.evaluation.constraints to org.junit.platform.commons;
}