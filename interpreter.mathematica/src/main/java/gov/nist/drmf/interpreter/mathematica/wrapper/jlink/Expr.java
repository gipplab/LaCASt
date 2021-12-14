package gov.nist.drmf.interpreter.mathematica.wrapper.jlink;

import gov.nist.drmf.interpreter.mathematica.wrapper.ExprFormatException;

public interface Expr {
    Expr[] args();

    int length();

    boolean trueQ();

    boolean numberQ();

    double asDouble() throws ExprFormatException;

    Expr head();

    boolean listQ();

    String asString() throws ExprFormatException;
}
