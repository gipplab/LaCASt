package gov.nist.drmf.interpreter.cas.translation.components;

import gov.nist.drmf.interpreter.cas.blueprints.BlueprintMaster;
import gov.nist.drmf.interpreter.cas.blueprints.Limits;
import gov.nist.drmf.interpreter.cas.logging.TranslatedExpression;
import gov.nist.drmf.interpreter.cas.translation.AbstractListTranslator;
import gov.nist.drmf.interpreter.cas.translation.AbstractTranslator;
import gov.nist.drmf.interpreter.cas.translation.components.util.LimitAnalyzer;
import gov.nist.drmf.interpreter.cas.translation.components.util.VariableExtractor;
import gov.nist.drmf.interpreter.common.exceptions.TranslationException;
import gov.nist.drmf.interpreter.common.exceptions.TranslationExceptionReason;
import gov.nist.drmf.interpreter.common.grammar.LimitedExpressions;
import gov.nist.drmf.interpreter.common.symbols.BasicFunctionsTranslator;
import gov.nist.drmf.interpreter.mlp.FakeMLPGenerator;
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
        this.limitAnalyzer = new LimitAnalyzer();
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
        if ( category == null ) {
            throw TranslationException.buildExceptionObj(this, "Unsupported limited expressions." + root.getTermText(),
                    TranslationExceptionReason.MISSING_TRANSLATION_INFORMATION,
                    root.getTermText());
        }

        PomTaggedExpression limitExpression = list.remove(0);
        Limits limit = null;

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

        List<PomTaggedExpression> potentialArguments = VariableExtractor.getPotentialArgumentsUntilEndOfScope(
                list,
                limit.getVars(),
                this
        );

        // the potential arguments is a theoretical sequence, so handle it as a sequence!
        PomTaggedExpression topPTE = FakeMLPGenerator.generateEmptySequencePTE();
        for ( PomTaggedExpression pte : potentialArguments ) topPTE.addComponent(pte);

        // next, we translate the expressions to search for the variables
        SequenceTranslator p = new SequenceTranslator(getSuperTranslator());
        TranslatedExpression translatedPotentialArguments = p.translate( topPTE );

        // first, clear global expression
        getGlobalTranslationList().removeLastNExps(translatedPotentialArguments.getLength());

        // find elements that are part of the argument:
        // next, split into argument parts and the rest
        TranslatedExpression transArgs = category.equals(LimitedExpressions.INT) ?
                translatedPotentialArguments :
                removeUntilLastAppearance(
                        translatedPotentialArguments,
                        limit.getVars()
                );

        int lastIdx = limit.getVars().size()-1;

        // start with inner -> last elements in limit
        String finalTranslation = translatePattern(
                limit,
                lastIdx,
                stripMultiParentheses(
                    transArgs.getTranslatedExpression()
                ),
                category,
                root
        );

        if ( lastIdx > 0 ) {
            for ( int i = lastIdx-1; i >= 0; i-- ) {
                finalTranslation = translatePattern(
                        limit,
                        i,
                        stripMultiParentheses(finalTranslation),
                        category,
                        root
                );
            }
        }

        // add translation and the rest of the translation
        localTranslations.addTranslatedExpression(finalTranslation);
        getGlobalTranslationList().addTranslatedExpression(finalTranslation);

        if ( !category.equals(LimitedExpressions.INT) ){
            localTranslations.addTranslatedExpression(translatedPotentialArguments);
            getGlobalTranslationList().addTranslatedExpression(translatedPotentialArguments);
        }

        return localTranslations;
    }

    private String translatePattern(Limits limit, int idx, String arg, LimitedExpressions category, MathTerm mathTerm) {
        String[] arguments = null;
        String categoryKey = null;

        if ( indef ) {
            arguments = new String[]{
                    limit.getVars().get(idx),
                    arg
            };
            categoryKey = category.getIndefKey();
        } else if ( !limit.isLimitOverSet() ) {
            if ( limit.getDirection() != null ) {
                arguments = new String[]{
                        limit.getVars().get(idx),
                        limit.getLower().get(idx),
                        arg,
                };
                categoryKey = category.getDirectionKey(limit.getDirection());
            } else {
                arguments = new String[]{
                        limit.getVars().get(idx),
                        limit.getLower().get(idx),
                        limit.getUpper().get(idx),
                        arg,
                };
                categoryKey = category.getKey();
            }
        } else {
            arguments = new String[]{
                    limit.getVars().get(idx),
                    limit.getLower().get(idx),
                    arg,
            };
            categoryKey = category.getSetKey();
        }

        if ( category.equals(LimitedExpressions.INT) ) {
            int degree = LimitedExpressions.getMultiIntDegree(mathTerm);
            return recursiveIntTranslation( categoryKey, arguments, degree );
        } else return bft.translate(arguments, categoryKey);
    }

    private String recursiveIntTranslation( String translationKey, String[] args, int degree ) {
        if ( degree < 1 ) return "";

        String newArg = recursiveIntTranslation(translationKey, args, degree-1);
        // the real argument is always the last in the array
        if ( !newArg.isEmpty() )
            args[args.length-1] = newArg;

        return bft.translate(args, translationKey);
    }

    private Limits extractLimits(PomTaggedExpression limitSuperExpr, boolean lim) {
        LinkedList<PomTaggedExpression> upperBound = new LinkedList<>();
        Limits limit = limitAnalyzer.extractLimitsWithoutParsing(
                limitSuperExpr,
                upperBound,
                lim,
                getConfig().getLimitParser(),
                this
        );

        // if an upper bound was explicitly given, overwrite the parsed upper bound
        if ( !upperBound.isEmpty() ) {
            TranslatedExpression te = translateInnerExp(upperBound.remove(0), upperBound);
            limit.overwriteUpperLimit(te.getTranslatedExpression());
        }

        return limit;
    }

    private Limits extractIntegralLimits(PomTaggedExpression limitSuperExpr, AbstractTranslator parentTranslator) {
        LinkedList<PomTaggedExpression> upperBound = new LinkedList<>();
        PomTaggedExpression lower = limitAnalyzer.getLowerUpper(limitSuperExpr, upperBound, parentTranslator, true);

        if ( lower == null ) {
            this.indef = true;
            return new Limits(
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

        return new Limits(new LinkedList<>(), l, u);
    }

    private TranslatedExpression removeUntilLastAppearance(TranslatedExpression te, List<String> vars) {
        return te.removeUntilLastAppearanceOfVar(vars, getConfig().getMULTIPLY());
    }
}