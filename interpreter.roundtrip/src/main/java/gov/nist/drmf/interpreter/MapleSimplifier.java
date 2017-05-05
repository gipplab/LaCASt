package gov.nist.drmf.interpreter;

import com.maplesoft.externalcall.MapleException;
import com.maplesoft.openmaple.Algebraic;
import com.maplesoft.openmaple.MString;
import gov.nist.drmf.interpreter.maple.translation.MapleInterface;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 *
 * Created by AndreG-P on 27.04.2017.
 */
public class MapleSimplifier {
    private static final Logger LOG = LogManager.getLogger(MapleSimplifier.class.toString());

    /**
     * This zero pattern allows expressions such as
     *  0 or 0. or 0.0 or 0.000 and so on.
     */
    private static final String ZERO_PATTERN = "0\\.?0*";

    private MapleInterface mapleInterface;

    MapleSimplifier( MapleInterface mapleInterface ){
        this.mapleInterface = mapleInterface;
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
    public boolean isEquivalent( @Nullable String exp1, @Nullable String exp2 )
            throws MapleException {
        if ( isNullOrEmpty(exp1, exp2) ) return false;

        // otherwise build simplify command to test equivalence
        String command = "(" + exp1 + ") - (" + exp2 + ")";
        Algebraic a = simplify( command );
        return isZero(a);
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
            @Nullable String exp1,
            @Nullable String exp2,
            @Nonnull String conversion )
            throws MapleException{
        if ( isNullOrEmpty(exp1, exp2) ) return false;

        // otherwise build simplify command to test equivalence
        String command = "convert((" + exp1 + ") - (" + exp2 + "),"+ conversion +")";
        Algebraic a = simplify( command );
        return isZero(a);
    }

    public boolean isEquivalentWithExpension(
            @Nullable String exp1,
            @Nullable String exp2,
            @Nullable String conversion
    ) throws MapleException {
        if ( isNullOrEmpty(exp1, exp2) ) return false;

        // otherwise build simplify command to test equivalence
        String command = "expand((" + exp1 + ") - (" + exp2 + ")";
        command += conversion == null ? ")" : "," + conversion + ")";
        Algebraic a = simplify( command );
        return isZero(a);
    }

    /**
     * Simplify given expression. Be aware, the given expression should not
     * end with ';'.
     * @param maple_expr given maple expression, without ';'
     * @return the algebraic object of the result of simplify(maple_expr);
     * @throws MapleException if the given expression cannot be evaluated.
     * @see Algebraic
     */
    public Algebraic simplify( String maple_expr ) throws MapleException {
        String command = "simplify(" + maple_expr + ");";
        LOG.debug("Simplification: " + command);
        return mapleInterface.evaluateExpression( command );
    }

    /**
     *
     * @param expr
     * @return
     */
    public RelationResults holdsRelation( @Nullable String expr ) throws MapleException {
        try {
            String command = "op(1, ToInert(is(" + expr + ")));";
            Algebraic a = mapleInterface.evaluateExpression( command );
            if ( !(a instanceof MString) ) return RelationResults.ERROR;

            MString ms = (MString) a;
            String s = ms.stringValue();
            if ( s.equals("true") ) return RelationResults.TRUE;
            if ( s.equals("false") ) return RelationResults.FALSE;
            if ( s.equals("FAIL") ) return RelationResults.FAIL;
            return RelationResults.ERROR;
        } catch ( MapleException me ){
            return RelationResults.ERROR;
        }
    }

    /**
     * Checks if the given algebraic object is 0.
     * @param a an algebraic object
     * @return true if the result is 0. False otherwise.
     * @throws MapleException if the given command produces an error in Maple.
     */
    private boolean isZero( Algebraic a ) throws MapleException {
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
    private boolean isNullOrEmpty( String exp1, String exp2 ){
        // test if one of the inputs is null
        if ( exp1 == null || exp2 == null ) return true;
        // if one of the expressions is empty, it only returns true when both are empty
        if ( exp1.isEmpty() || exp2.isEmpty() ){
            return !(exp1.isEmpty() && exp2.isEmpty());
        }
        return false;
    }
}
