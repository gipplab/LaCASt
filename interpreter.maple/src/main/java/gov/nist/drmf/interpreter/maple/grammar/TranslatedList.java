package gov.nist.drmf.interpreter.maple.grammar;

import static gov.nist.drmf.interpreter.maple.common.MapleConstants.*;

import gov.nist.drmf.interpreter.common.grammar.Brackets;
import gov.nist.drmf.interpreter.maple.parser.MapleInterface;

import java.util.LinkedList;

/**
 * TODO should we handle spaces here as well? I think so.
 *
 * Created by AndreG-P on 24.02.2017.
 */
public class TranslatedList extends TranslatedExpression {

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
        this.addTranslatedExpression( list.merge() );
    }

    public void addPreviousTranslatedExpression( String expression ){
        TranslatedExpression t = new TranslatedExpression( expression );
        this.addPreviousTranslatedExpression(t);
    }

    public void addPreviousTranslatedExpression( TranslatedExpression te ){
        this.trans_list.addFirst( te );
    }

    public void addPreviousTranslatedExpression( TranslatedList list ){
        this.addPreviousTranslatedExpression( list.merge() );
    }

    public TranslatedExpression removeLastExpression(){
        return trans_list.removeLast();
    }

    public TranslatedExpression removePreviousTranslatedExpression(){
        return trans_list.removeFirst();
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
        String s = getAccurateString();
        return new TranslatedExpression( s, this.isPositive() );
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
                        copy.addFirst( new TranslatedExpression( NEGATIVE_SIGN, POSITIVE ) );
                    } else {
                        copy.addFirst( new TranslatedExpression( POSITIVE_SIGN ) );
                    }
                    prev = curr;
                    continue;
                } else {
                    TranslatedExpression e = copy.removeFirst();
                    e.changeExpression( Brackets.left_parenthesis.symbol +
                            NEGATIVE_SIGN + e.getPlainExpression() +
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
            t.changeExpression(NEGATIVE_SIGN + t.getPlainExpression());
            copy.addFirst(t);
        }

        String str = "";
        while ( !copy.isEmpty() )
            str += copy.removeFirst().getPlainExpression();

        if ( isEmbraced() )
            str = brackets.symbol + str + brackets.counterpart;
        if ( isNegative() ){
            if ( !isEmbraced() ) {
                str = MapleInterface.DEFAULT_LATEX_BRACKET.symbol + str;
                str += MapleInterface.DEFAULT_LATEX_BRACKET.counterpart;
            }
            str = "-" + str;
        }

        return str;
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
