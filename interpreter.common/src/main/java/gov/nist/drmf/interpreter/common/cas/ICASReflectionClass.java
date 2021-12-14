package gov.nist.drmf.interpreter.common.cas;

public interface ICASReflectionClass {
    String getClassName();

    ICASReflectionMethod[] getMethodSpecs();
}
