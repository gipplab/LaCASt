package gov.nist.drmf.interpreter.mathematica.wrapper;

import gov.nist.drmf.interpreter.common.exceptions.CASUnavailableException;
import gov.nist.drmf.interpreter.mathematica.config.MathematicaConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.reflections.Reflections;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * This class wraps J/Link. It must be initiated prior invoking methods via the {@link #init()} method.
 * This class is not self-initializing to provide more control over a good timing.
 *
 * @author Andre Greiner-Petter
 */
public class JLinkWrapper {
    private static final Logger LOG = LogManager.getLogger(JLinkWrapper.class.getName());

    // the single instance of this class. This is necessary to avoid loading classes multiple times
    private static final JLinkWrapper instance = new JLinkWrapper();

    // The register that stores methods that can be invoked
    private final Map<String, Method> methodRegister;

    // avoid calling init multiple times
    private boolean initiated;

    /**
     * Use {@link #getInstance()}
     */
    private JLinkWrapper() {
        methodRegister = new HashMap<>();
        this.initiated = false;
    }

    /**
     * Loads J/Link classes and methods via reflections.
     * @throws MalformedURLException if the JLink.jar path (from {@link MathematicaConfig#getJLinkJarPath()}) is invalid
     * @throws ClassNotFoundException if one of the classes we expect to see in J/Link does not exist anymore
     * @throws NoSuchMethodException if one of the methods we expect to see in J/Link does not exist anymore
     * @throws IllegalAccessException if the VM security system does not allow to access an instance of our wrapper classes
     * @throws InvocationTargetException if we are unable to construct one of our wrapper classes
     * @throws InstantiationException if we are unable to construct (instantiate) one of our wrapper classes
     */
    void init() throws MalformedURLException, ClassNotFoundException, NoSuchMethodException,
            IllegalAccessException, InvocationTargetException, InstantiationException {
        if ( initiated ) {
            LOG.debug("Avoid multiple loading cycles for J/Link lib. This should be avoided programmatically");
            return;
        }

        LOG.info("Try to load J/Link jar");
        Path jLinkJar = MathematicaConfig.getJLinkJarPath();
        ClassLoader classLoader = URLClassLoader.newInstance(
                new URL[] {jLinkJar.toUri().toURL()},
                getClass().getClassLoader() // we may want to use the system classloader here?
        );

        LOG.info("Load classes and methods from J/Link");
        // get all classes that implement the IJLinkClass interface
        Reflections reflections = new Reflections("gov.nist.drmf.interpreter.mathematica.wrapper");
        Set<Class<? extends IJLinkClass>> interfaces = reflections.getSubTypesOf(IJLinkClass.class);

        assert(interfaces.size() > 0);
        for ( Class<? extends IJLinkClass> jLinkInterface : interfaces ) {
            // get an instance of those classes in order to invoke the interface methods
            IJLinkClass jLinkClass = jLinkInterface.getDeclaredConstructor().newInstance();
            // load specific classes from the J/Link library
            Class<?> clazz = classLoader.loadClass(jLinkClass.getJLinkClassName());
            IJLinkMethod[] methodSpecs = jLinkClass.getMethodSpecs();
            String className = clazz.getSimpleName();
            // load and store the methods we want to use for each class of interest in the J/Link lib
            for ( IJLinkMethod methodSpec : methodSpecs ) {
                Method method = methodSpec.getArguments() == null ?
                        clazz.getMethod(methodSpec.getMethodID()) :
                        clazz.getMethod(methodSpec.getMethodID(), methodSpec.getArguments());
                methodRegister.put(
                        buildMethodRegisterID(className, methodSpec.getMethodID()),
                        method
                );
            }
        }

        LOG.info("Successfully loaded J/Link library");
        initiated = true;
    }

    private static String buildMethodRegisterID(String clazzName, String methodName) {
        return clazzName + "#" + methodName;
    }

    public Method getMethod(String clazzName, String methodName) {
        return methodRegister.get(buildMethodRegisterID(clazzName, methodName));
    }

    /**
     * Invokes the given method on the given object with the given arguments. The number of arguments is variable
     * and can be empty (not null).
     * @param method the method object we want to invoke
     * @param obj the instance we perform the method on
     * @param args the arguments for the method
     * @return the result Object or null if nothing was returned (a void method)
     * @throws ExprFormatException might be thrown by the invoked method
     * @throws MathLinkException might be thrown by the invoked method
     * @throws CASUnavailableException if another unusual error was thrown (neither of the above) during invoking the
     *                                  method. Note that an VM security issue can also prohibit invoking a method which
     *                                  is also encapsulated by a CAS unavailable exception here.
     */
    Object invoke(Method method, Object obj, Object... args)
            throws ExprFormatException, MathLinkException, CASUnavailableException {
        assert( method != null && initiated );
        try {
            return args == null ? method.invoke(obj) : method.invoke(obj, args);
        } catch (IllegalAccessException e) {
            LOG.error("Unable to call method '" + method.getName() + "'");
            throw new CASUnavailableException("Unable to communicate via Mathematica's interface J/Link", e);
        } catch (InvocationTargetException e) {
            LOG.error("Invoking J/Link method '" + method.getName() + "' threw an exception.");
            Throwable throwable = e.getCause();
            if ( throwable != null && throwable.getClass().getSimpleName().matches("ExprFormatException") )
                throw new ExprFormatException(throwable);
            else if ( throwable != null && throwable.getClass().getSimpleName().matches("MathLinkException") )
                throw new MathLinkException(throwable);
            else throw new CASUnavailableException("Unable to communicate via Mathematica's interface J/Link", e);
        }
    }

    static JLinkWrapper getInstance() {
        return instance;
    }
}
