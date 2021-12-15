package gov.nist.drmf.interpreter.common.cas;

public interface ICASReflectionMethod {
    String getMethodID();

    Class<?>[] getArguments();
}
