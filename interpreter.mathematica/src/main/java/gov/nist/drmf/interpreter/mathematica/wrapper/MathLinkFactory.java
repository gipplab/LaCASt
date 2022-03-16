package gov.nist.drmf.interpreter.mathematica.wrapper;

import gov.nist.drmf.interpreter.mathematica.wrapper.jlink.KernelLink;

import java.lang.reflect.Method;

public class MathLinkFactory extends JLinkWrapper {

    private static final MathLinkFactory instance = new MathLinkFactory();
    
    private MathLinkFactory() {}

    public static KernelLink createKernelLink(String[] args) throws MathLinkException {
        return KernelLinkHelper.getKernelLink(instance.getEntryPointInstance((Object) args));
    }

    @Override
    protected Method getProxyMethod(String methodName) {
        throw new IllegalCallerException("MathLinkFactory only supports the createKernelLink method");
    }

    @Override
    protected Object getProxyReference() {
        throw new IllegalCallerException("MathLinkFactory only supports the createKernelLink method");
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
        throw new IllegalCallerException("MathLinkFactory only supports the createKernelLink method");
    }
}
