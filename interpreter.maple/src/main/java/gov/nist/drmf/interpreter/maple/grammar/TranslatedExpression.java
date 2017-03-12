package gov.nist.drmf.interpreter.maple.grammar;

import java.util.regex.Matcher;

import static gov.nist.drmf.interpreter.maple.common.MapleConstants.*;

/**
 * This object is created to represent a translated expression.
 * This can be just a subexpression but also the global translated
 * expression.
 *
 * Created by AndreG-P on 24.02.2017.
 */
public class TranslatedExpression {
    // The plain expression, without sign
    private String expression = "";

    // The sign of this translated expression
    boolean sign = POSITIVE;

    // if this sign is a +/- symbol
    private boolean plus_minus_symbol = false;

    /**
     * Default constructor.
     */
    TranslatedExpression(){}

    /**
     * Creates a translated expression by given string.
     * It checks if this expression contains a leading -.
     * @param expression translated expression
     */
    public TranslatedExpression( String expression ){
        // test if this expression is a +/- sign
        Matcher m = PLUS_MINUS_SIGN_PATTER.matcher( expression );
        if ( m.matches() ) {
            plus_minus_symbol = true;
            if ( m.group(1).equals(MINUS_SIGN) ){
                sign = NEGATIVE;
                this.expression = MINUS_SIGN;
            }
            else {
                sign = POSITIVE;
                this.expression = PLUS_SIGN;
            }
            return;
        }

        // test if this expression has a leading - symbol
        m = PREV_NEG_SIGN_PATTERN.matcher( expression );
        if ( m.matches() ){
            this.expression = m.group(1);
            this.sign = NEGATIVE;
        } else this.expression = expression;
    }

    /**
     * Create a translated expression with defined defined sign
     * @param expression translated expression
     * @param sign {@link gov.nist.drmf.interpreter.maple.common.MapleConstants#POSITIVE} or
     *             {@link gov.nist.drmf.interpreter.maple.common.MapleConstants#NEGATIVE}
     */
    public TranslatedExpression( String expression, boolean sign ){
        this( expression );
        this.sign = sign;
    }

    /**
     * Copy constructor
     * @param texp copy expression
     */
    public TranslatedExpression( TranslatedExpression texp ){
        this.expression = texp.expression;
        this.sign = texp.sign;
        this.plus_minus_symbol = texp.plus_minus_symbol;
    }

    /**
     * Use this function carefully. Only the subclass should use this function
     * because the class is probably no longer inherent after you called this method!
     * @param expression new expression
     */
    void changeExpression( String expression ){
        this.expression = expression;
    }

    /**
     * Set the sign of this expression.
     * @param sign is {@link gov.nist.drmf.interpreter.maple.common.MapleConstants#POSITIVE}
     *             or {@link gov.nist.drmf.interpreter.maple.common.MapleConstants#NEGATIVE}.
     */
    public void setSign( boolean sign ){
        this.sign = sign;
    }

    /**
     * Returns the plain expression without the sign!
     * @return the plain expression without sign
     */
    String getPlainExpression(){
        return expression;
    }

    /**
     * Returns true when this expression has a positive sign.
     * @return true if this expression is positive
     */
    public boolean isPositive(){
        return sign == POSITIVE;
    }

    /**
     * Returns true when this expression has a negative sign.
     * @return true when this expression is negative
     */
    public boolean isNegative(){
        return sign == NEGATIVE;
    }

    /**
     * Returns the current sign of this expression.
     * @return {@link gov.nist.drmf.interpreter.maple.common.MapleConstants#POSITIVE}
     *          or {@link gov.nist.drmf.interpreter.maple.common.MapleConstants#NEGATIVE}.
     */
    public boolean getSign(){
        return sign;
    }

    /**
     * Returns if this is a + or -.
     * @return true if this expression is + or -
     */
    boolean isSummationSymbol(){
        return plus_minus_symbol;
    }

    /**
     * Returns the "accurate" string representation.
     * For negative symbol a, it returns -a.
     * Summation symbols like +/- will returned just as + or -.
     * @return
     */
    @Override
    public String toString(){
        if ( isNegative() && !isSummationSymbol() )
            return MINUS_SIGN + expression;
        return expression;
    }
}
