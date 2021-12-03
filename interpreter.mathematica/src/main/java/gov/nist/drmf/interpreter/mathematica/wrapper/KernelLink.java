package gov.nist.drmf.interpreter.mathematica.wrapper;

import java.lang.reflect.Method;

public class KernelLink implements IJLinkClass {
    private static final String CLAZZ_NAME = "KernelLink";
    private static final String CLAZZ_PATH = "com.wolfram.jlink." + CLAZZ_NAME;

    private static final JLinkWrapper jLinkWrapper = JLinkWrapper.getInstance();

    private final Object kernelLink;

    KernelLink() {
        this.kernelLink = null;
    }

    KernelLink(Object kernelLink) {
        this.kernelLink = kernelLink;
    }

    public void evaluate(String s) throws MathLinkException {
        Methods.evaluate.invoke(kernelLink, s);
    }

    public void discardAnswer() throws MathLinkException {
        Methods.discardAnswer.invoke(kernelLink);
    }

    public void clearError() {
        Methods.clearError.invoke(kernelLink);
    }

    public void newPacket() {
        Methods.newPacket.invoke(kernelLink);
    }

    public void waitForAnswer() throws MathLinkException {
        Methods.waitForAnswer.invoke(kernelLink);
    }

    public Expr getExpr() throws MathLinkException {
        return new Expr(Methods.getExpr.invoke(kernelLink));
    }

    public String evaluateToOutputForm(String fullf, int i) {
        return (String) Methods.evaluateToOutputForm.invoke(kernelLink, fullf, i);
    }

    public void close() {
        Methods.close.invoke(kernelLink);
    }

    public boolean getBoolean() throws MathLinkException {
        return (boolean) Methods.getBoolean.invoke(kernelLink);
    }

    public void abortEvaluation() {
        Methods.abortEvaluation.invoke(kernelLink);
    }

    public Throwable getLastError() {
        return (Throwable) Methods.getLastError.invoke(kernelLink);
    }

    @Override
    public String getJLinkClassName() {
        return CLAZZ_PATH;
    }

    @Override
    public IJLinkMethod[] getMethodSpecs() {
        return Methods.values();
    }

    private enum Methods implements IJLinkMethod {
        evaluate(String.class),
        evaluateToOutputForm(String.class, int.class),
        abortEvaluation(),

        waitForAnswer(),
        discardAnswer(),

        getExpr(),
        getBoolean(),
        getLastError(),

        clearError(),
        newPacket(),
        close();

        private final String id;
        private final Class<?>[] arguments;

        Methods(Class<?>... args) {
            this.id = this.name();
            this.arguments = args;
        }

        @Override
        public String getMethodID() {
            return id;
        }

        @Override
        public Class<?>[] getArguments() {
            return arguments;
        }

        public Object invoke(Object obj, Object... arguments) {
            Method method = jLinkWrapper.getMethod(CLAZZ_NAME, id);
            return jLinkWrapper.invoke(method, obj, arguments);
        }
    }
}
