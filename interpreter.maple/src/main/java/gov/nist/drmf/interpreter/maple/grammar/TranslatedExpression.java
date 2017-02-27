package gov.nist.drmf.interpreter.maple.grammar;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This object is created to represent a translated expression.
 * This can be just a subexpression but also the global translated
 * expression.
 *
 * Created by AndreG-P on 24.02.2017.
 */
public class TranslatedExpression {
    public static final String SUM_SIGN_PATTERN = "\\s*[+-]\\s*";

    public static final String PREV_NEG_SIGN = "\\s*-\\s*([^\\s]+)";
    public static final Pattern PREV_NEG_SIGN_PATTERN = Pattern.compile( PREV_NEG_SIGN );

    public static final boolean POSITIVE = true;
    public static final boolean NEGATIVE = false;

    public static final char NEGATIVE_SIGN = '-';

    private String expression = "";
    private boolean sign = true;

    private boolean summation_symbol = false;

    public TranslatedExpression( String expression ){
        if ( expression.matches( SUM_SIGN_PATTERN ) ) {
            this.expression = expression;
            summation_symbol = true;
        }

        Matcher m = PREV_NEG_SIGN_PATTERN.matcher( expression );
        if ( m.matches() ){
            this.expression = m.group(1);
            this.sign = NEGATIVE;
        } else this.expression = expression;
    }

    public TranslatedExpression( String expression, boolean sign ){
        this( expression );
        this.sign = sign;
    }

    protected void changeExpression( String expression ){
        this.expression = expression;
    }

    /**
     * Set the sign of this expression.
     *
     * @param sign is {@link #POSITIVE} or {@link #NEGATIVE}.
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
        if ( !sign ) return NEGATIVE_SIGN + expression;
        return expression;
    }
}
