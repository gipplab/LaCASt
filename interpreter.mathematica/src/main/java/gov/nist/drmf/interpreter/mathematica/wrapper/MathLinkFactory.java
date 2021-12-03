package gov.nist.drmf.interpreter.mathematica.wrapper;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;

public class MathLinkFactory implements IJLinkClass {
    private static final String CLAZZ_NAME = "MathLinkFactory";
    private static final String CLAZZ_PATH = "com.wolfram.jlink." + CLAZZ_NAME;

    private static final JLinkWrapper jLinkWrapper = JLinkWrapper.getInstance();

    MathLinkFactory() {}

    public static KernelLink createKernelLink(String[] args)
            throws  MathLinkException, ClassNotFoundException, NoSuchMethodException,
                    MalformedURLException, IllegalAccessException, InstantiationException,
                    InvocationTargetException {
        invokeReflectionInit();
        Method method = jLinkWrapper.getMethod(CLAZZ_NAME, "createKernelLink");
        return new KernelLink( jLinkWrapper.invoke(method, null, (Object) args) );
    }

    public static void invokeReflectionInit() throws ClassNotFoundException, NoSuchMethodException,
            MalformedURLException, IllegalAccessException, InstantiationException,
            InvocationTargetException {
        jLinkWrapper.init();
    }

    @Override
    public String getJLinkClassName() {
        return CLAZZ_PATH;
    }

    @Override
    public IJLinkMethod[] getMethodSpecs() {
        return new IJLinkMethod[] {
                new IJLinkMethod() {
                    @Override
                    public String getMethodID() {
                        return "createKernelLink";
                    }

                    @Override
                    public Class<?>[] getArguments() {
                        return new Class[]{String[].class};
                    }
                }
        };
    }
}
