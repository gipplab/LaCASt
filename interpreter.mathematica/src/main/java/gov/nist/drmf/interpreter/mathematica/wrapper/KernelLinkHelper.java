package gov.nist.drmf.interpreter.mathematica.wrapper;

import gov.nist.drmf.interpreter.mathematica.wrapper.jlink.KernelLink;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Andre Greiner-Petter
 */
public class KernelLinkHelper extends JLinkWrapperHelper {
    private static final Map<String, Method> methodRegister = new HashMap<>();

    KernelLinkHelper(Object reference) {
        super(methodRegister, reference);
    }

    @Override
    protected Method getProxyMethod(String methodName) {
        return methodRegister.get(methodName);
    }

    public static KernelLink getKernelLink(Object kernelLink) {
        return (KernelLink) Proxy.newProxyInstance(
                ExprHelper.class.getClassLoader(),
                new Class[]{ KernelLink.class },
                new KernelLinkHelper(kernelLink)
        );
    }
}
