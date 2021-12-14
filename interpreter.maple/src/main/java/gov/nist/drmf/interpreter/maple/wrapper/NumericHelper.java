package gov.nist.drmf.interpreter.maple.wrapper;

import gov.nist.drmf.interpreter.maple.wrapper.openmaple.Numeric;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Andre Greiner-Petter
 */
public class NumericHelper extends OpenMapleWrapper {
    private static final Map<String, Method> methodRegister = new HashMap<>();

    NumericHelper(Object reference) {
        super(methodRegister, reference);
    }

    @Override
    protected Method getProxyMethod(String methodName) {
        return methodRegister.get(methodName);
    }

    static Numeric getNumeric(Object reference) {
        return (Numeric) Proxy.newProxyInstance(
                NumericHelper.class.getClassLoader(),
                new Class[]{ Numeric.class },
                new NumericHelper(reference)
        );
    }
}
