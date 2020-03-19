package gov.nist.drmf.interpreter.maple.grammar;

import static gov.nist.drmf.interpreter.maple.common.MapleConstants.*;

import gov.nist.drmf.interpreter.common.constants.GlobalConstants;
import gov.nist.drmf.interpreter.common.grammar.Brackets;
import gov.nist.drmf.interpreter.maple.translation.MapleTranslator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;

/**
 * Translated list handles a list of already translated expressions.
 * It can be a translated expression by itself. In that case the list
 * is merged to one element.
 *
 * Main features of this classes are:
 *  1) Add translated expressions and lists
 *  2) Embrace the list
 *  3) Get an accurate string representation
 *      - this takes care of white spaces between symbols
 *      - handles signs
 *      - handles brackets
 *
 * Created by AndreG-P on 24.02.2017.
 */
public class TranslatedList extends TranslatedExpression {
    private static final Logger LOG = LogManager.getLogger( TranslatedList.class );

    /**
     * List of translated expression. Can be empty.
     */
    private LinkedList<TranslatedExpression> trans_list;

    /**
     * If this list should be wrapped by brackets.
     * Otherwise this variable is null.
     */
    private Brackets brackets;

    /**
     * Empty expression list.
     */
    public TranslatedList(){
        super();
        this.trans_list = new LinkedList<>();
    }

    /**
     * Copy constructor
     * @param list get copied
     */
    public TranslatedList( TranslatedList list ){
        this();
        this.trans_list = new LinkedList<>(list.trans_list);
        this.brackets = list.brackets;
        this.sign = list.sign;
    }

    /**
     * Add a new translated expression to this list.
     * Can be negative or positive also.
     * @param expression gets transformed to TranslatedExpresison
     */
    public void addTranslatedExpression( String expression ){
        TranslatedExpression t = new TranslatedExpression(expression);
        this.addTranslatedExpression(t);
    }

    /**
     * Add new translated expression to this list.
     * @param te translated expression
     */
    public void addTranslatedExpression( TranslatedExpression te ){
        this.trans_list.add( te );
    }

    /**
     * Add a whole other list to this list.
     * @param list
     */
    public void addTranslatedExpression( TranslatedList list ){
        if ( trans_list.isEmpty() && !list.isEmbraced() ){
            this.trans_list = list.trans_list;
            this.brackets = list.brackets;
            this.sign = calcSign( sign, list.sign );
        } else this.trans_list.add( list );
    }

    private boolean calcSign( boolean sign1, boolean sign2 ){
        if (( sign1 == POSITIVE && sign2 == POSITIVE ) ||
                ( sign1 == NEGATIVE && sign2 == NEGATIVE )) return POSITIVE;
        else return NEGATIVE;
    }

    /**
     * Removes the last expression. Be aware this can be a whole translated list.
     * @return the last expression (or the whole list)
     */
    public TranslatedExpression removeLastExpression(){
        try {
            TranslatedExpression last = trans_list.removeLast();
            if ( trans_list.isEmpty() ) {
                setSign( POSITIVE );
                brackets = null;
            }
            return last;
        } catch ( NoSuchElementException e ){
            return null;
        }
    }

    /**
     * Returns the last expression. Be aware this can be a whole translated list.
     * @return last expression or whole last translated list
     */
    public TranslatedExpression getLastExpression(){
        return trans_list.getLast();
    }

    /**
     * Returns the length of this list. The length
     * is not the real length. Since this list also contains lists itself, a
     * inner list will be counted as one.
     * @return
     */
    public int getLength(){
        if ( trans_list.isEmpty() ) return 0;
        int counter = 0;
        for ( TranslatedExpression te : trans_list ){
            if ( te instanceof TranslatedList )
                counter += ((TranslatedList)te).getLength();
            else counter++;
        }
        return counter;
    }

    /**
     * Embrace this expression with default brackets.
     * @see MapleTranslator#DEFAULT_LATEX_BRACKET
     */
    public void embrace(){
        embrace( MapleTranslator.DEFAULT_LATEX_BRACKET );
    }

    /**
     * Embraces this expression with given brackets.
     * Use the open bracket please.
     * @param brackets open bracket
     */
    public void embrace( Brackets brackets ){
        if ( !brackets.opened )
            this.brackets = brackets.getCounterPart();
        else this.brackets = brackets;
    }

    /**
     * Delete the brackets
     */
    public void removeBrackets(){
        brackets = null;
    }

    /**
     * Returns true if this expression is embraced, otherwise false.
     * @return true if this expression is embraced or false if not.
     */
    public boolean isEmbraced(){
        return brackets != null;
    }

    @Override
    public void setSign( boolean sign ) {
        super.sign = sign;
    }

    @Override
    public boolean getSign(){
        if ( !isEmbraced() && !trans_list.isEmpty() ){
            return calcSign( sign, trans_list.getFirst().getSign() );
        } else return sign;
    }

    @Override
    public boolean isNegative(){
        return !isPositive();
    }

    @Override
    public boolean isPositive(){
        return sign == POSITIVE;
    }

    @Override
    boolean isSummationSymbol(){
        if ( trans_list.size() != 1 ) return false;
        else return trans_list.getFirst().isSummationSymbol();
    }

    private static boolean isInternalNegative( TranslatedExpression te ){
        if ( te instanceof TranslatedList ){
            TranslatedList tl = (TranslatedList)te;
            if ( tl.isEmbraced() ) return tl.isNegative();
            TranslatedExpression f = tl.trans_list.getFirst();
            boolean inner_is_neg = isInternalNegative(f);
            return (tl.isNegative() && !inner_is_neg) || (tl.isPositive() && inner_is_neg);
        } else return te.isNegative();
    }

    private static void setLeadingPositive( TranslatedExpression te ){
        if ( te instanceof TranslatedList ){
            TranslatedList list = (TranslatedList)te;
            if ( list.isNegative() ) list.setSign( POSITIVE );
            else setLeadingPositive( list.trans_list.getFirst() );
        } else te.setSign( POSITIVE );
    }

    /**
     *
     * @return
     */
    public String getAccurateString() throws IllegalArgumentException {
        if ( trans_list.isEmpty() ) return "";

        if ( trans_list.size() == 1 && !isEmbraced() ){
            TranslatedExpression t = trans_list.getFirst();
            if ( t instanceof TranslatedList ){
                TranslatedList l = (TranslatedList)t;
                t = new TranslatedExpression(l.getAccurateString());
            }
            if ( (isNegative() && t.isNegative())
                    || (isPositive() && t.isPositive())) return t.getPlainExpression();
            else return MINUS_SIGN + t.getPlainExpression();
        }

        LinkedList<TranslatedExpression> copy_elements_list = new LinkedList<>();

        TranslatedList copy = new TranslatedList(this);
        TranslatedExpression previous = null;
        TranslatedExpression current;

        // first, copy all and eliminate +/- problems
        while ( !copy.trans_list.isEmpty() ){
            current = copy.trans_list.removeLast();

            if ( current.isSummationSymbol() ){
                if ( previous == null )
                    continue;
                //    throw new IllegalArgumentException("Expression cannot ends with a + or -.");
                else if ( previous.isSummationSymbol() )
                    throw new IllegalArgumentException("Two +/- symbols in a row is not allowed.");
                else if ( isInternalNegative( previous ) ){
                    if ( current.isPositive() )
                        copy_elements_list.addFirst( new TranslatedExpression( MINUS_SIGN, POSITIVE ) );
                    else copy_elements_list.addFirst( new TranslatedExpression( PLUS_SIGN, POSITIVE ) );
                } else {
                    String s = previous.isNegative() ? MINUS_SIGN : PLUS_SIGN;
                    copy_elements_list.addFirst( new TranslatedExpression( s, POSITIVE ) );
                }

                setLeadingPositive( previous );

                previous = current;
                continue;
            }

            copy_elements_list.addFirst( current );
            previous = current;
        }

        // now build output string
        LinkedList<String> str_list = new LinkedList<>();
        while ( !copy_elements_list.isEmpty() ){
            current = copy_elements_list.removeFirst();
            if ( current instanceof TranslatedList ){
                TranslatedList l = (TranslatedList)current;
                str_list.add( l.getAccurateString() );
            } else {
                str_list.add( current.toString() );
            }
        }

        String output = "", curr;
        while ( !str_list.isEmpty() ){
            curr = str_list.removeFirst();
            if ( str_list.size() >= 1 && whiteSpaceCheck( curr, str_list.getFirst() ) )
                curr += GlobalConstants.WHITESPACE;
            output += curr;
        }

        if ( isEmbraced() ){
            output = brackets.symbol + output + brackets.counterpart;
        }

        if ( isNegative() ){
            output = MINUS_SIGN + output;
        }

        return output;
    }

    private boolean whiteSpaceCheck( String s, String next ){
        Matcher m = GlobalConstants.LATEX_COMMAND_PATTERN.matcher( s );
        return m.matches() && next.matches( "[A-Za-z]+.*" );
    }

    @Override
    @Deprecated
    public String toString(){
        String str = "";
        str = trans_list.toString();
        if ( isEmbraced() ) str = brackets.symbol + str + brackets.counterpart;
        if ( isNegative() ) str = "-" + str;
//        for ( TranslatedExpression s : trans_list )
//            str += s.toString();
        return str;
    }
}
