package gov.nist.drmf.interpreter.mathematica.wrapper;

public interface IJLinkMethod {
    String getMethodID();

    Class<?>[] getArguments();
}
