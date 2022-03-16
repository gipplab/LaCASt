package gov.nist.drmf.interpreter.mathematica.wrapper;

import gov.nist.drmf.interpreter.common.cas.CASReflectionWrapper;
import gov.nist.drmf.interpreter.common.exceptions.CASUnavailableException;
import gov.nist.drmf.interpreter.mathematica.config.MathematicaConfig;
import gov.nist.drmf.interpreter.mathematica.wrapper.jlink.Expr;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

/**
 * This class is the base for loading the J/Link library dynamically (at runtime). The main function is the
 * {@link #getEntryPointInstance(Object...)} which hooks up a class loader instance to load the MathLinkFactory class
 * and calls the createKernelLink. This can be considered the J/Link entrypoint. Since every other class will be
 * derived through the resulted KernelLink object, all classes uses our new class loader (bootstrapping). Hence,
 * we do not need to write a full custom class loader and specify it on startup.
 *
 * @author Andre Greiner-Petter
 */
public abstract class JLinkWrapper extends CASReflectionWrapper {
    private static final Logger LOG = LogManager.getLogger(JLinkWrapper.class.getName());

    private static final String entryPointClass = "com.wolfram.jlink.MathLinkFactory";
    private static final String entryPointMethod = "createKernelLink";
    private static final Class<?> entryPointMethodArguments = String[].class;

    private static final Map<String, Class<?>> proxyClasses = new HashMap<>();

    private static ClassLoader classLoader = null;

    /**
     * @return the classloader that loads classes from the J/Link library
     */
    public static ClassLoader getClassLoader() {
        if ( classLoader == null ) {
            try {
                classLoader = CASReflectionWrapper.getClassLoader( MathematicaConfig.getJLinkJarPath() );
            } catch (MalformedURLException e) {
                throw new CASUnavailableException("Unable to access Mathematica's interface J/Link library", e);
            }
        }
        return classLoader;
    }

    /**
     * Loads the J/Link entrypoint com.wolfram.jlink.MathLinkFactory and invokes createKernelLink
     * with the given arguments and returns the proxied KernelLink instance.
     * @param arguments the arguments that are passed through to the createKernelLink method in MathLinkFactory
     * @return an instance of the KernelLink object that will be returned by createKernelLink
     * @throws CASUnavailableException in case we could not load the desired J/Link entrypoint.
     */
    @Override
    protected Object getEntryPointInstance(Object... arguments) throws CASUnavailableException {
        try {
            ClassLoader classLoader = getClassLoader();
            Class<?> clazz = classLoader.loadClass(entryPointClass);
            Method method = clazz.getMethod(entryPointMethod, entryPointMethodArguments);
            //noinspection JavaReflectionInvocation
            Object returnVal = method.invoke(clazz, arguments);

            if ( proxyClasses.isEmpty() ) {
                proxyClasses.put("Expr", classLoader.loadClass("com.wolfram.jlink.Expr"));
                proxyClasses.put("KernelLink", classLoader.loadClass("com.wolfram.jlink.KernelLink"));
            }

            return returnVal;
        } catch (ClassNotFoundException | NoSuchMethodException |
                IllegalAccessException | InvocationTargetException e) {
            LOG.warn("Unable to access J/Link library and load necessary classes; " + e.getMessage());
            throw new CASUnavailableException("Unable to access Mathematica's interface J/Link library", e);
        }
    }

    /**
     * Might be little confusing but the proxy object is our interface not the actual implementation.
     * Even more confusing, the given method is the method from our interface too! Hence invoking
     * the given method on the given proxy: method.invoke(proxy, args)
     * is not gonna work since we would try to invoke an interface method that does not have any real
     * implementation behind it. So what we need to do is to ignore the proxy object (which is just our
     * interface), map the given method (our interface method) to the actual implementation in J/Link
     * and invoke that instead.
     *
     * @param proxy an auto generated instance of our interface which does not do anything
     * @param method a method from our interface which should exist in J/Link too
     * @param args the arguments to invoke the method on the J/Link implementation of our interface
     * @return the result as an object. The result is automatically casted to our local interfaces (not the
     *          actual J/Link interfaces).
     * @throws ExprFormatException if J/Link throws an ExprFormatException
     * @throws MathLinkException if J/Link throws an ExprFormatException
     * @throws CASUnavailableException if the method/proxy implementation does not exist because Mathematica is not
     *                                  available in the classpath
     */
    @Override
    public Object invoke(Object proxy, Method method, Object... args)
            throws ExprFormatException, MathLinkException, CASUnavailableException {
        try {
            Method exprImplMethod = getProxyMethod(CASReflectionWrapper.getQualifiedMethodID(method));
            Object returnVal = exprImplMethod.invoke(getProxyReference(), args);
            return castExpression(returnVal);
        } catch (InvocationTargetException | IllegalAccessException e) {
            LOG.error("Invoking J/Link method '" + method.getName() + "' threw an exception.");
            Throwable throwable = e.getCause();
            if (throwable != null && throwable.getClass().getSimpleName().matches("ExprFormatException"))
                throw new ExprFormatException(throwable);
            else if (throwable != null && throwable.getClass().getSimpleName().matches("MathLinkException"))
                throw formMathLinkException(throwable);
            else throw new CASUnavailableException("Unable to communicate via Mathematica's interface J/Link", e);
        }
    }

    private Object castExpression(Object jLinkObject) {
        Class<?> jLinkExprClass = proxyClasses.get("Expr");
        if ( jLinkExprClass.isInstance(jLinkObject) ) {
            return ExprHelper.getExpr(jLinkObject);
        } else if ( jLinkObject != null && jLinkObject.getClass().isArray() ) {
            Object[] arr = (Object[]) jLinkObject;
            Expr[] castedArr = new Expr[arr.length];
            for ( int i = 0; i < arr.length; i++ ) {
                castedArr[i] = ExprHelper.getExpr(arr[i]);
            }
            return castedArr;
        } else return jLinkObject;
    }

    private MathLinkException formMathLinkException(Throwable cause) {
        try {
            Method method = cause.getClass().getMethod("getErrCode");
            int errCode = (int) method.invoke(cause);
            return new MathLinkException(errCode, cause);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            return new MathLinkException(cause);
        }
    }
}
