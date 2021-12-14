package gov.nist.drmf.interpreter.maple.wrapper;

import gov.nist.drmf.interpreter.maple.wrapper.openmaple.Engine;
import gov.nist.drmf.interpreter.maple.wrapper.openmaple.EngineCallBacks;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Andre Greiner-Petter
 */
public class EngineHelper extends OpenMapleWrapper {
    private static final Map<String, Method> methodRegister = new HashMap<>();

    EngineHelper(Object reference) {
        super(methodRegister, reference);
    }

    @Override
    protected Method getProxyMethod(String methodName) {
        return methodRegister.get(methodName);
    }

    /**
     * This method mimics OpenMaple's com.maplesoft.openmaple.Engine constructor.
     * @param args the arguments for com.maplesoft.openmaple.Engine
     * @param callBack the event listener for com.maplesoft.openmaple.Engine
     * @param data optional data
     * @param info optional info
     * @return a proxy of the Engine
     */
    static Engine getNewEngine(String[] args, EngineCallBacks callBack, Object data, Object info) {
        EngineHelper engineHelper = new EngineHelper(null);
        Object engineObj = engineHelper.getEntryPointInstance(args, callBack, data, info);
        return (Engine) Proxy.newProxyInstance(
                EngineHelper.class.getClassLoader(),
                new Class[]{ Engine.class },
                new EngineHelper(engineObj)
        );
    }
}