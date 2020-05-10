package gov.nist.drmf.interpreter.maple.extension;

import com.maplesoft.externalcall.MapleException;
import com.maplesoft.openmaple.Algebraic;
import gov.nist.drmf.interpreter.common.cas.ICASEngineSymbolicEvaluator;
import gov.nist.drmf.interpreter.common.exceptions.ComputerAlgebraSystemEngineException;
import gov.nist.drmf.interpreter.maple.listener.MapleListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author Andre Greiner-Petter
 */
public class Simplifier implements ICASEngineSymbolicEvaluator<Algebraic> {
    private static final Logger LOG = LogManager.getLogger(Simplifier.class.getName());

    /**
     * This zero pattern allows expressions such as
     *  0 or 0. or 0.0 or 0.000 and so on.
     */
    private static final String ZERO_PATTERN = "0\\.?0*";

    private final MapleInterface maple;
    private final MapleListener listener;

    private int timeout = -1;

    public Simplifier() {
        maple = MapleInterface.getUniqueMapleInterface();
        listener = MapleInterface.getUniqueMapleListener();
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    /**
     * This method takes two maple expressions and returns true when both expression
     * are symbolically the same. To verify this, we use the "simplify" command from
     * Maple. Be aware that both expressions still can be mathematically equivalent
     * even when this method returns false!
     *
     * Be also aware that null inputs always returns false, even when both inputs are null.
     * However, two empty expression such as "" and "" returns true.
     *
     * @param exp1 Maple string of the first expression
     * @param exp2 Maple string of the second expression
     * @return true if both expressions are symbolically equivalent or false otherwise.
     *          If it returns false, both expressions still can be mathematically equivalent!
     * @throws MapleException If the test of equivalence produces an Maple error.
     */
    public boolean isEquivalent( String exp1, String exp2 )
            throws ComputerAlgebraSystemEngineException {
        if ( isNullOrEmpty(exp1, exp2) ) return false;

        // otherwise build simplify command to test equivalence
        String command = "(" + exp1 + ") - (" + exp2 + ")";
        Algebraic a = simplify( command );
        try {
            return isZero(a);
        } catch ( MapleException me ) {
            throw new ComputerAlgebraSystemEngineException(me);
        }
    }

    /**
     * This method takes two maple expressions and converts the difference
     * to the specified function before it tries to simplify the difference.
     *
     * It works exactly in the same way as {@link #isEquivalent(String, String)},
     * but converts the difference of {@param exp1} and {@param exp2} before it tries
     * to simplify the new expression.
     *
     * @param exp1 Maple string of the first expression
     * @param exp2 Maple string of the second expression
     * @param conversion Specified the destination of the conversion. For example, "expe" or "hypergeom".
     * @return true if both expressions are symbolically equivalent or false otherwise.
     *          If it returns false, both expressions still can be mathematically equivalent!
     * @throws MapleException If the test of equivalence produces an Maple error.
     */
    public boolean isEquivalentWithConversion(
            String exp1,
            String exp2,
            String conversion )
            throws ComputerAlgebraSystemEngineException, MapleException {
        if ( isNullOrEmpty(exp1, exp2) ) return false;

        // otherwise build simplify command to test equivalence
        String command = "convert((" + exp1 + ") - (" + exp2 + "),"+ conversion +")";
        Algebraic a = simplify( command );
        return isZero(a);
    }

    public Algebraic isMultipleEquivalent( String exp1, String exp2 )
            throws ComputerAlgebraSystemEngineException {
        if ( isNullOrEmpty(exp1, exp2) ) return null;

        // otherwise build simplify command to test equivalence
        String command = "(" + exp1 + ") / (" + exp2 + ")";
        Algebraic a = simplify( command );
        return a;
    }

    public Algebraic isMultipleEquivalentWithConversion(
            String exp1,
            String exp2,
            String conversion )
            throws ComputerAlgebraSystemEngineException{
        if ( isNullOrEmpty(exp1, exp2) ) return null;

        // otherwise build simplify command to test equivalence
        String command = "convert((" + exp1 + ") / (" + exp2 + "),"+ conversion +")";
        return simplify( command );
    }

    public boolean isEquivalentWithExpension(
            String exp1,
            String exp2,
            String conversion
    ) throws ComputerAlgebraSystemEngineException, MapleException {
        if ( isNullOrEmpty(exp1, exp2) ) return false;

        // otherwise build simplify command to test equivalence
        String command = "expand((" + exp1 + ") - (" + exp2 + ")";
        command += conversion == null ? ")" : "," + conversion + ")";
        Algebraic a = simplify( command );
        return isZero(a);
    }

    public Algebraic isMultipleEquivalentWithExpension(
            String exp1,
            String exp2,
            String conversion
    ) throws ComputerAlgebraSystemEngineException {
        if ( isNullOrEmpty(exp1, exp2) ) return null;

        // otherwise build simplify command to test equivalence
        String command = "expand((" + exp1 + ") / (" + exp2 + ")";
        command += conversion == null ? ")" : "," + conversion + ")";
        return simplify( command );
    }

    /**
     * Checks if the given algebraic object is 0.
     * @param a an algebraic object
     * @return true if the result is 0. False otherwise.
     * @throws MapleException if the given command produces an error in Maple.
     */
    public static boolean isZero( Algebraic a ) throws MapleException {
        // null solutions returns false
        if ( a == null || a.isNULL() ) return false;
        // analyze the output string and returns true when it matches "0".
        String solution_str = a.toString();
        return solution_str.trim().matches(ZERO_PATTERN);
    }

    /**
     * If one of them is null, returns true.
     * If none is null but one of them is empty, it returns true
     * when both are empty, otherwise false.
     * Otherwise returns false.
     * @param exp1 string
     * @param exp2 string
     * @return true or false
     */
    public static boolean isNullOrEmpty( String exp1, String exp2 ){
        // test if one of the inputs is null
        if ( exp1 == null || exp2 == null ) return true;
        // if one of the expressions is empty, it only returns true when both are empty
        if ( exp1.isEmpty() || exp2.isEmpty() ){
            return !(exp1.isEmpty() && exp2.isEmpty());
        }
        return false;
    }

    /**
     * Simplify given expression. Be aware, the given expression should not
     * end with ';'.
     * @param maple_expr given maple expression, without ';'
     * @return the algebraic object of the result of simplify(maple_expr);
     * @throws MapleException if the given expression cannot be evaluated.
     * @see Algebraic
     */
    public Algebraic mapleSimplify( String maple_expr ) throws MapleException {
        String command = "simplify(" + maple_expr + ")";
        if ( timeout > 0 ) {
            command = "try timelimit("+timeout+","+command+"); catch \"time expired\": \"";
            command += MapleInterface.TIMED_OUT_SIGNAL;
            command += "\"; end try;";
        } else command += ";";
        LOG.debug("Simplification: " + command);
        listener.timerReset();
        return maple.evaluate( command );
    }

    @Override
    public Algebraic simplify(String expr) throws ComputerAlgebraSystemEngineException {
        try {
            return mapleSimplify(expr);
        } catch ( MapleException me ) {
            throw new ComputerAlgebraSystemEngineException(me);
        }
    }

    private String buildAssumeSimplify(String expr, String assumption) {
        String cmd = "simplify(" + expr + ")";//+ assuming " + assumption + ";";

        if ( timeout > 0 ) {
            cmd = "try timelimit("+timeout+", "+ cmd+") ";
            cmd += "assuming " + assumption + "; ";
            cmd += "catch \"time expired\": \"";
            cmd += MapleInterface.TIMED_OUT_SIGNAL;
            cmd += "\"; end try;";
        } else cmd += "assuming " + assumption + ";";

        return cmd;
    }

    @Override
    public Algebraic simplify(String expr, String assumption) throws ComputerAlgebraSystemEngineException {
        try {
            String cmd = buildAssumeSimplify(expr, assumption);
            LOG.debug("Simplification: " + cmd);
            listener.timerReset();
            return maple.evaluate( cmd );
        } catch ( MapleException me ) {
            throw new ComputerAlgebraSystemEngineException(me);
        }
    }

    @Override
    public boolean isAsExpected(Algebraic in, String expect) {
        String str = in.toString();
        if ( expect == null ){
            try {
                Double.parseDouble(str);
                return true;
            } catch ( NumberFormatException nfe ) {}
            return false;
        } else if ( str.matches(expect) ) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void abort() {
        LOG.warn("Abortion is not supported by Maple.");
    }

    @Override
    public boolean wasAborted(Algebraic result) {
        return maple.isAbortedExpression(result);
    }
}