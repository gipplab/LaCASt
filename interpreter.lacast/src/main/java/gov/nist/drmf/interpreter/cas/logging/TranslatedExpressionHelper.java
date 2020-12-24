package gov.nist.drmf.interpreter.cas.logging;

import gov.nist.drmf.interpreter.common.TranslationInformation;

import java.util.LinkedList;
import java.util.List;

/**
 * @author Andre Greiner-Petter
 */
public class TranslatedExpressionHelper {

    private final ElementCache elementCache;
    private final String multiplyChar;
    private LinkedList<String> cache;
    private LinkedList<String> innerCache;

    public TranslatedExpressionHelper(
            String multiplyChar,
            LinkedList<String> cache
    ){
        this.multiplyChar = multiplyChar;
        this.cache = cache;
        this.innerCache = new LinkedList<>();

        boolean prevElementEndsWithMultiply =
                TranslatedExpressionHelper.endsWithMultiply(cache.getLast(), multiplyChar);
        this.elementCache = new ElementCache(prevElementEndsWithMultiply, false);;
    }

    public LinkedList<String> getInnerCache() {
        return innerCache;
    }

    public static boolean hit(String element, List<String> vars) {
        StringBuilder varPattern = TranslatedExpressionHelper.getVarPattern(vars);
        return element.matches("^(?:.*[^\\p{Alpha}]|\\s*)" + varPattern + "(?:[^\\p{Alpha}].*|\\s*)$");
    }

    public void handleElement(String element, List<String> var) {
        if ( TranslatedExpressionHelper.hit(element, var) ) handleElementHit(element);
        else handleNonHitElement(element);
    }

    public void handleElementHit(
            String element
    ) {
        // contains element! so add it, but first, add remaining inner cache, if existing
        while ( !innerCache.isEmpty() ) {
            cache.addLast(innerCache.removeFirst());
        } // now, inner cache is clean. add new element
        cache.addLast(element);
        elementCache.setPrevElementInnerCache(false);
        elementCache.setPrevElementEndsWithMultiply(endsWithMultiply(element, multiplyChar));
    }

    public void handleNonHitElement(String element) {
        // in case it DOES not contain a var... move on, maybe it comes later
        // the previous element stops with a multiply symbol, so it is part of the argument:
        if ( elementCache.isPrevElementEndsWithMultiply() ) {
            ifPrevEndsWithMultiply(element);
        } else if ( element.matches("\\s*[+-]\\s*") ){
            // next element is + or -... so fill up inner cache
            innerCache.addLast(element);
            elementCache.setPrevElementEndsWithMultiply(false);
            elementCache.setPrevElementInnerCache(true);
        } else if ( element.matches("\\s*[/*]\\s*") ){
            // multiply symbols may appear isolated in single elements. If so treat them as a multiply
            if ( elementCache.isPrevElementInnerCache() )
                innerCache.addLast(element);
            else cache.addLast(element);
            // note, prevElementInnerCache does not change here... of course
            elementCache.setPrevElementEndsWithMultiply(true);
        } else {
            // in any other case, its something not related, so just do the normal work
            innerCache.addLast(element);
            elementCache.setPrevElementEndsWithMultiply(endsWithMultiply(element, multiplyChar));
            elementCache.setPrevElementInnerCache(true);
        }
    }

    private void ifPrevEndsWithMultiply(String element) {
        if ( elementCache.isPrevElementInnerCache() ) {
            // the previous element went to innerCache, so fill up the innerCache
            innerCache.addLast(element);
        } else { // otherwise the previous element went to the cache directly, so put it there
            cache.addLast(element);
        }
        // note, here also, prev element does not change
        elementCache.setPrevElementEndsWithMultiply(endsWithMultiply(element, multiplyChar));
    }

    /**
     * TODO ! not yet used the real multiply symbol
     * @param expression
     * @param multiply
     * @return
     */
    private static boolean endsWithMultiply(String expression, String multiply) {
        return expression.matches(".*["+multiply+"/]\\s*");
    }

    public static StringBuilder getVarPattern(List<String> var) {
        StringBuilder varPattern = new StringBuilder("(");
        for ( int i = 0; i < var.size()-1; i++ ) {
            varPattern.append("\\Q").append(var.get(i)).append("\\E");
            varPattern.append("|");
        }
        varPattern.append("\\Q").append(var.get(var.size()-1)).append("\\E)");
        return varPattern;
    }

    public static void addTranslatedExpressionInformation(TranslatedExpression te, TranslationInformation ti) {
        ti.setTranslatedExpression( te.getTranslatedExpression() );
        ti.setRequiredPackages( te.getRequiredPackages() );
        ti.setFreeVariables( te.getFreeVariables() );
        ti.setRelationalComponents( te.getAllRelationalComponents() );
        ti.addTranslatedConstraints( te.getConstraints() );
    }
}
