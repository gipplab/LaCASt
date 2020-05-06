package gov.nist.drmf.interpreter.mlp;

import gov.nist.drmf.interpreter.common.grammar.ExpressionTags;
import mlp.MathTerm;
import mlp.PomTaggedExpression;

import java.util.List;

/**
 * @author Andre Greiner-Petter
 */
public abstract class PomTaggedExpressionUtility {

    private PomTaggedExpressionUtility() {}

    public static boolean isSequence(PomTaggedExpression pte) {
        if ( pte == null || pte.isEmpty() ) return false;
        ExpressionTags tag = ExpressionTags.getTagByKey(pte.getTag());
        return ExpressionTags.sequence.equals(tag);
    }

    /**
     * True if the given expression is accented (e.g. é).
     * @param pte the expression component
     * @return true if it has an accent
     */
    public static boolean isAccented( PomTaggedExpression pte ) {
        List<String> tags = pte.getSecondaryTags();
        for ( String t : tags ) {
            if ( t.matches(ExpressionTags.accented.tag()) ) {
                return true;
            }
        }

        MathTerm mt = pte.getRoot();
        List<String> mtags = mt.getSecondaryTags();
        for ( String t : mtags ) {
            if ( t.matches(ExpressionTags.accented.tag()) ) {
                return true;
            }
        }

        return false;
    }

    public static boolean equals(PomTaggedExpression pte, ExpressionTags tag) {
        if ( pte == null || tag == null ) return false;
        ExpressionTags t = ExpressionTags.getTagByKey(pte.getTag());
        return tag.equals(t);
    }
}
