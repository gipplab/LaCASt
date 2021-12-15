package gov.nist.drmf.interpreter.mathematica.wrapper.jlink;

import gov.nist.drmf.interpreter.mathematica.wrapper.MathLinkException;

/**
 * An interface of J/Links com.wolfram.jlink.KernelLink.
 * This interface is just used to provide convenient access to the methods we use. The actual KernelLink
 * interface (and final implementation) contains many more methods but we do not need to call them directly.
 *
 * We use Java's internal proxy mechanism to map the implementation of this interface to the dynamically loaded
 * class from J/Link.
 *
 * @author Andre Greiner-Petter
 * @see gov.nist.drmf.interpreter.mathematica.wrapper.JLinkWrapper
 */
public interface KernelLink {
    void evaluate(String s) throws MathLinkException;

    void discardAnswer() throws MathLinkException;

    void clearError();

    void newPacket();

    void waitForAnswer();

    Expr getExpr() throws MathLinkException;

    String evaluateToOutputForm(String str, int i);

    void close();

    boolean getBoolean() throws MathLinkException;

    void abortEvaluation();

    Throwable getLastError();
}
