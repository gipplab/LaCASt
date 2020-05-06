package gov.nist.drmf.interpreter.cas.translation.components.util;

import gov.nist.drmf.interpreter.cas.blueprints.BlueprintMaster;
import gov.nist.drmf.interpreter.cas.blueprints.Limits;
import gov.nist.drmf.interpreter.cas.translation.AbstractTranslator;
import gov.nist.drmf.interpreter.common.exceptions.TranslationException;
import gov.nist.drmf.interpreter.common.exceptions.TranslationExceptionReason;
import gov.nist.drmf.interpreter.common.grammar.ExpressionTags;
import gov.nist.drmf.interpreter.common.grammar.MathTermTags;
import mlp.MathTerm;
import mlp.PomTaggedExpression;

import java.util.List;

/**
 * @author Andre Greiner-Petter
 */
public class LimitAnalyzer {

    public LimitAnalyzer() {}

    public Limits extractLimitsWithoutParsing(
            PomTaggedExpression limitSuperExpr,
            List<PomTaggedExpression> upperBound,
            boolean lim,
            BlueprintMaster btm,
            AbstractTranslator parentTranslator) {
        PomTaggedExpression limitExpression = getLowerUpper(limitSuperExpr, upperBound, parentTranslator, false);

        // now we have limitExpression and an optional upperBound. Parse it:
        return btm.findMatchingLimit(lim, limitExpression);
    }

    public PomTaggedExpression getLowerUpper(
            PomTaggedExpression limitSuperExpr,
            List<PomTaggedExpression> upperBound,
            AbstractTranslator parentTranslator,
            boolean allowIndefinite
    ) {
        MathTerm term = limitSuperExpr.getRoot();

        PomTaggedExpression limitExpression = null;

        // in case it is a MathTerm, it MUST be a lower bound!
        if ( term != null && !term.isEmpty() ) {
            MathTermTags tag = MathTermTags.getTagByKey(term.getTag());
            if ( !tag.equals(MathTermTags.underscore) ) {
                if ( allowIndefinite ) return null;
                else throw TranslationException.buildException(
                        parentTranslator,
                        "Illegal expression followed a limited expression: " + term.getTermText(),
                        TranslationExceptionReason.INVALID_LATEX_INPUT);
            }
            // underscore always has only one child!
            limitExpression = limitSuperExpr.getComponents().get(0);
        } else {
            String tagS = limitSuperExpr.getTag();
            ExpressionTags tag = ExpressionTags.getTagByKey(tagS);
            if ( tag.equals(ExpressionTags.sub_super_script) ) {
                List<PomTaggedExpression> els = limitSuperExpr.getComponents();
                for ( PomTaggedExpression pte : els ) {
                    MathTermTags t = MathTermTags.getTagByKey(pte.getRoot().getTag());
                    if ( t.equals(MathTermTags.underscore) ) {
                        limitExpression = pte.getComponents().get(0);
                    } else if ( t.equals(MathTermTags.caret) ) {
                        upperBound.addAll(pte.getComponents());
                    }
                }
            } else {
                if ( allowIndefinite ) return null;
                else throw TranslationException.buildException( parentTranslator,
                        "A limited expression without limits is not allowed: " + term.getTermText(),
                        TranslationExceptionReason.INVALID_LATEX_INPUT);
            }
        }

        return limitExpression;
    }
}
