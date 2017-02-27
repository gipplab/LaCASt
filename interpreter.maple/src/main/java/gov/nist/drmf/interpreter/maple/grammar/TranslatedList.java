package gov.nist.drmf.interpreter.maple.grammar;

import gov.nist.drmf.interpreter.common.grammar.Brackets;
import gov.nist.drmf.interpreter.maple.parser.MapleInterface;

import java.util.LinkedList;

/**
 * Created by AndreG-P on 24.02.2017.
 */
public class TranslatedList extends TranslatedExpression {

    private LinkedList<TranslatedExpression> trans_list;

    private Brackets brackets;

    public TranslatedList(){
        super("");
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

    public TranslatedExpression removeLastExpression(){
        return trans_list.removeLast();
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
        LinkedList<TranslatedExpression> copy = new LinkedList<>();

        TranslatedExpression prev = null;
        for ( int i = trans_list.size()-1; i >= 0; i-- ){
            TranslatedExpression curr = trans_list.get(i);

            if ( i+1 < trans_list.size() && prev.isNegative() ){
                if ( curr.isSummationSymbol() ){
                    if ( curr.getPlainExpression().matches("\\s*\\+\\s*") ){
                        copy.addFirst( new TranslatedExpression(" - ") );
                    } else {
                        copy.addFirst( new TranslatedExpression(" + ") );
                    }
                    prev = curr;
                    continue;
                } else {
                    TranslatedExpression e = copy.removeFirst();
                    e.changeExpression( "(-" + e.getPlainExpression() + ")" );
                    copy.addFirst(e);
                }
            }

            TranslatedExpression c = new TranslatedExpression( curr.getPlainExpression(), curr.isPositive() );
            copy.addFirst( c );
            prev = curr;
        }

        if ( trans_list.getFirst().isNegative() ){
            TranslatedExpression t = copy.removeFirst();
            t.changeExpression("-" + t.getPlainExpression());
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
