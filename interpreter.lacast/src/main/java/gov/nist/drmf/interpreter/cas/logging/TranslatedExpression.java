package gov.nist.drmf.interpreter.cas.logging;

import gov.nist.drmf.interpreter.common.grammar.Brackets;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * @author Andre Greiner-Petter
 */
public class TranslatedExpression {
    private LinkedList<String> trans_exps;

    private int autoMergeLast;

    public TranslatedExpression(){
        this.trans_exps = new LinkedList<>();
        this.autoMergeLast = 0;
    }

    public void setAutoMergeLast( int num_of_last ){
        autoMergeLast = num_of_last;
    }

    public void addAutoMergeLast( int add_num_of_last ){
        autoMergeLast += add_num_of_last;
    }

    private String autoMergeLast(){
        String last_elems = "";
        for ( ; autoMergeLast > 0; autoMergeLast-- ){
            if ( trans_exps.isEmpty() ) break;
            last_elems += trans_exps.removeLast();
        }
        return last_elems;
    }

    public void addTranslatedExpression( String trans_exp ){
        this.trans_exps.add( autoMergeLast() + trans_exp );
    }

    public void addTranslatedExpression( TranslatedExpression expressions ){
        this.autoMergeLast += expressions.autoMergeLast;
        String next = autoMergeLast();
        if ( next.isEmpty() ){
            this.trans_exps.addAll( expressions.trans_exps );
            return;
        }
        if ( !expressions.trans_exps.isEmpty() )
            next += expressions.trans_exps.removeFirst();
        this.trans_exps.add( next );
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
        for( int i = 0; i < n && !trans_exps.isEmpty(); i++ ){
            tmp.add(removeLastExpression());
        }
        while ( !tmp.isEmpty() )
            sub.addTranslatedExpression( tmp.removeLast() );
        return sub;
    }

    public void mergeLastNExpressions( int n ){
        if ( n > trans_exps.size() ){
            mergeAll();
            return;
        }
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
        autoMergeLast = 0;
        return length;
    }

    public int mergeAllWithParenthesis(){
        String tmp = Brackets.left_parenthesis.symbol;
        int i = trans_exps.size();
        while ( !trans_exps.isEmpty() ){
            tmp += trans_exps.removeFirst();
        }
        trans_exps.add( tmp + Brackets.left_parenthesis.counterpart );
        autoMergeLast = 0;
        return i;
    }

    public String removeLastExpression(){
        if ( !trans_exps.isEmpty() ) return trans_exps.removeLast();
        else return null;
    }

    public String getLastExpression(){
        if ( trans_exps.isEmpty() ) return null;
        else return trans_exps.getLast();
    }

    public void replaceLastExpression( String new_exp ){
        if ( !trans_exps.isEmpty() )
            trans_exps.removeLast();
        trans_exps.add(new_exp);
    }

    /**
     * This method extracts all terms that contains the given variables in {@param var}.
     * The strategy is as following. Note that the first element is always part of the argument.
     *
     * @param var
     * @param multiplyChar
     * @return
     */
    public TranslatedExpression removeUntilLastAppearanceOfVar(List<String> var, String multiplyChar) {
        TranslatedExpression cache = new TranslatedExpression();

        // first element is ALWAYS part of the argument
        cache.trans_exps.addFirst(trans_exps.removeFirst());

        if ( multiplyChar.matches("\\*") ) {
            multiplyChar = "\\*";
        }

        // check the rest of it
        // does the previous element ends with a multiplication symbol?
        TranslatedExpressionHelper helper = new TranslatedExpressionHelper(multiplyChar, cache.trans_exps);

        while ( !trans_exps.isEmpty() ){
            helper.handleElement(trans_exps.removeFirst(), var);
        }

        LinkedList<String> innerCache = helper.getInnerCache();
        // otherwise, roll back inner cache expressions
        while ( !innerCache.isEmpty() ) {
            // be careful, reverse order here
            this.trans_exps.addFirst(innerCache.removeLast());
        }
        return cache;
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

    public String debugString() {
        return trans_exps.toString();
    }
}
