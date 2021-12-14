package gov.nist.drmf.interpreter.mathematica.wrapper;

import gov.nist.drmf.interpreter.mathematica.wrapper.jlink.Expr;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

public class ExprHelper extends JLinkWrapperHelper {
    private static final Map<String, Method> methodRegister = new HashMap<>();

    ExprHelper(Object reference) {
        super(methodRegister, reference);
    }

    @Override
    protected Method getProxyMethod(String methodName) {
        return methodRegister.get(methodName);
    }

    public static Expr getExpr(Object expr) {
        return (Expr) Proxy.newProxyInstance(
                ExprHelper.class.getClassLoader(),
                new Class[]{ Expr.class },
                new ExprHelper(expr)
        );
    }
}
