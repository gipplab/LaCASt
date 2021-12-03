package gov.nist.drmf.interpreter.mathematica.wrapper;

public interface IJLinkClass {
    String getJLinkClassName();

    IJLinkMethod[] getMethodSpecs();
}
