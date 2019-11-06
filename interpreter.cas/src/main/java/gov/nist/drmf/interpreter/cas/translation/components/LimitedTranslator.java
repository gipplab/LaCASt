package gov.nist.drmf.interpreter.cas.translation.components;

import gov.nist.drmf.interpreter.cas.blueprints.BlueprintMaster;
import gov.nist.drmf.interpreter.cas.blueprints.Limits;
import gov.nist.drmf.interpreter.cas.logging.TranslatedExpression;
import gov.nist.drmf.interpreter.cas.translation.AbstractListTranslator;
import gov.nist.drmf.interpreter.cas.translation.AbstractTranslator;
import gov.nist.drmf.interpreter.common.exceptions.TranslationException;
import gov.nist.drmf.interpreter.common.grammar.Brackets;
import gov.nist.drmf.interpreter.common.grammar.ExpressionTags;
import gov.nist.drmf.interpreter.common.grammar.LimitedExpressions;
import gov.nist.drmf.interpreter.common.grammar.MathTermTags;
import gov.nist.drmf.interpreter.common.symbols.BasicFunctionsTranslator;
import mlp.MathTerm;
import mlp.PomTaggedExpression;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.ArrayList;
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

    private static ArrayList<ArrayList<String>> args = new ArrayList<>();

    private String index;

    private static int num = -1;

    // perform translation and put everything into global_exp
    private BasicFunctionsTranslator bft;

    private TranslatedExpression localTranslations;

    public LimitedTranslator(AbstractTranslator superTranslator) {
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
        LimitedExpressions category = LimitedExpressions.getExpression(root);
        if ( category == null ) {
            throw new TranslationException("Unsupported limited expressions." + root.getTermText());
        }

        PomTaggedExpression limitExpression = list.remove(0);
        Limits limit = null;

        switch( category ) {
            case INT:
                limit = extractIntegralLimits(limitExpression);
                break;
            case SUM:
            case PROD:
                limit = extractLimits(limitExpression, BlueprintMaster.LIMITED);
                break;
            case LIM:
                limit = extractLimits(limitExpression, BlueprintMaster.LIM);
                break;
        }

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
        TranslatedExpression transArgs = category.equals(LimitedExpressions.INT) ?
                translatedPotentialArguments :
                removeUntilLastAppearence(
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
        getGlobalTranslationList().addTranslatedExpression(finalTranslation);

        if ( !category.equals(LimitedExpressions.INT) ){
            localTranslations.addTranslatedExpression(translatedPotentialArguments);
            getGlobalTranslationList().addTranslatedExpression(translatedPotentialArguments);
        }

        return true;
    }

    private String translatePattern(Limits limit, int idx, String arg, LimitedExpressions category) {
        if ( !limit.isLimitOverSet() ) {
            if ( limit.getDirection() != null ) {
                String[] args = new String[]{
                        limit.getVars().get(idx),
                        limit.getLower().get(idx),
                        arg,
                };
                return bft.translate(args, category.getDirectionKey(limit.getDirection()));
            } else {
                String[] args = new String[]{
                        limit.getVars().get(idx),
                        limit.getLower().get(idx),
                        limit.getUpper().get(idx),
                        arg,
                };
                return bft.translate(args, category.getKey());
            }
        } else {
            String[] args = new String[]{
                    limit.getVars().get(idx),
                    limit.getLower().get(idx),
                    arg,
            };
            return bft.translate(args, category.getSetKey());
        }
    }

    private Limits extractLimits(PomTaggedExpression limitSuperExpr, boolean lim) {
        LinkedList<PomTaggedExpression> upperBound = new LinkedList<>();
        Limits limit = extractLimitsWithoutParsing(limitSuperExpr, upperBound, lim);

        // if an upper bound was explicitly given, overwrite the parsed upper bound
        if ( !upperBound.isEmpty() ) {
            TranslatedExpression te = translateInnerExp(upperBound.remove(0), upperBound);
            limit.overwriteUpperLimit(te.getTranslatedExpression());
        }

        return limit;
    }

    private Limits extractIntegralLimits(PomTaggedExpression limitSuperExpr) {
        LinkedList<PomTaggedExpression> upperBound = new LinkedList<>();
        PomTaggedExpression lower = getLowerUpper(limitSuperExpr, upperBound);

        TranslatedExpression upperTrans = translateInnerExp(upperBound.removeFirst(), upperBound);
        TranslatedExpression lowerTrans = translateInnerExp(lower, new LinkedList<>());

        LinkedList<String> u = new LinkedList<>();
        LinkedList<String> l = new LinkedList<>();

        u.add(upperTrans.toString());
        l.add(lowerTrans.toString());

        return new Limits(new LinkedList<>(), l, u);
    }

    private Limits extractLimitsWithoutParsing(PomTaggedExpression limitSuperExpr, List<PomTaggedExpression> upperBound, boolean lim) {
        PomTaggedExpression limitExpression = getLowerUpper(limitSuperExpr, upperBound);

        // now we have limitExpression and an optional upperBound. Parse it:
        BlueprintMaster btm = getConfig().getLimitParser();
        return btm.findMatchingLimit(lim, limitExpression);
    }

    private PomTaggedExpression getLowerUpper(PomTaggedExpression limitSuperExpr, List<PomTaggedExpression> upperBound) {
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

        return limitExpression;
    }

    /**
     * Removes and returns all elements that may potentially be in the scope of a limited expression.
     * This does not check for appearances of variables, it simply takes all expressions until a
     * breakpoint is reached. Breakpoints are relations (equal signs etc.) and closed brackets (if
     * the brackets are was not opened inside in the scope).
     * @param list
     * @return
     */
    private List<PomTaggedExpression> getPotentialArgumentsUntilEndOfScope(
            List<PomTaggedExpression> list,
            List<String> currVars
    ) {
        LinkedList<PomTaggedExpression> cache = new LinkedList<>();
        LinkedList<Brackets> parenthesisCache = new LinkedList<>();
        LinkedList<PomTaggedExpression> fracList = null;
        int innerInts = 0;

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

        if ( !first.getRoot().isEmpty() && LimitedExpressions.isIntegral(first.getRoot())){
            innerInts++;
        }

        fracList = isDiffFrac(first);
        if ( fracList != null ) {
            cache.removeFirst();
            while (!fracList.isEmpty())
                list.add(0, fracList.removeLast());
            cache.add(list.remove(0));
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
                } else if ( !parenthesisCache.isEmpty() ) {
                    cache.addLast(list.remove(0));
                    continue;
                } else if ( LimitedExpressions.isSum(mt) || LimitedExpressions.isProduct(mt) ) {
                    // there is a special case where we also have to stop...
                    // if a new limited expression is coming that shares the same variable

                    // in this case, the next element are the limits. So lets analyze them in advance
                    PomTaggedExpression nextLimits = list.get(1);
                    Limits nextL = extractLimitsWithoutParsing(nextLimits, new LinkedList<>(), BlueprintMaster.LIMITED);
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
                } else if ( mt.getTermText().matches("\\\\diffd?") ) {
                    if ( innerInts > 0 ) innerInts--;
                    else {
                        list.remove(0); // diff or diffd... so get next for arg
                        PomTaggedExpression argPTE = list.remove(0);
                        TranslatedExpression argTe = translateInnerExp(argPTE, list);
                        currVars.add(argTe.toString());
                        return cache;
                    }
                } else if ( LimitedExpressions.isIntegral(mt) ) {
                    innerInts++;
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
            } else if ( (fracList = isDiffFrac(curr)) != null ) {
                list.remove(0);
                while (!fracList.isEmpty())
                    list.add(0, fracList.removeLast());
            }

            // if no stopper is found, just add it to the potential list
            cache.addLast(list.remove(0));
        }

        // well, it might be the entire expression until the end, of course
        return cache;
    }

    private LinkedList<PomTaggedExpression> isDiffFrac(PomTaggedExpression pte) {
        ExpressionTags pt = ExpressionTags.getTagByKey(pte.getTag());
        if ( pt != null && pt.equals(ExpressionTags.fraction) ) {
            PomTaggedExpression numeratorPTE = pte.getComponents().get(0);
            ExpressionTags it = ExpressionTags.getTagByKey(numeratorPTE.getTag());

            if ( it != null && !it.equals(ExpressionTags.sequence) )
                return null;

            LinkedList<PomTaggedExpression> list = new LinkedList<>();
            LinkedList<PomTaggedExpression> temp = new LinkedList<>();

            List<PomTaggedExpression> seq = numeratorPTE.getComponents();
            while ( !seq.isEmpty() ) {
                PomTaggedExpression e = seq.remove(0);

                MathTerm mt = e.getRoot();
                if ( mt != null && !mt.isEmpty() ) {
                    if ( mt.getTermText().matches("\\\\diffd?") ) {
                        list.addLast(e);
                        list.addLast(seq.remove(0));
                        continue;
                    }
                }

                temp.addLast(e);
            }

            if ( !temp.isEmpty() ) {
                for ( PomTaggedExpression t : temp )
                    numeratorPTE.addComponent(t);
            } else {
                MathTerm oneMT = new MathTerm("1", "digit", "numeric", "numerator");
                numeratorPTE.addComponent(0, oneMT);
            }

            list.addFirst(pte);
            return list;
        }
        return null;
    }

    private TranslatedExpression removeUntilLastAppearence(TranslatedExpression te, List<String> vars) {
        return te.removeUntilLastAppearanceOfVar(vars, getConfig().getMULTIPLY());
    }
}