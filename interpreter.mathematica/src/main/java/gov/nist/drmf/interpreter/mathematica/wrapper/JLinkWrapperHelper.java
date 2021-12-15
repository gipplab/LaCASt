package gov.nist.drmf.interpreter.mathematica.wrapper;

import gov.nist.drmf.interpreter.common.cas.CASReflectionWrapper;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * @author Andre Greiner-Petter
 */
public abstract class JLinkWrapperHelper extends JLinkWrapper {
    private final Object reference;

    JLinkWrapperHelper(Map<String, Method> methodRegister, Object reference) {
        this.reference = reference;
        if ( methodRegister.isEmpty() )
            methodRegister.putAll(CASReflectionWrapper.registerMethods(reference));
    }

    @Override
    protected Object getProxyReference() {
        return reference;
    }
}
