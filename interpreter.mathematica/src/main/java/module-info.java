import gov.nist.drmf.interpreter.mathematica.common.AssumeMathematicaAvailabilityCondition;

module interpreter.mathematica {
    exports gov.nist.drmf.interpreter.mathematica;
    exports gov.nist.drmf.interpreter.mathematica.common;
    exports gov.nist.drmf.interpreter.mathematica.evaluate;

    provides org.junit.jupiter.api.extension.ExecutionCondition with AssumeMathematicaAvailabilityCondition;

    requires interpreter.common;
    requires transitive mathematica;
    requires org.junit.jupiter.api;
}