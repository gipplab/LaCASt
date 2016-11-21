package gov.nist.drmf.interpreter.cas.logging;

import gov.nist.drmf.interpreter.common.grammar.Brackets;

import java.util.LinkedList;

/**
 * @author Andre Greiner-Petter
 */
public class TranslatedExpression {
    public LinkedList<String> trans_exps;

    public TranslatedExpression(){
        this.trans_exps = new LinkedList<>();
    }

    public void addTranslatedExpression(String trans_exp){
        this.trans_exps.add(trans_exp);
    }

    public void addTranslatedExpression( TranslatedExpression expressions ){
        this.trans_exps.addAll( expressions.trans_exps );
    }

    public int getLength(){
        return trans_exps.size();
    }

    public int clear(){
        int s = trans_exps.size();
        trans_exps = new LinkedList<>();
        return s;
    }

    public TranslatedExpression removeLastNExps(int n){
        TranslatedExpression sub = new TranslatedExpression();
        LinkedList<String> tmp = new LinkedList<>();
        for( int i = 0; i < n; i++ ){
            tmp.add(removeLastExpression());
        }
        while ( !tmp.isEmpty() )
            sub.addTranslatedExpression( tmp.removeLast() );
        return sub;
    }

    public void mergeLastNExpressions( int n ){
        TranslatedExpression tmp = new TranslatedExpression();
        LinkedList<String> tmpList = new LinkedList<>();
        for ( int i = 0; i < n; i++ )
            tmpList.add( this.removeLastExpression() );
        while ( !tmpList.isEmpty() ){
            tmp.addTranslatedExpression( tmpList.removeLast() );
        }
        addTranslatedExpression( tmp.toString() );
    }

    public int mergeAll(){
        String tmp = toString();
        int length = trans_exps.size();
        trans_exps.clear();
        trans_exps.add( tmp );
        return length;
    }

    public int mergeAllWithParenthesis(){
        String tmp = Brackets.left_parenthesis.symbol;
        int i = trans_exps.size();
        while ( !trans_exps.isEmpty() ){
            tmp += trans_exps.removeFirst();
        }
        trans_exps.add( tmp + Brackets.left_parenthesis.counterpart );
        return i;
    }

    public String removeLastExpression(){
        return trans_exps.removeLast();
    }

    public String getLastExpression(){
        return trans_exps.getLast();
    }

    public void replaceLastExpression( String new_exp ){
        trans_exps.removeLast();
        trans_exps.add(new_exp);
    }

    public String getTranslatedExpression(){
        String output = "";
        for ( String part : trans_exps )
            output += part;
        return output;
    }

    @Override
    public String toString(){
        return getTranslatedExpression();
    }
}
