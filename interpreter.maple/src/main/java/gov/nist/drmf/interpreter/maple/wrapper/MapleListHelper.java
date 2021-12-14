package gov.nist.drmf.interpreter.maple.wrapper;

import gov.nist.drmf.interpreter.maple.wrapper.openmaple.MString;
import gov.nist.drmf.interpreter.maple.wrapper.openmaple.MapleList;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Andre Greiner-Petter
 */
public class MapleListHelper extends OpenMapleWrapper {
    private static final Map<String, Method> methodRegister = new HashMap<>();

    MapleListHelper(Object reference) {
        super(methodRegister, reference);
    }

    @Override
    protected Method getProxyMethod(String methodName) {
        return methodRegister.get(methodName);
    }

    static MapleList getMapleList(Object reference) {
        return (MapleList) Proxy.newProxyInstance(
                MapleListHelper.class.getClassLoader(),
                new Class[]{ MapleList.class },
                new MapleListHelper(reference)
        );
    }
}
