package gov.nist.drmf.interpreter.mathematica.wrapper;

public class MathLinkFactory {
    public static KernelLink createKernelLink(String[] args) throws MathLinkException {
        try {
            return new KernelLink(com.wolfram.jlink.MathLinkFactory.createKernelLink(args));
        } catch (com.wolfram.jlink.MathLinkException e) {
            throw new gov.nist.drmf.interpreter.mathematica.wrapper.MathLinkException(e);
        }
    }
}
