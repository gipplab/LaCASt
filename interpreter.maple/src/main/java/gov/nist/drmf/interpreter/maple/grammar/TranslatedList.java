package gov.nist.drmf.interpreter.maple.grammar;

import static gov.nist.drmf.interpreter.maple.common.MapleConstants.*;

import gov.nist.drmf.interpreter.common.GlobalConstants;
import gov.nist.drmf.interpreter.common.grammar.Brackets;
import gov.nist.drmf.interpreter.maple.translation.MapleInterface;

import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * TODO should we handle spaces here as well? I think so.
 *
 * Created by AndreG-P on 24.02.2017.
 */
public class TranslatedList extends TranslatedExpression {
    public static final String LATEX_COMMAND = "\\\\[a-zA-Z\\(\\)\\[\\]\\{}]+";
    public static final Pattern LATEX_COMMAND_PATTERN = Pattern.compile(LATEX_COMMAND);

    private LinkedList<TranslatedExpression> trans_list;

    private Brackets brackets;

    public TranslatedList(){
        super();
        this.trans_list = new LinkedList<>();
    }

    public void addTranslatedExpression( String expression ){
        TranslatedExpression t = new TranslatedExpression(expression);
        this.addTranslatedExpression(t);
    }

    public void addTranslatedExpression( TranslatedExpression te ){
        this.trans_list.add( te );
    }

    public void addTranslatedExpression( TranslatedList list ){

        if ( list.isEmbraced() ){
            trans_list.add( list.merge() );
            return;
        }

        this.trans_list.addAll(list.trans_list);
    }

    public void addPreviousTranslatedExpression( String expression ){
        TranslatedExpression t = new TranslatedExpression( expression );
        this.addPreviousTranslatedExpression(t);
    }

    public void addPreviousTranslatedExpression( TranslatedExpression te ){
        this.trans_list.addFirst( te );
    }

    public void addPreviousTranslatedExpression( TranslatedList list ){
        this.trans_list.addAll( 0, list.trans_list );
    }

    public TranslatedExpression removeLastExpression(){
        try {
            TranslatedExpression last = trans_list.removeLast();
            if ( trans_list.isEmpty() ) setSign( POSITIVE );
            return last;
        } catch ( NoSuchElementException e ){
            return null;
        }
    }

    public TranslatedExpression removePreviousTranslatedExpression(){
        try {
            TranslatedExpression last = trans_list.removeFirst();
            if ( trans_list.isEmpty() ) setSign( POSITIVE );
            return last;
        } catch ( NoSuchElementException e ){
            return null;
        }
    }

    public int getLength(){
        return trans_list.size();
    }

    public void embrace(){
        embrace( MapleInterface.DEFAULT_LATEX_BRACKET );
    }

    public void embrace( Brackets brackets ){
        if ( !brackets.opened )
            this.brackets = brackets.getCounterPart();
        else this.brackets = brackets;
    }

    public void removeBrackets(){
        brackets = null;
    }

    public boolean isEmbraced(){
        return brackets != null;
    }

    public TranslatedExpression merge(){
        String representation = getAccurateString();
        return new TranslatedExpression( representation );
    }

    @Override
    public void setSign( boolean sign ){
        if ( sign != getSign() ){
            super.setSign( sign );
            if ( sign == NEGATIVE && !isEmbraced() ){
                embrace( MapleInterface.DEFAULT_LATEX_BRACKET );
            } else if ( sign == POSITIVE && isEmbraced() )
                removeBrackets();
        }
    }

    /**
     *
     * @return
     */
    public String getAccurateString(){
        if ( trans_list.isEmpty() ) return "";
        LinkedList<TranslatedExpression> copy = new LinkedList<>();

        TranslatedExpression prev = null;
        for ( int i = trans_list.size()-1; i >= 0; i-- ){
            TranslatedExpression curr = trans_list.get(i);

            if ( i+1 < trans_list.size() && prev.isNegative() && !prev.isSummationSymbol() ){
                if ( curr.isSummationSymbol() ){
                    if ( curr.isPositive() ){
                        copy.addFirst( new TranslatedExpression(MINUS_SIGN, POSITIVE ) );
                    } else {
                        copy.addFirst( new TranslatedExpression(PLUS_SIGN) );
                    }
                    prev = curr;
                    continue;
                } else {
                    TranslatedExpression e = copy.removeFirst();
                    e.changeExpression( Brackets.left_parenthesis.symbol +
                            MINUS_SIGN + e.getPlainExpression() +
                            Brackets.left_parenthesis.counterpart
                    );
                    copy.addFirst(e);
                }
            }

            TranslatedExpression c = new TranslatedExpression( curr );
            copy.addFirst( c );
            prev = curr;
        }

        if ( trans_list.getFirst().isNegative() ){
            TranslatedExpression t = copy.removeFirst();
            t.changeExpression(MINUS_SIGN + t.getPlainExpression());
            copy.addFirst(t);
        }

        String str = "";
        String tmp;
        while ( !copy.isEmpty() ){
            tmp = copy.removeFirst().getPlainExpression();
            if ( whiteSpaceCheck(tmp) )
                str += tmp + GlobalConstants.WHITESPACE;
            else str += tmp;
        }

        if ( isEmbraced() )
            str = brackets.symbol + str + brackets.counterpart;

        if ( isNegative() )
            str = MINUS_SIGN + str;

        return str;
    }

    private boolean whiteSpaceCheck( String s ){
        Matcher m = LATEX_COMMAND_PATTERN.matcher( s );
        return m.matches();
    }

    @Override
    @Deprecated
    public String toString(){
        String str = "";
        for ( TranslatedExpression s : trans_list )
            str += s.toString();
        return str;
    }
}
