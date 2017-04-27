package gov.nist.drmf.interpreter;

import com.maplesoft.externalcall.MapleException;
import com.maplesoft.openmaple.Algebraic;
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
    public boolean simplificationTester(@Nullable String exp1, @Nullable String exp2 )
            throws MapleException {
        if ( nullChecker(exp1, exp2) ) return false;

        // otherwise build simplify command to test equivalence
        String command = "(" + exp1 + ") - (" + exp2 + ")";
        return simplificationTesterOf( command );
    }

    /**
     * This method takes two maple expressions and converts the difference
     * to the specified function before it tries to simplify the difference.
     *
     * It works exactly in the same way as {@link #simplificationTester(String, String)},
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
    public boolean simplificationTesterWithConversion(
            @Nullable String exp1, @Nullable String exp2, @Nonnull String conversion )
            throws MapleException{
        if ( nullChecker(exp1, exp2) ) return false;

        // otherwise build simplify command to test equivalence
        String command = "convert((" + exp1 + ") - (" + exp2 + "),"+ conversion +")";
        return simplificationTesterOf( command );
    }

    public boolean simplificationTesterWithExpension(
            @Nullable String exp1, @Nullable String exp2, @Nullable String conversion
    ) throws MapleException {
        if ( nullChecker(exp1, exp2) ) return false;

        // otherwise build simplify command to test equivalence
        String command = "expand((" + exp1 + ") - (" + exp2 + ")";
        command += conversion == null ? ")" : "," + conversion + ")";
        return simplificationTesterOf( command );
    }

    private boolean nullChecker( String exp1, String exp2 ){
        // test if one of the inputs is null
        if ( exp1 == null || exp2 == null ) return true;
        // if one of the expressions is empty, it only returns true when both are empty
        if ( exp1.isEmpty() || exp2.isEmpty() ){
            return !(exp1.isEmpty() && exp2.isEmpty());
        }
        return false;
    }

    public boolean simplificationTesterOf( String expression )
            throws MapleException{
        String command = "simplify(" + expression + ");";
        LOG.debug("Simplification-Test: " + expression);
        return resultIsEqualToZeroTest( command );
    }

    /**
     * Checks if the given command returns 0.
     * @param command usually a simplify(...) commmand.
     * @return true if the result is 0. False otherwise.
     * @throws MapleException if the given command produces an error in Maple.
     */
    private boolean resultIsEqualToZeroTest( String command ) throws MapleException {
        // analyze the algebraic solution
        Algebraic solution = mapleInterface.evaluateExpression( command );
        // null solutions returns false
        if ( solution == null || solution.isNULL() ) return false;
        // analyze the output string and returns true when it matches "0".
        String solution_str = solution.toString();
        return solution_str.trim().matches("0");
    }

}
