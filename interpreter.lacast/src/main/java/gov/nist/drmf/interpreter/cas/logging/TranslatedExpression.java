package gov.nist.drmf.interpreter.cas.logging;

import gov.nist.drmf.interpreter.common.grammar.Brackets;

import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * @author Andre Greiner-Petter
 */
public class TranslatedExpression {
    public LinkedList<String> trans_exps;

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
     * 1)
     *
     * @param var
     * @param multiplyChar
     * @return
     */
    public TranslatedExpression removeUntilLastAppearanceOfVar(List<String> var, String multiplyChar) {
        StringBuilder varPattern = new StringBuilder("(");
        for ( int i = 0; i < var.size()-1; i++ ) {
            varPattern.append(var.get(i)).append("|");
        }
        varPattern.append(var.get(var.size() - 1)).append(")");

        TranslatedExpression cache = new TranslatedExpression();

        // first element is ALWAYS part of the argument
        cache.trans_exps.addFirst(trans_exps.removeFirst());

        if ( multiplyChar.matches("\\*") ) {
            multiplyChar = "\\*";
        }

        // check the rest of it
        // does the previous element ends with a multiplication symbol?
        boolean prevElementEndsWithMultiply = endsWithMultiply(cache.trans_exps.getLast(), multiplyChar);
        boolean prevElementInnerCache = false;

        LinkedList<String> innerCache = new LinkedList<>();

        while ( !trans_exps.isEmpty() ){
            String element = trans_exps.removeFirst();
            if (    element.matches("^(?:.*[^\\p{Alpha}]|\\s*)" + varPattern + "(?:[^\\p{Alpha}].*|\\s*)$") //||
                    //element.matches("^\\s*" + varPattern + "\\s*$")
                    ) {
                // contains element! so add it, but first, add remaining inner cache, if existing
                while ( !innerCache.isEmpty() ) {
                    cache.trans_exps.addLast(innerCache.removeFirst());
                } // now, inner cache is clean. add new element
                cache.trans_exps.addLast(element);
                prevElementInnerCache = false;
                prevElementEndsWithMultiply = endsWithMultiply(element, multiplyChar);
            } else { // in case it DOES not contain a var... move on, maybe it comes later
                // the previous element stops with a multiply symbol, so it is part of the argument:
                if ( prevElementEndsWithMultiply ) {
                    if ( prevElementInnerCache ) {
                        // the previous element went to innerCache, so fill up the innerCache
                        innerCache.addLast(element);
                    } else { // otherwise the previous element went to the cache directly, so put it there
//                        //
//                        while ( !innerCache.isEmpty() ) {
//                            cache.trans_exps.addLast(innerCache.removeFirst());
//                        } // now, inner cache is clean. add new element
                        cache.trans_exps.addLast(element);
                    }
                    // note, here also, prev element does not change
                    prevElementEndsWithMultiply = endsWithMultiply(element, multiplyChar);
                } else if ( element.matches("\\s*[+-]\\s*") ){
                    // next element is + or -... so fill up inner cache
                    innerCache.addLast(element);
                    prevElementEndsWithMultiply = false;
                    prevElementInnerCache = true;
                } else if ( element.matches("\\s*[/*]\\s*") ){
                    // multiply symbols may appear isolated in single elements. If so treat them as a multiply
                    if ( prevElementInnerCache )
                        innerCache.addLast(element);
                    else cache.trans_exps.addLast(element);
                    // note, prevElementInnerCache does not change here... of course
                    prevElementEndsWithMultiply = true;
                } else {
                    // in any other case, its something not related, so just do the normal work
                    innerCache.addLast(element);
                    prevElementInnerCache = true;
                    prevElementEndsWithMultiply = endsWithMultiply(element, multiplyChar);
                }
            }


//            if ( prevElementEndsWithMultiply ) {
//                if ( prevElementInnerCache ) { // connects to inner cache, not normal cache
//                    innerCache.addLast(element);
//                    prevElementInnerCache = true;
//                } else { // connects to cache element
//                    while ( !innerCache.isEmpty() ) { // contains element! so add it, but first, add remaining inner cache, if existing
//                        cache.trans_exps.addLast(innerCache.removeFirst());
//                    } // now, inner cache is clean. add new element
//                    cache.trans_exps.addLast(element);
//                    prevElementInnerCache = false;
//                }
//                prevElementEndsWithMultiply = endsWithMultiply(element, multiplyChar);
//            } else if ( element.matches("\\s*([+-])\\s*") ){
//                // its multiple or so, so add if next element is a part
//                innerCache.addLast(element);
//            } else { // now, check if var exists
//                // the var must be a subexpression isolated from other letters (otherwise m appears in sum)
//                if ( element.matches(".*[^\\p{Alpha}]" + varPattern + "[^\\p{Alpha}].*") ||
//                        element.matches("^\\s*" + varPattern + "\\s*$")) {
//                    // contains element! so add it, but first, add remaining inner cache, if existing
//                    while ( !innerCache.isEmpty() ) {
//                        cache.trans_exps.addLast(innerCache.removeFirst());
//                    } // now, inner cache is clean. add new element
//                    cache.trans_exps.addLast(element);
//                    prevElementInnerCache = false;
//                    prevElementEndsWithMultiply = endsWithMultiply(element, multiplyChar);
//                } else { // next element is NOT included! but maybe a later it comes...
//                    // so put it into the inner cache
//                    innerCache.addLast(element);
//                    prevElementInnerCache = true;
//                    prevElementEndsWithMultiply = endsWithMultiply(element, multiplyChar);
//                }
//            }
        }

        if ( innerCache.isEmpty() ) {
            // well, all were part of the sum, so we are done here
            return cache;
        }

        // otherwise, roll back inner cache expressions
        while ( !innerCache.isEmpty() ) {
            // be careful, reverse order here
            this.trans_exps.addFirst(innerCache.removeLast());
        }

        return cache;
    }

    /**
     * TODO ! not yet used the real multiply symbol
     * @param expression
     * @param multiply
     * @return
     */
    private boolean endsWithMultiply(String expression, String multiply) {
        return expression.matches(".*["+multiply+"/]\\s*");
    }

    public static void main(String[] args) {
        String expression = "2*";
        System.out.println(expression.matches(".*\\*\\s*"));
    }

    public String getTranslatedExpression(){
        String output = "";
        for ( String part : trans_exps )
            output += part;
        return output;
    }

    public String debugToString() {
        return trans_exps.toString();
    }

    @Override
    public String toString(){
        return getTranslatedExpression();
    }

    public String debugString() {
        return trans_exps.toString();
    }
}
