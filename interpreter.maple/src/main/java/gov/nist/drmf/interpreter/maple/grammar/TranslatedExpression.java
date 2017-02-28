package gov.nist.drmf.interpreter.maple.grammar;

import static gov.nist.drmf.interpreter.maple.common.MapleConstants.*;

import java.util.regex.Matcher;

/**
 * This object is created to represent a translated expression.
 * This can be just a subexpression but also the global translated
 * expression.
 *
 * Created by AndreG-P on 24.02.2017.
 */
public class TranslatedExpression {
    private String expression = "";
    private boolean sign = true;

    private boolean summation_symbol = false;

    protected TranslatedExpression(){}

    public TranslatedExpression( String expression ){
        Matcher m = PLUS_MINUS_SIGN_PATTER.matcher( expression );
        if ( m.matches() ) {
            summation_symbol = true;
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

        m = PREV_NEG_SIGN_PATTERN.matcher( expression );
        if ( m.matches() ){
            this.expression = m.group(1);
            this.sign = NEGATIVE;
        } else this.expression = expression;
    }

    public TranslatedExpression( String expression, boolean sign ){
        this( expression );
        this.sign = sign;
    }

    public TranslatedExpression( TranslatedExpression texp ){
        this.expression = texp.expression;
        this.sign = texp.sign;
        this.summation_symbol = texp.summation_symbol;
    }

    /**
     * Use this function carefully. Only the subclass should use this function
     * because the class is probably no longer inherent after you called this method!
     * @param expression
     */
    protected void changeExpression( String expression ){
        this.expression = expression;
    }

    /**
     * Set the sign of this expression.
     *
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
    public String getPlainExpression(){
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
    protected boolean getSign(){
        return sign;
    }

    /**
     * Returns if this is a + or -.
     * @return true if this expression is + or -
     */
    public boolean isSummationSymbol(){
        return summation_symbol;
    }

    /**
     *
     * @return
     */
    @Override
    public String toString(){
        if ( !sign ) return MINUS_SIGN + expression;
        return expression;
    }
}
