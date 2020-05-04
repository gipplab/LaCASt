package gov.nist.drmf.interpreter.mlp;

import gov.nist.drmf.interpreter.common.grammar.ExpressionTags;
import mlp.PomTaggedExpression;

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
}
