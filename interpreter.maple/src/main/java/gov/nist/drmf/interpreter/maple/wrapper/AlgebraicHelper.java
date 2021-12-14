package gov.nist.drmf.interpreter.maple.wrapper;

import gov.nist.drmf.interpreter.maple.wrapper.openmaple.Algebraic;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Andre Greiner-Petter
 */
public class AlgebraicHelper extends OpenMapleWrapper {
    private static final Map<String, Method> methodRegister = new HashMap<>();

    AlgebraicHelper(Object reference) {
        super(methodRegister, reference);
    }

    @Override
    protected Method getProxyMethod(String methodName) {
        return methodRegister.get(methodName);
    }

    static Algebraic getAlgebraic(Object reference) {
        return (Algebraic) Proxy.newProxyInstance(
                AlgebraicHelper.class.getClassLoader(),
                new Class[]{ Algebraic.class },
                new AlgebraicHelper(reference)
        );
    }
}
