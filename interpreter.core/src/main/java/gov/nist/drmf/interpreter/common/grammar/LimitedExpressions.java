package gov.nist.drmf.interpreter.common.grammar;

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

    public String getDirectionKey(LimDirections dir) {
        return name().toLowerCase() + dir.getKey();
    }

    public static LimitedExpressions getExpression(MathTerm mt) {
        if ( !isLimitedExpression(mt) ) return null;

        String title = mt.getTermText().substring(1);
        for ( LimitedExpressions l : LimitedExpressions.values() ){
            if ( l.name().toLowerCase().equals(title) ) return l;
        }
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
            return mt.getTermText().matches("\\\\(?:sum|prod|int|lim)");
        }
        return false;
    }

    public static boolean isSum(MathTerm term){
        return term.getTermText().equals("\\sum");
    }

    public static boolean isProduct(MathTerm term){
        return term.getTermText().equals("\\prod");
    }

    public static boolean isIntegral(MathTerm term){
        return term.getTermText().equals("\\int");
    }

    public static boolean isLimit(MathTerm term){
        return term.getTermText().equals("\\lim");
    }
}
