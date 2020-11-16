package gov.nist.drmf.interpreter.pom.common.grammar;

import mlp.MathTerm;

/**
 * @author Andre Greiner-Petter
 */
public enum LimitedExpressions {
    SUM,
    PROD,
    INT,
    LIM;

    public String getKey() {
        return name().toLowerCase();
    }

    public String getSetKey() {
        return name().toLowerCase() + "Set";
    }

    public String getIndefKey() {
        return name().toLowerCase() + "Indef";
    }

    public String getDirectionKey(LimDirections dir) {
        return name().toLowerCase() + dir.getKey();
    }

    public static LimitedExpressions getExpression(MathTerm mt) {
        if ( !isLimitedExpression(mt) ) return null;

        String title = mt.getTermText().substring(1);
        for ( LimitedExpressions l : LimitedExpressions.values() ){
            if ( l.name().toLowerCase().equals(title) ) return l;
        }

        if ( title.matches("i{1,4}nt") ) return INT;

        return null;
    }

    public static LimitedExpressions getExpression(String title) {
        for ( LimitedExpressions l : LimitedExpressions.values() ){
            if ( l.name().toLowerCase().equals(title) ) return l;
        }
        return null;
    }

    public static boolean isLimitedExpression(MathTerm mt) {
        MathTermTags mtag = MathTermTags.getTagByKey(mt.getTag());
        if (mtag != null && mtag.equals(MathTermTags.operator)) {
            return mt.getTermText().matches("\\\\(?:sum|prod|i{1,4}nt|lim)");
        }
        return false;
    }

    public static int getMultiIntDegree(MathTerm mt) {
        if ( !isLimitedExpression(mt) )
            throw new IllegalArgumentException("Requested int degree of a non-int expression.");

        String title = mt.getTermText().substring(1);
        for ( int pos = 0, i = 0; pos < title.length(); pos++ ) {
            if ( title.charAt(pos) == 'i' ) i++;
            else return i;
        }

        throw new IllegalArgumentException("Requested int degree of illegal expression.");
    }

    public static boolean isSum(MathTerm term){
        return term.getTermText().equals("\\sum");
    }

    public static boolean isProduct(MathTerm term){
        return term.getTermText().equals("\\prod");
    }

    public static boolean isIntegral(MathTerm term){
        return term.getTermText().matches("\\\\i{1,4}nt");
    }

    public static boolean isLimit(MathTerm term){
        return term.getTermText().equals("\\lim");
    }
}
