package gov.nist.drmf.interpreter.cas.translation.components;

import gov.nist.drmf.interpreter.cas.blueprints.BlueprintMaster;
import gov.nist.drmf.interpreter.cas.blueprints.MathematicalEssentialOperatorMetadata;
import gov.nist.drmf.interpreter.cas.logging.TranslatedExpression;
import gov.nist.drmf.interpreter.cas.translation.AbstractListTranslator;
import gov.nist.drmf.interpreter.cas.translation.AbstractTranslator;
import gov.nist.drmf.interpreter.cas.translation.components.util.LimitAnalyzer;
import gov.nist.drmf.interpreter.cas.translation.components.util.VariableExtractor;
import gov.nist.drmf.interpreter.common.exceptions.TranslationException;
import gov.nist.drmf.interpreter.common.exceptions.TranslationExceptionReason;
import gov.nist.drmf.interpreter.common.grammar.LimitedExpressions;
import gov.nist.drmf.interpreter.common.symbols.BasicFunctionsTranslator;
import gov.nist.drmf.interpreter.pom.FakeMLPGenerator;
import mlp.MathTerm;
import mlp.PomTaggedExpression;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.LinkedList;
import java.util.List;

/**
 * LimitedTranslator uses parseGeneralExpression to get the arguments to the sum/product.
 * Then it uses BasicFunctionParser to put the arguments where they need to go.
 *
 * Method call order: translate -> addToArgs -> onlyLower/lowerAndUpper -> addFactorsToSummand
 *
 * @author Andre Greiner-Petter
 * @author Rajen Dey
 *
 * July 2019
 */
public class LimitedTranslator extends AbstractListTranslator {
    private static final Logger LOG = LogManager.getLogger(LimitedTranslator.class.getName());

    // perform translation and put everything into global_exp
    private BasicFunctionsTranslator bft;
    private LimitAnalyzer limitAnalyzer;

    private final TranslatedExpression localTranslations;

    private boolean indef = false;

    public LimitedTranslator(AbstractTranslator superTranslator) {
        super(superTranslator);
        this.localTranslations = new TranslatedExpression();
        this.bft = super.getConfig().getBasicFunctionsTranslator();
        this.limitAnalyzer = new LimitAnalyzer(this);
    }

    @Override
    public TranslatedExpression getTranslatedExpressionObject() {
        return localTranslations;
    }

    @Override
    public TranslatedExpression translate(PomTaggedExpression exp, List<PomTaggedExpression> list){
        if (list.isEmpty()) {
            throw TranslationException.buildException(this, "Limited expression in the end are illegal!",
                    TranslationExceptionReason.INVALID_LATEX_INPUT);
        }

        MathTerm root = exp.getRoot();
        LimitedExpressions category = LimitedExpressions.getExpression(root);
        checkCategoryValidity(category, root);

        MathematicalEssentialOperatorMetadata limit = saveGetLimit(list, category);;
        getInfoLogger().getFreeVariables().suppressingVars(limit.getVars());

        // find elements that are part of the argument:
        // next, split into argument parts and the rest
        TranslatedExpression translatedPotentialArguments = getPotentialTranslatedExpressions(limit, list);
        TranslatedExpression transArgs = getTranslatedExpression(limit, category, translatedPotentialArguments);

        String finalTranslation = getFinalTranslationString(limit, transArgs, category, root);

        // add translation and the rest of the translation
        updateTranslationLists(finalTranslation, translatedPotentialArguments, category);

        getInfoLogger().getFreeVariables().releaseVars(limit.getVars());

        return localTranslations;
    }

    private void checkCategoryValidity(LimitedExpressions category, MathTerm root) {
        if ( category == null ) {
            throw TranslationException.buildExceptionObj(this, "Unsupported limited expressions." + root.getTermText(),
                    TranslationExceptionReason.MISSING_TRANSLATION_INFORMATION,
                    root.getTermText()
            );
        }
    }

    private MathematicalEssentialOperatorMetadata saveGetLimit(List<PomTaggedExpression> list, LimitedExpressions category) {
        try {
            return getLimit(list, category);
        } catch (Error | Exception e) {
            throw TranslationException.buildException(
                    this, "Unable to identify interval of " + category,
                    TranslationExceptionReason.MISSING_TRANSLATION_INFORMATION
            );
        }
    }

    private MathematicalEssentialOperatorMetadata getLimit(List<PomTaggedExpression> list, LimitedExpressions category) {
        PomTaggedExpression limitExpression = list.remove(0);
        MathematicalEssentialOperatorMetadata limit = null;

        switch( category ) {
            case INT:
                limit = extractIntegralLimits(limitExpression, this);
                break;
            case SUM:
            case PROD:
                limit = extractLimits(limitExpression, BlueprintMaster.LIMITED);
                break;
            case LIM:
                limit = extractLimits(limitExpression, BlueprintMaster.LIM);
                break;
        }

        if ( this.indef ) {
            // when there are no limits, we accidentally removed the first argument -> rollback
            list.add(0, limitExpression);
        }

        return limit;
    }

    private void updateTranslationLists(
            String finalTranslation,
            TranslatedExpression translatedPotentialArguments,
            LimitedExpressions category
    ) {
        // add translation and the rest of the translation
        localTranslations.addTranslatedExpression(finalTranslation);
        getGlobalTranslationList().addTranslatedExpression(finalTranslation);

        if ( !category.equals(LimitedExpressions.INT) ){
            localTranslations.addTranslatedExpression(translatedPotentialArguments);
            getGlobalTranslationList().addTranslatedExpression(translatedPotentialArguments);
        }
    }

    private TranslatedExpression getPotentialTranslatedExpressions(MathematicalEssentialOperatorMetadata limit, List<PomTaggedExpression> list) {
        List<PomTaggedExpression> potentialArguments =
                VariableExtractor.getPotentialArgumentsUntilEndOfScope(list, limit.getVars(), this);

        // the potential arguments is a theoretical sequence, so handle it as a sequence!
        PomTaggedExpression topPTE = FakeMLPGenerator.generateEmptySequencePPTE();
        for ( PomTaggedExpression pte : potentialArguments ) topPTE.addComponent(pte);

        // next, we translate the expressions to search for the variables
        SequenceTranslator p = new SequenceTranslator(getSuperTranslator());
        return p.translate( topPTE );
    }

    private TranslatedExpression getTranslatedExpression(
            MathematicalEssentialOperatorMetadata limit,
            LimitedExpressions category,
            TranslatedExpression translatedPotentialArguments
    ) {
        // first, clear global expression
        getGlobalTranslationList().removeLastNExps(translatedPotentialArguments.getLength());

        // find elements that are part of the argument:
        // next, split into argument parts and the rest
        return category.equals(LimitedExpressions.INT) ?
                translatedPotentialArguments :
                removeUntilLastAppearance(
                        translatedPotentialArguments,
                        limit.getVars()
                );
    }

    private String getFinalTranslationString(
            MathematicalEssentialOperatorMetadata limit,
            TranslatedExpression transArgs,
            LimitedExpressions category,
            MathTerm root
    ) {
        int lastIdx = limit.getVars().size()-1;

        // start with inner -> last elements in limit
        MathematicalEssentialOperatorMetadata.BoundaryStrings boundaries = limit.getArguments(
                lastIdx, indef, stripMultiParentheses(transArgs.getTranslatedExpression()), category
        );
        String finalTranslation = translatePattern(boundaries, category, root);

        if ( lastIdx > 0 ) {
            for ( int i = lastIdx-1; i >= 0; i-- ) {
                boundaries = limit.getArguments(i, indef, stripMultiParentheses(finalTranslation), category);
                finalTranslation = translatePattern(boundaries, category, root);
            }
        }

        return finalTranslation;
    }

    private String translatePattern(MathematicalEssentialOperatorMetadata.BoundaryStrings boundaries, LimitedExpressions category, MathTerm mathTerm) {
        if ( category.equals(LimitedExpressions.INT) ) {
            int degree = LimitedExpressions.getMultiIntDegree(mathTerm);
            return recursiveIntTranslation( boundaries.getCategoryKey(), boundaries.getArgs(), degree );
        } else return bft.translate(boundaries.getArgs(), boundaries.getCategoryKey());
    }

    private String recursiveIntTranslation( String translationKey, String[] args, int degree ) {
        if ( degree < 1 ) return "";

        String newArg = recursiveIntTranslation(translationKey, args, degree-1);
        // the real argument is always the last in the array
        if ( !newArg.isEmpty() )
            args[args.length-1] = newArg;

        return bft.translate(args, translationKey);
    }

    private MathematicalEssentialOperatorMetadata extractLimits(PomTaggedExpression limitSuperExpr, boolean lim) {
        LinkedList<PomTaggedExpression> upperBound = new LinkedList<>();
        MathematicalEssentialOperatorMetadata limit = limitAnalyzer.extractLimitsWithoutParsing(
                limitSuperExpr,
                upperBound,
                lim
        );

        // if an upper bound was explicitly given, overwrite the parsed upper bound
        if ( !upperBound.isEmpty() ) {
            TranslatedExpression te = translateInnerExp(upperBound.remove(0), upperBound);
            limit.overwriteUpperLimit(te.getTranslatedExpression());
        }

        return limit;
    }

    private MathematicalEssentialOperatorMetadata extractIntegralLimits(PomTaggedExpression limitSuperExpr, AbstractTranslator parentTranslator) {
        LinkedList<PomTaggedExpression> upperBound = new LinkedList<>();
        PomTaggedExpression lower = limitAnalyzer.getLowerUpper(limitSuperExpr, upperBound, parentTranslator, true);

        if ( lower == null ) {
            this.indef = true;
            return new MathematicalEssentialOperatorMetadata(
                    new LinkedList<>(),
                    new LinkedList<>(),
                    new LinkedList<>()
            );
        }

        TranslatedExpression upperTrans = translateInnerExp(upperBound.removeFirst(), upperBound);
        TranslatedExpression lowerTrans = translateInnerExp(lower, new LinkedList<>());

        LinkedList<String> u = new LinkedList<>();
        LinkedList<String> l = new LinkedList<>();

        u.add(upperTrans.toString());
        l.add(lowerTrans.toString());

        return new MathematicalEssentialOperatorMetadata(new LinkedList<>(), l, u);
    }

    private TranslatedExpression removeUntilLastAppearance(TranslatedExpression te, List<String> vars) {
        return te.removeUntilLastAppearanceOfVar(vars, getConfig().getMULTIPLY());
    }
}