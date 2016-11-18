package gov.nist.drmf.interpreter.cas.logging;

import java.util.LinkedList;

/**
 * @author Andre Greiner-Petter
 */
public class TranslatedExpression {
    private LinkedList<String> trans_exps;

    public TranslatedExpression(){
        this.trans_exps = new LinkedList<>();
    }

    public void addTranslatedExpression(String trans_exp){
        this.trans_exps.add(trans_exp);
    }

    public void addTranslatedExpression( TranslatedExpression expressions ){
        this.trans_exps.addAll( expressions.trans_exps );
    }

    public String removeLastExpression(){
        return trans_exps.removeLast();
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
