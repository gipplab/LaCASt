package gov.nist.drmf.interpreter.cas.logging;

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
