package gov.nist.drmf.interpreter.maple.wrapper;

import gov.nist.drmf.interpreter.common.cas.CASReflectionWrapper;
import gov.nist.drmf.interpreter.common.exceptions.CASUnavailableException;
import gov.nist.drmf.interpreter.maple.common.MapleConfig;
import gov.nist.drmf.interpreter.maple.wrapper.openmaple.Algebraic;
import gov.nist.drmf.interpreter.maple.wrapper.openmaple.EngineCallBacks;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.*;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Andre Greiner-Petter
 */
public abstract class OpenMapleWrapper extends CASReflectionWrapper {
    private static final Logger LOG = LogManager.getLogger(OpenMapleWrapper.class.getName());

    private static final Map<String, Class<?>> proxyClasses = new HashMap<>();

    private static ClassLoader classLoader = null;

    private final Object reference;

    OpenMapleWrapper(Map<String, Method> methodRegister, Object reference) {
        this.reference = reference;
        if (methodRegister.isEmpty() && reference != null)
            methodRegister.putAll(CASReflectionWrapper.registerMethods(reference));
    }

    @Override
    protected Object getProxyReference() {
        return reference;
    }

    /**
     * @return the classloader that loads classes from the OpenMaple lib
     */
    public static ClassLoader getClassLoader() {
        if (classLoader == null) {
            try {
                classLoader = CASReflectionWrapper.getClassLoader(
                        MapleConfig.getMapleJarPath(),
                        MapleConfig.getMapleExternalCallJarPath()
                );
            } catch (MalformedURLException e) {
                throw new CASUnavailableException("Unable to access Maple's interface OpenMaple library", e);
            }
        }
        return classLoader;
    }

    /**
     * The entry point hookup for OpenMaple is quite more complicated compared to Mathematica due to the event
     * listener that we want to implement ourself. The entry point for OpenMaple is the Engine class:
     * <pre>
     *      com.maplesoft.openmaple.Engine
     * </pre>
     * which has the constructor:
     * <pre>
     *      com.maplesoft.openmaple.Engine(
     *          java.lang.String[],
     *          com.maplesoft.openmaple.EngineCallBacks,
     *          java.lang.Object,
     *          java.lang.Object
     *      );
     * </pre>
     * <p>
     * This method assumes the arguments to construct an instance of the Engine object.
     * Since the EngineCallBacks interface is not available at runtime, the second argument must
     * be an instance of the gov.nist.drmf.interpreter.maple.wrapper.openmaple.EngineCallBacks interface.
     *
     * @param arguments the arguments for the com.maplesoft.openmaple.Engine constructor
     * @return com.maplesoft.openmaple.Engine
     * @throws CASUnavailableException if Maple (i.e., OpenMaple) is not available or the structure of the
     *                                 OpenMaple interface has changed.
     */
    @Override
    protected Object getEntryPointInstance(Object... arguments) throws CASUnavailableException {
        assert (arguments != null && arguments.length == 4);

        if (!(arguments[1] instanceof EngineCallBacks && arguments[1] instanceof InvocationHandler))
            throw new IllegalArgumentException(
                    "The second argument must be an implementation of our local EngineCallBacks interface and " +
                            "an invocation handler."
            );

        try {
            ClassLoader classLoader = getClassLoader();

            // first, load required classes
            Class<?> engineCallbacksInterfaceClazz = classLoader.loadClass("com.maplesoft.openmaple.EngineCallBacks");
            Class<?> engineClazz = classLoader.loadClass("com.maplesoft.openmaple.Engine");

            // make a proxy of the second argument
            Object engineCallBacksProxyImpl = getMapleEngineCallBackImpl(
                    engineCallbacksInterfaceClazz,
                    (InvocationHandler) arguments[1]
            );

            // get the constructor
            Constructor<?> engineConstructor = engineClazz.getConstructor(String[].class, engineCallbacksInterfaceClazz, Object.class, Object.class);
            //noinspection JavaReflectionInvocation
            Object engineObject = engineConstructor.newInstance(arguments[0], engineCallBacksProxyImpl, arguments[2], arguments[3]);

            if (proxyClasses.isEmpty()) {
                proxyClasses.put("Algebraic", classLoader.loadClass("com.maplesoft.openmaple.Algebraic"));
                proxyClasses.put("Numeric", classLoader.loadClass("com.maplesoft.openmaple.Numeric"));
                proxyClasses.put("MString", classLoader.loadClass("com.maplesoft.openmaple.MString"));
                proxyClasses.put("List", classLoader.loadClass("com.maplesoft.openmaple.List"));
            }

            return engineObject;
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException |
                InvocationTargetException | InstantiationException e) {
            LOG.warn("Unable to access OpenMaple library and load necessary classes. " +
                    "Either Maple is not available or the OpenMaple interface has changed." + e.getMessage());
            throw new CASUnavailableException("Unable to access the Maple's interface OpenMaple. " +
                    "Either Maple is not available or the OpenMaple interface has changed.", e);
        }
    }

    /**
     * This method makes a proxy implementation of the com.maplesoft.openmaple.EngineCallBacks interface.
     * The returns object is a proxy in which every call on the EngineCallBacks interface are proxied to the
     * given invocation handler instance. Use this method to create an object for the second argument of the
     * {@link #getEntryPointInstance(Object...)} method, which must be an instance of the
     * com.maplesoft.openmaple.EngineCallBacks interface.
     *
     * @param engineCallbackClazz class object of the com.maplesoft.openmaple.EngineCallBacks interface
     * @param invocationHandler   an implementation for the com.maplesoft.openmaple.EngineCallBacks interface
     * @return a proxied EngineCallBacks interface where every method is routed to the given invocation handler implementation
     */
    private Object getMapleEngineCallBackImpl(Class<?> engineCallbackClazz, InvocationHandler invocationHandler) {
        return Proxy.newProxyInstance(
                getClassLoader(),
                new Class[]{engineCallbackClazz},
                invocationHandler
        );
    }

    /**
     * Might be little confusing but the proxy object is our interface not the actual implementation.
     * Even more confusing, the given method is the method from our interface too! Hence invoking
     * the given method on the given proxy: method.invoke(proxy, args)
     * is not gonna work since we would try to invoke an interface method that does not have any real
     * implementation behind it. So what we need to do is to ignore the proxy object (which is just our
     * interface), map the given method (our interface method) to the actual implementation in OpenMaple
     * and invoke that instead.
     *
     * @param proxy  an auto generated instance of our interface which does not do anything
     * @param method a method from our interface which should exist in OpenMaple too
     * @param args   the arguments to invoke the method on the OpenMaple implementation of our interface
     * @return the result as an object.  The result is automatically casted to our local interfaces (not the
     * actual OpenMaple interfaces).
     * @throws MapleException          if OpenMaple throws an internal exception
     * @throws CASUnavailableException if the method/proxy implementation does not exist because Mathematica is not
     *                                 available in the classpath
     */
    @Override
    public Object invoke(Object proxy, Method method, Object... args)
            throws MapleException, CASUnavailableException {
        try {
            Method exprImplMethod = getProxyMethod(CASReflectionWrapper.getQualifiedMethodID(method));
            Object returnVal = exprImplMethod.invoke(getProxyReference(), args);
            return castExpression(returnVal);
        } catch (InvocationTargetException | IllegalAccessException e) {
            LOG.error("Invoking OpenMaple method '" + method.getName() + "' threw an exception.");
            Throwable throwable = e.getCause();
            if (throwable != null && throwable.getClass().getSimpleName().matches("MapleException"))
                throw new MapleException(throwable);
            else throw new CASUnavailableException("Unable to communicate via Maple's interface OpenMaple", e);
        }
    }

    private Object castExpression(Object openMapleObject) {
        // careful, the order below matters because the other classes
        // are actually subclasses of Algebraic. So check the subclasses
        // first and Algebraic last.
        Class<?> numericClass = proxyClasses.get("Numeric");
        if (numericClass.isInstance(openMapleObject)) {
            return NumericHelper.getNumeric(openMapleObject);
        }

        Class<?> mStringClass = proxyClasses.get("MString");
        if (mStringClass.isInstance(openMapleObject)) {
            return MStringHelper.getMString(openMapleObject);
        }

        Class<?> listClass = proxyClasses.get("List");
        if (listClass.isInstance(openMapleObject)) {
            return MapleListHelper.getMapleList(openMapleObject);
        }

        Class<?> algebraicClass = proxyClasses.get("Algebraic");
        if (algebraicClass.isInstance(openMapleObject)) {
            return AlgebraicHelper.getAlgebraic(openMapleObject);
        }

        if (openMapleObject instanceof List) {
            return (List<Algebraic>) openMapleObject;
//            if (list.isEmpty()) return list;
//
//            Object elementObj = list.get(0);
//            if (algebraicClass.isInstance(elementObj)) {
//                List<Algebraic> newList = new LinkedList<>();
//                for ( Object obj : list ) {
//                    newList.add( (Algebraic) castExpression(obj) );
//                }
//                return newList;
//            }
//            return list;
        }

        return openMapleObject;
    }
}
