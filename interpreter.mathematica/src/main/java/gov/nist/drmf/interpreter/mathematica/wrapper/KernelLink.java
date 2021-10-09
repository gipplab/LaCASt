package gov.nist.drmf.interpreter.mathematica.wrapper;

public class KernelLink {
    com.wolfram.jlink.KernelLink kernelLink;

    public KernelLink(com.wolfram.jlink.KernelLink kernelLink) {
        this.kernelLink=kernelLink;
    }

    public void evaluate(String s) throws MathLinkException {
        try {
            kernelLink.evaluate(s);
        } catch (com.wolfram.jlink.MathLinkException e) {
            throw new MathLinkException(e);
        }
    }

    public void discardAnswer() throws MathLinkException {
        try {
            kernelLink.discardAnswer();
        } catch (com.wolfram.jlink.MathLinkException e) {
            throw new MathLinkException(e);
        }
    }

    public void clearError() {
        kernelLink.clearError();
    }

    public void newPacket() {
        kernelLink.newPacket();
    }

    public void waitForAnswer() throws MathLinkException {
        try {
            kernelLink.waitForAnswer();
        } catch (com.wolfram.jlink.MathLinkException e) {
            throw new MathLinkException(e);
        }
    }

    public Expr getExpr() throws MathLinkException {
        try {
            return new Expr(kernelLink.getExpr());
        } catch (com.wolfram.jlink.MathLinkException e) {
            throw new MathLinkException(e);
        }
    }

    public String evaluateToOutputForm(String fullf, int i) {
        return kernelLink.evaluateToOutputForm(fullf,i);
    }

    public void close() {
        kernelLink.close();
    }

    public boolean getBoolean() throws MathLinkException {
        try {
            return kernelLink.getBoolean();
        } catch (com.wolfram.jlink.MathLinkException e) {
           throw new MathLinkException(e);
        }
    }

    public void abortEvaluation() {
        kernelLink.abortEvaluation();
    }

    public Throwable getLastError() {
        return kernelLink.getLastError();
    }
}
