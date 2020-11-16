package gov.nist.drmf.interpreter.cas.translation.components.util;

import gov.nist.drmf.interpreter.cas.blueprints.BlueprintMaster;
import gov.nist.drmf.interpreter.cas.blueprints.MathematicalEssentialOperatorMetadata;
import gov.nist.drmf.interpreter.cas.translation.AbstractTranslator;
import gov.nist.drmf.interpreter.common.exceptions.InitTranslatorException;
import gov.nist.drmf.interpreter.common.exceptions.TranslationException;
import gov.nist.drmf.interpreter.common.exceptions.TranslationExceptionReason;
import gov.nist.drmf.interpreter.pom.common.grammar.ExpressionTags;
import gov.nist.drmf.interpreter.pom.common.grammar.MathTermTags;
import gov.nist.drmf.interpreter.pom.common.MathTermUtility;
import gov.nist.drmf.interpreter.pom.common.PomTaggedExpressionUtility;
import mlp.MathTerm;
import mlp.PomTaggedExpression;

import java.util.List;

/**
 * @author Andre Greiner-Petter
 */
public class LimitAnalyzer {

    private final BlueprintMaster btm;
    private final AbstractTranslator parentTranslator;

    public LimitAnalyzer(AbstractTranslator parentTranslator) {
        this.parentTranslator = parentTranslator;
        try {
            this.btm = parentTranslator.getConfig().getLimitParser();
        } catch (InitTranslatorException e) {
            throw TranslationException.buildException(
                    parentTranslator,
                    "Unable to load blueprint matcher",
                    TranslationExceptionReason.INSTANTIATION_ERROR
            );
        }
    }

    public MathematicalEssentialOperatorMetadata extractLimitsWithoutParsing(
            PomTaggedExpression limitSuperExpr,
            List<PomTaggedExpression> upperBound,
            boolean lim
    ) {
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

        // in case it is a MathTerm, it MUST be a lower bound!
        if ( !term.isEmpty() ) {
            return getLowerBound(term, allowIndefinite, parentTranslator, limitSuperExpr);
        }

        if ( PomTaggedExpressionUtility.equals(limitSuperExpr, ExpressionTags.sub_super_script) ) {
            return getUpperLowerBound(limitSuperExpr, upperBound);
        }

        if ( allowIndefinite ) return null;
        else throw TranslationException.buildException( parentTranslator,
                "A limited expression without limits is not allowed: " + term.getTermText(),
                TranslationExceptionReason.INVALID_LATEX_INPUT);
    }

    private PomTaggedExpression getUpperLowerBound (
            PomTaggedExpression limitSuperExpr,
            List<PomTaggedExpression> upperBound
    ) {
        PomTaggedExpression limitExpression = null;
        List<PomTaggedExpression> els = limitSuperExpr.getComponents();
        for ( PomTaggedExpression pte : els ) {
            MathTermTags t = MathTermTags.getTagByKey(pte.getRoot().getTag());
            if ( t.equals(MathTermTags.underscore) ) {
                limitExpression = pte.getComponents().get(0);
            } else if ( t.equals(MathTermTags.caret) ) {
                upperBound.addAll(pte.getComponents());
            }
        }
        return limitExpression;
    }

    private PomTaggedExpression getLowerBound(
            MathTerm term,
            boolean allowIndefinite,
            AbstractTranslator abstractTranslator,
            PomTaggedExpression limitSuperExpr
    ) {
        if ( !MathTermUtility.equals(term, MathTermTags.underscore) ) {
            if ( allowIndefinite ) return null;
            else throw TranslationException.buildException(
                    abstractTranslator,
                    "Illegal expression followed a limited expression: " + term.getTermText(),
                    TranslationExceptionReason.INVALID_LATEX_INPUT);
        }
        // underscore always has only one child!
        return limitSuperExpr.getComponents().get(0);
    }
}
