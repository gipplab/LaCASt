package gov.nist.drmf.interpreter.cas.logging;

import gov.nist.drmf.interpreter.common.cas.PackageWrapper;
import gov.nist.drmf.interpreter.common.meta.ListExtender;
import gov.nist.drmf.interpreter.pom.common.grammar.Brackets;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Andre Greiner-Petter
 */
public class TranslatedExpression {
    private LinkedList<String> trans_exps;
    private final Set<String> requiredPackages;

    private int latestRelationSymbol;

    private List<String> constraints;

    private int negativeReplacements;

    private int autoMergeLast;

    public TranslatedExpression(){
        this.trans_exps = new LinkedList<>();
        this.autoMergeLast = 0;
        this.requiredPackages = new TreeSet<>();
        this.negativeReplacements = 0;
        this.constraints = new LinkedList<>();
        this.latestRelationSymbol = -1;
    }

    public TranslatedExpression(TranslatedExpression copy) {
        this.trans_exps = new LinkedList<>(copy.trans_exps);
        this.autoMergeLast = copy.autoMergeLast;
        this.requiredPackages = new TreeSet<>(copy.requiredPackages);
        this.negativeReplacements = copy.negativeReplacements;
        this.constraints = new LinkedList<>(copy.constraints);
        this.latestRelationSymbol = copy.latestRelationSymbol;
    }

    public boolean endedOnRelationSymbol() {
        return this.latestRelationSymbol == trans_exps.size()-1;
    }

    public boolean containsRelationSymbol() {
        return this.latestRelationSymbol >= 0;
    }

    public void tagLastElementAsRelation(){
        this.latestRelationSymbol = Math.max(0, trans_exps.size()-1);
    }

    public TranslatedExpression getElementsAfterRelation() {
        TranslatedExpression te = new TranslatedExpression();
        te.autoMergeLast = this.autoMergeLast;
        te.requiredPackages.addAll(this.requiredPackages);
        te.negativeReplacements = this.negativeReplacements;
        te.constraints.addAll(this.constraints);
        if ( this.latestRelationSymbol < trans_exps.size() )
            te.trans_exps.addAll(this.trans_exps.subList(this.latestRelationSymbol+1, trans_exps.size()));
        return te;
    }

    public void addConstraint(String constraint) {
        this.constraints.add(constraint);
    }

    public void tagLastNExpressionsToConstraint(int n) {
        LinkedList<String> constraintElements = new LinkedList<>();
        for ( int i = 0; i < n && !trans_exps.isEmpty(); i++ ) {
            constraintElements.addFirst( trans_exps.removeLast() );
        }
        addConstraint( String.join("", constraintElements) );
    }

    public List<String> getConstraints() {
        return this.constraints;
    }

    public void setNegativeReplacements(int length) {
        this.negativeReplacements = length;
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
        if ( expressions.negativeReplacements > 0 && trans_exps.size() >= expressions.negativeReplacements ) {
            List<String> tmp = trans_exps.subList(0, trans_exps.size() - expressions.negativeReplacements);
            this.trans_exps = new LinkedList<>(tmp);
        }

        if ( this.latestRelationSymbol < expressions.latestRelationSymbol )
            this.latestRelationSymbol = expressions.latestRelationSymbol;
        this.requiredPackages.addAll(expressions.getRequiredPackages());
        ListExtender.addIfNotExist(constraints, expressions.constraints);

        this.autoMergeLast += expressions.autoMergeLast;
        String next = autoMergeLast();
        if ( next.isEmpty() ){
            this.trans_exps.addAll( expressions.trans_exps );
            return;
        }

        if ( !expressions.trans_exps.isEmpty() ) {
            next += expressions.trans_exps.removeFirst();
        }

        this.trans_exps.add( next );
        this.trans_exps.addAll( expressions.trans_exps );
    }

    public int getLength(){
        return trans_exps.size();
    }

    public int clear(){
        int s = trans_exps.size();
        trans_exps = new LinkedList<>();
        requiredPackages.clear();
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

        cache.addRequiredPackages(requiredPackages);
        return cache;
    }

    public TranslatedExpression removeUntilFirstAppearanceOfVar(List<String> var, String multiply) {
        TranslatedExpression te = new TranslatedExpression();

        if ( trans_exps.isEmpty() ) return te;

        int latestHitIdx = trans_exps.size();
        for ( int i = trans_exps.size()-1; i >= 0; i-- ) {
            String element = trans_exps.get(i);

            if ( element.matches("^.*[=<>.,;\n\t]\\s*$") ) break;
            if ( TranslatedExpressionHelper.hit(element, var) ) latestHitIdx = i;
        }

        if ( latestHitIdx == trans_exps.size() ) return te;

        String lastElement = trans_exps.getLast();
        Pattern multiplyPattern = Pattern.compile("^(.*)\\Q"+ multiply +"\\E\\s*$");
        Matcher m = multiplyPattern.matcher(lastElement);
        if ( m.matches() ) {
            lastElement = m.group(1);
            trans_exps.removeLast();
            trans_exps.addLast(lastElement);
        }

        List<String> elementsPointer = trans_exps.subList(latestHitIdx, trans_exps.size());
        te.trans_exps.addAll(elementsPointer);
        te.negativeReplacements = te.trans_exps.size();

        return te;
    }

    public String getTranslatedExpression(){
        return String.join("", trans_exps);
    }

    public String getTranslatedExpression(PackageWrapper pw) {
        if ( requiredPackages.isEmpty() ) return getTranslatedExpression();
        return pw.addPackages(getTranslatedExpression(), requiredPackages);
    }

    public String[] splitOn(String splitter) {
        LinkedList<List<String>> parts = new LinkedList<>();
        parts.add(new LinkedList<>());

        for ( String p : trans_exps ) {
            if ( p != null && !p.isBlank() && p.matches("\\s*"+splitter+"\\s*") ) {
                parts.addLast(new LinkedList<>());
                continue;
            }

            parts.getLast().add(p);
        }

        String[] res = new String[parts.size()];
        for ( int i = 0; i < parts.size(); i++ ) {
            res[i] = String.join("", parts.get(i));
        }

        return res;
    }

    public void addRequiredPackages(Collection<String> requiredPackages) {
        this.requiredPackages.addAll(requiredPackages);
    }

    public Set<String> getRequiredPackages() {
        return requiredPackages;
    }

    @Override
    public String toString(){
        return getTranslatedExpression();
    }

    public String debugString() {
        return trans_exps.toString();
    }
}
