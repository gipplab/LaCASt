package gov.nist.drmf.interpreter.mathematica.wrapper;

import java.lang.reflect.Method;

public class MathLinkException extends RuntimeException implements IJLinkClass {
    private static final String CLAZZ_NAME = "MathLinkException";
    private static final String CLAZZ_PATH = "com.wolfram.jlink." + CLAZZ_NAME;

    private static final JLinkWrapper jLinkWrapper = JLinkWrapper.getInstance();

    private final int errorCode;

    MathLinkException() {
        super();
        errorCode = -1;
    }

    MathLinkException(Throwable e) {
        super(e.getMessage(), e);

        Method method = jLinkWrapper.getMethod(CLAZZ_NAME, "getErrCode");
        this.errorCode = (Integer) jLinkWrapper.invoke(method, e);
    }

    public int getErrorCode() {
        return errorCode;
    }

    @Override
    public String getJLinkClassName() {
        return CLAZZ_PATH;
    }

    @Override
    public IJLinkMethod[] getMethodSpecs() {
        return new IJLinkMethod[]{
                new IJLinkMethod() {
                    @Override
                    public String getMethodID() {
                        return "getErrCode";
                    }

                    @Override
                    public Class<?>[] getArguments() {
                        return null;
                    }
                }
        };
    }
}
