package gov.nist.drmf.interpreter.maple.wrapper;

import gov.nist.drmf.interpreter.maple.wrapper.openmaple.Algebraic;
import gov.nist.drmf.interpreter.maple.wrapper.openmaple.MString;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Andre Greiner-Petter
 */
public class MStringHelper extends OpenMapleWrapper {
    private static final Map<String, Method> methodRegister = new HashMap<>();

    MStringHelper(Object reference) {
        super(methodRegister, reference);
    }

    @Override
    protected Method getProxyMethod(String methodName) {
        return methodRegister.get(methodName);
    }

    static MString getMString(Object reference) {
        return (MString) Proxy.newProxyInstance(
                MStringHelper.class.getClassLoader(),
                new Class[]{ MString.class },
                new MStringHelper(reference)
        );
    }
}
