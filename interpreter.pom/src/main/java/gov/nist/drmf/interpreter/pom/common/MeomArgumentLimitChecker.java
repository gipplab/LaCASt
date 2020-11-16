package gov.nist.drmf.interpreter.pom.common;

import gov.nist.drmf.interpreter.pom.common.grammar.Brackets;
import gov.nist.drmf.interpreter.pom.common.grammar.MathTermTags;
import mlp.MathTerm;
import mlp.PomTaggedExpression;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author Andre Greiner-Petter
 */
public class MeomArgumentLimitChecker {
    private static final Logger LOG = LogManager.getLogger(MeomArgumentLimitChecker.class.getName());

    private MeomArgumentLimitChecker() {}

    /**
     * Returns true if the given math term is a break point.
     * If possible, call {@link #isBreakPoint(PomTaggedExpression)} instead
     * for more secure check.
     * @param mt the math term that might be a break point
     * @return true if the given term is a break point
     */
    public static boolean isGeneralBreakPoint(MathTerm mt) {
        MathTermTags tag = MathTermTags.getTagByKey(mt.getTag());
        if ( tag == null ) return false;
        // stop only in case of a harsh stop symbol appears on the same level of the sum
        // stoppers are relations (left-hand side and right-hand side).
        switch (tag) {
            case relation:
            case equals:
            case less_than:
            case greater_than:
                // found stopper -> return the cache
                LOG.debug("Limited expression breakpoint reached (reason: relation)");
                return true;
            default:
                return false;
        }
    }

    /**
     * Returns true if the given expression is a breakpoint
     * @param pte the expression that might be a breakpoint
     * @return true if the given expression marks a breakpoint
     */
    public static boolean isBreakPoint(PomTaggedExpression pte) {
        if ( pte == null || pte.isEmpty() ) return true;
        if ( isGeneralBreakPoint(pte.getRoot()) ) return true;
        Brackets bracket = Brackets.getBracket(pte);
        return bracket != null && !bracket.opened;
    }

    /**
     * Returns true if the given expression is a potential limit breakpoint
     * like the alphanumeric expression "dx".
     * @param pte the expression to check
     * @return true if the given expression is a look-alike differential expression
     */
    public static boolean isPotentialLimitBreakpoint(PomTaggedExpression pte) {
        MathTermTags tag = MathTermTags.getTagByExpression(pte);
        if ( MathTermTags.alphanumeric.equals(tag) ) {
            String termText = pte.getRoot().getTermText();
            return termText.matches("^d[a-zA-Z]+");
        }
        return false;
    }
}
