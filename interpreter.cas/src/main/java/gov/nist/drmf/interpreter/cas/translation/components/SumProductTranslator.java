package gov.nist.drmf.interpreter.cas.translation.components;

import gov.nist.drmf.interpreter.cas.blueprints.BlueprintMaster;
import gov.nist.drmf.interpreter.cas.logging.TranslatedExpression;
import gov.nist.drmf.interpreter.cas.translation.AbstractListTranslator;
import gov.nist.drmf.interpreter.cas.translation.AbstractTranslator;
import gov.nist.drmf.interpreter.cas.translation.SemanticLatexTranslator;
import gov.nist.drmf.interpreter.common.constants.GlobalConstants;
import gov.nist.drmf.interpreter.common.exceptions.TranslationException;
import gov.nist.drmf.interpreter.cas.blueprints.Limits;
import gov.nist.drmf.interpreter.common.grammar.Brackets;
import gov.nist.drmf.interpreter.common.grammar.ExpressionTags;
import gov.nist.drmf.interpreter.common.grammar.MathTermTags;
import gov.nist.drmf.interpreter.common.symbols.BasicFunctionsTranslator;
import gov.nist.drmf.interpreter.mlp.extensions.FeatureSetUtility;
import mlp.MathTerm;
import mlp.PomTaggedExpression;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static gov.nist.drmf.interpreter.common.exceptions.TranslationException.Reason.MLP_ERROR;

/**
 * SumProductTranslator uses parseGeneralExpression to get the arguments to the sum/product.
 * Then it uses BasicFunctionParser to put the arguments where they need to go.
 *
 * Method call order: translate -> addToArgs -> onlyLower/lowerAndUpper -> addFactorsToSummand
 *
 * @author Andre Greiner-Petter
 * @author Rajen Dey
 *
 * July 2019
 */
public class SumProductTranslator extends AbstractListTranslator {
    private static final Logger LOG = LogManager.getLogger(SumProductTranslator.class.getName());

    private static ArrayList<ArrayList<String>> args = new ArrayList<>();

    private String index;

    private static int num = -1;

    // perform translation and put everything into global_exp
    private BasicFunctionsTranslator bft;

    private TranslatedExpression localTranslations;

    public SumProductTranslator(AbstractTranslator superTranslator) {
        super(superTranslator);
        this.localTranslations = new TranslatedExpression();
        this.bft = super.getConfig().getBasicFunctionsTranslator();
    }

    @Nullable
    @Override
    public TranslatedExpression getTranslatedExpressionObject() {
        return localTranslations;
    }

    @Override
    public boolean translate(PomTaggedExpression exp, List<PomTaggedExpression> list){
        // exp is sum/prod/lim

        if (list.isEmpty()) {
            throw new TranslationException("Limited expression in the end are illegal!");
        }

        MathTerm root = exp.getRoot();
        String category;
        if (FeatureSetUtility.isSum(root) ) category = "sum";
        else if ( FeatureSetUtility.isProduct(root) ) category = "prod";
        else throw new TranslationException("Other limited expressions than sum/prod are not yet supported.");

        PomTaggedExpression limitExpression = list.remove(0);
        Limits limit = extractLimits(limitExpression);

        List<PomTaggedExpression> potentialArguments = getPotentialArgumentsUntilEndOfScope(list, limit.getVars());
        // the potential arguments is a theoretical sequence, so handle it as a sequence!
        PomTaggedExpression topPTE = new PomTaggedExpression(new MathTerm("",""), "sequence");
        for ( PomTaggedExpression pte : potentialArguments ) topPTE.addComponent(pte);

        SequenceTranslator p = new SequenceTranslator(getSuperTranslator());
        boolean successful = p.translate( topPTE );

        if ( !successful ) { // well, there were an error... stop here
            return false;
        }

        // next, we translate the expressions to search for the variables
        TranslatedExpression translatedPotentialArguments = p.getTranslatedExpressionObject();

        // first, clear global expression
        getGlobalTranslationList().removeLastNExps(translatedPotentialArguments.getLength());

        // find elements that are part of the argument:
        // next, split into argument parts and the rest
        TranslatedExpression transArgs = removeUntilLastAppearence(
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
                category
        );

        if ( lastIdx > 0 ) {
            for ( int i = lastIdx-1; i >= 0; i-- ) {
                finalTranslation = translatePattern(
                        limit,
                        i,
                        stripMultiParentheses(finalTranslation),
                        category
                );
            }
        }

        // add translation and the rest of the translation
        localTranslations.addTranslatedExpression(finalTranslation);
        localTranslations.addTranslatedExpression(translatedPotentialArguments);

        getGlobalTranslationList().addTranslatedExpression(finalTranslation);
        getGlobalTranslationList().addTranslatedExpression(translatedPotentialArguments);

        return true;
    }

    private String translatePattern(Limits limit, int idx, String arg, String key) {
        if ( !limit.isLimitOverSet() ) {
            String[] args = new String[]{
                    limit.getVars().get(idx),
                    limit.getLower().get(idx),
                    limit.getUpper().get(idx),
                    arg,
            };
            return bft.translate(args, key);
        } else {
            String[] args = new String[]{
                    limit.getVars().get(idx),
                    limit.getLower().get(idx),
                    arg,
            };
            return bft.translate(args, key+"Set");
        }
    }

    private Limits extractLimits(PomTaggedExpression limitSuperExpr) {
        LinkedList<PomTaggedExpression> upperBound = new LinkedList<>();
        Limits limit = extractLimitsWithoutParsing(limitSuperExpr, upperBound);

        // if an upper bound was explicitly given, overwrite the parsed upper bound
        if ( !upperBound.isEmpty() ) {
            TranslatedExpression te = parseGeneralExpression(upperBound.remove(0), upperBound);
            getGlobalTranslationList().removeLastNExps(te.getLength());
            limit.overwriteUpperLimit(te.getTranslatedExpression());
        }

        return limit;
    }

    private Limits extractLimitsWithoutParsing(PomTaggedExpression limitSuperExpr, List<PomTaggedExpression> upperBound) {
        MathTerm term = limitSuperExpr.getRoot();

        PomTaggedExpression limitExpression = null;

        // in case it is a MathTerm, it MUST be a lower bound!
        if ( term != null && !term.isEmpty() ) {
            MathTermTags tag = MathTermTags.getTagByKey(term.getTag());
            if ( !tag.equals(MathTermTags.underscore) ) {
                throw new TranslationException("Illegal expression followed a limited expression: " + term.getTermText());
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
                throw new TranslationException("A limited expression without limits is not allowed: " + term.getTermText());
            }
        }

        // now we have limitExpression and an optional upperBound. Parse it:
        BlueprintMaster btm = getConfig().getLimitParser();
        return btm.findMatchingLimit(limitExpression);
    }

    /**
     * Removes and returns all elements that may potentially be in the scope of a limited expression.
     * This does not check for appearances of variables, it simply takes all expressions until a
     * breakpoint is reached. Breakpoints are relations (equal signs etc.) and closed brackets (if
     * the brackets are was not opened inside in the scope).
     * @param list
     * @return
     */
    private List<PomTaggedExpression> getPotentialArgumentsUntilEndOfScope(List<PomTaggedExpression> list, List<String> currVars) {
        LinkedList<PomTaggedExpression> cache = new LinkedList<>();
        LinkedList<Brackets> parenthesisCache = new LinkedList<>();

        // the very next element is always(!) part of the argument
        if ( list.isEmpty() ) {
            throw new TranslationException(
                    "A limited expression ends with no argument left."
            );
        }

        PomTaggedExpression first = list.remove(0);
        cache.add(first);

        // first element could be a parenthesis also... than take all elements until this parenthesis is closed
        Brackets bracket = SequenceTranslator.ifIsBracketTransform(first.getRoot());
        if ( bracket != null ) {
            if ( !bracket.opened ) throw new TranslationException("No arguments for limited expression found.");
            parenthesisCache.addLast(bracket);
        }

        // now add all until there is a stop expression
        while ( !list.isEmpty() ) {
            PomTaggedExpression curr = list.get(0); // do not remove yet!
            MathTerm mt = curr.getRoot();
            if ( mt != null && mt.getTag() != null ) {
                bracket = SequenceTranslator.ifIsBracketTransform(mt);
                // check for brackets
                if ( bracket != null ) {
                    // if new bracket opens, add it to cache
                    if ( bracket.opened ) {
                        parenthesisCache.addLast(bracket);
                    }
                    else {
                        if ( parenthesisCache.isEmpty() ) {
                            // in case a bracket is closed that was not opened in this scope, we reached a breakpoint
                            LOG.debug("Limited expression breakpoint reached (reason: outer closing parenthesis)");
                            return cache;
                        } else if ( parenthesisCache.getLast().counterpart.equals(bracket.symbol) ) {
                            // in case the bracket closes a previously opened bracket, update cache
                            parenthesisCache.removeLast();
                        } else { // well, that's an illegal situation, parentheses does not match
                            throw new TranslationException("Open and close parentheses does not match!");
                        }
                    }
                } else if ( FeatureSetUtility.isSum(mt) || FeatureSetUtility.isProduct(mt) ) {
                    // there is a special case where we also have to stop...
                    // if a new limited expression is coming that shares the same variable

                    // in this case, the next element are the limits. So lets analyze them in advance
                    PomTaggedExpression nextLimits = list.get(1);
                    Limits nextL = extractLimitsWithoutParsing(nextLimits, new LinkedList<>());
                    for ( String nextVar : nextL.getVars() ){
                        if ( currVars.contains(nextVar) ) {
                            LOG.debug("Limited expression breakpoint reached (reason: sharing variables)");
                            // there is a match in variables... so, we reached a breakpoint
                            if ( !cache.isEmpty() ) {
                                PomTaggedExpression last = cache.getLast();
                                MathTerm t = last.getRoot();
                                if ( t != null && !t.isEmpty() && t.getTermText().matches("\\s*[+-.,;^/*]\\s*") ) {
                                    cache.removeLast();
                                    list.add(0, last);
                                }
                            }
                            return cache;
                        }
                    }
                } else {
                    MathTermTags tag = MathTermTags.getTagByKey(mt.getTag());
                    // stop only in case of a harsh stop symbol appears on the same level of the sum
                    // stoppers are relations (left-hand side and right-hand side).
                    switch (tag) {
                        case relation:
                        case equals:
                        case less_than:
                        case greater_than:
                            // found stopper -> return the cache
                            LOG.debug("Limited expression breakpoint reached (reason: relation)");
                            return cache;
                    }
                }
            } // if no stopper is found, just add it to the potential list
            cache.addLast(list.remove(0));
        }

        // well, it might be the entire expression until the end, of course
        return cache;
    }

    private TranslatedExpression removeUntilLastAppearence(TranslatedExpression te, List<String> vars) {
        return te.removeUntilLastAppearanceOfVar(vars, getConfig().getMULTIPLY());
    }
}