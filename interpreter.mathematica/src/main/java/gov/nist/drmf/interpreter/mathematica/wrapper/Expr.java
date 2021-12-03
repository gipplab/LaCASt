package gov.nist.drmf.interpreter.mathematica.wrapper;

import java.lang.reflect.Method;

public class Expr implements IJLinkClass {
    private static final String CLAZZ_NAME = "Expr";
    private static final String CLAZZ_PATH = "com.wolfram.jlink." + CLAZZ_NAME;

    private static final JLinkWrapper jLinkWrapper = JLinkWrapper.getInstance();

    private final Object expr;

    Expr() {
        this.expr = null;
    }

    Expr(Object expr) {
        this.expr = expr;
    }

    public Expr[] args() {
        Object[] args = (Object[]) Methods.args.invoke(expr);
        int len = args.length;
        Expr[] out = new Expr[len];
        for (int i = 0; i < len; i++) {
            out[i] = new Expr(args[i]);
        }
        return out;
    }

    public int length() {
        return (int) Methods.length.invoke(expr);
    }

    public boolean trueQ() {
        return (boolean) Methods.trueQ.invoke(expr);
    }

    public boolean numberQ() {
        return (boolean) Methods.numberQ.invoke(expr);
    }

    public double asDouble() throws ExprFormatException {
        return (double) Methods.asDouble.invoke(expr);
    }

    public Expr head() {
        return new Expr(Methods.head.invoke(expr));
    }

    public boolean listQ() {
        return (boolean) Methods.listQ.invoke(expr);
    }

    public String asString() throws ExprFormatException {
        return (String) Methods.asString.invoke(expr);
    }

    @Override
    public String toString() {
        return expr.toString();
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
        head(),
        args(),
        length(),
        trueQ(),
        listQ(),
        numberQ(),
        asDouble(),
        asString();

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
            Method method = jLinkWrapper.getMethod( CLAZZ_NAME, id );
            return jLinkWrapper.invoke(method, obj, arguments);
        }
    }
}
