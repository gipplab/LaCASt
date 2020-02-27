package gov.nist.drmf.interpreter.cas.translation.components;

import gov.nist.drmf.interpreter.cas.blueprints.BlueprintMaster;
import gov.nist.drmf.interpreter.cas.blueprints.Limits;
import gov.nist.drmf.interpreter.cas.logging.TranslatedExpression;
import gov.nist.drmf.interpreter.cas.translation.AbstractListTranslator;
import gov.nist.drmf.interpreter.cas.translation.AbstractTranslator;
import gov.nist.drmf.interpreter.common.exceptions.TranslationException;
import gov.nist.drmf.interpreter.common.exceptions.TranslationExceptionReason;
import gov.nist.drmf.interpreter.common.grammar.Brackets;
import gov.nist.drmf.interpreter.common.grammar.ExpressionTags;
import gov.nist.drmf.interpreter.common.grammar.LimitedExpressions;
import gov.nist.drmf.interpreter.common.grammar.MathTermTags;
import gov.nist.drmf.interpreter.common.symbols.BasicFunctionsTranslator;
import gov.nist.drmf.interpreter.mlp.extensions.FakeMLPGenerator;
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

    private TranslatedExpression localTranslations;

    private boolean indef = false;

    public LimitedTranslator(AbstractTranslator superTranslator) {
        super(superTranslator);
        this.localTranslations = new TranslatedExpression();
        this.bft = super.getConfig().getBasicFunctionsTranslator();
    }

    @Override
    public TranslatedExpression getTranslatedExpressionObject() {
        return localTranslations;
    }

    @Override
    public TranslatedExpression translate(PomTaggedExpression exp, List<PomTaggedExpression> list){
        if (list.isEmpty()) {
            throw buildException("Limited expression in the end are illegal!",
                    TranslationExceptionReason.INVALID_LATEX_INPUT);
        }

        MathTerm root = exp.getRoot();
        LimitedExpressions category = LimitedExpressions.getExpression(root);
        if ( category == null ) {
            throw buildExceptionObj("Unsupported limited expressions." + root.getTermText(),
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

        List<PomTaggedExpression> potentialArguments = getPotentialArgumentsUntilEndOfScope(
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
        Limits limit = extractLimitsWithoutParsing(
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
        PomTaggedExpression lower = getLowerUpper(limitSuperExpr, upperBound, parentTranslator, true);

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

    private static Limits extractLimitsWithoutParsing(
            PomTaggedExpression limitSuperExpr,
            List<PomTaggedExpression> upperBound,
            boolean lim,
            BlueprintMaster btm,
            AbstractTranslator parentTranslator) {
        PomTaggedExpression limitExpression = getLowerUpper(limitSuperExpr, upperBound, parentTranslator, false);

        // now we have limitExpression and an optional upperBound. Parse it:
        return btm.findMatchingLimit(lim, limitExpression);
    }

    private static PomTaggedExpression getLowerUpper(
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
                else throw parentTranslator.buildException(
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
                else throw parentTranslator.buildException(
                        "A limited expression without limits is not allowed: " + term.getTermText(),
                        TranslationExceptionReason.INVALID_LATEX_INPUT);
            }
        }

        return limitExpression;
    }

    /**
     * Removes and returns all elements that may potentially be in the scope of a limited expression.
     * This does not check for appearances of variables, it simply takes all expressions until a
     * breakpoint is reached. Breakpoints are relations (equal signs etc.) and closed brackets (if
     * the brackets are was not opened inside in the scope).
     * @param list following expressions, will be modified by this function
     * @param currVars list of variables
     * @param abstractTranslator the translator object that should be invoked, if necessary
     * @return a sublist of {@param list}, note that {@param list} will be shortened
     */
    static List<PomTaggedExpression> getPotentialArgumentsUntilEndOfScope(
            List<PomTaggedExpression> list,
            List<String> currVars,
            AbstractTranslator abstractTranslator
    ) {
        LinkedList<PomTaggedExpression> cache = new LinkedList<>();
        LinkedList<Brackets> parenthesisCache = new LinkedList<>();
        LinkedList<PomTaggedExpression> fracList;
        int innerInts = 0;

        // the very next element is always(!) part of the argument
        if ( list.isEmpty() ) {
            throw abstractTranslator.buildException( "A limited expression ends with no argument left.",
                    TranslationExceptionReason.INVALID_LATEX_INPUT);
        }

        PomTaggedExpression first = list.remove(0);
        cache.add(first);

        // first element could be a parenthesis also... than take all elements until this parenthesis is closed
        Brackets bracket = SequenceTranslator.ifIsBracketTransform(first.getRoot(), null);
        if ( bracket != null ) {
            if ( !bracket.opened ) throw abstractTranslator.buildException(
                    "Empty arguments for limited expressions are invalid math.",
                    TranslationExceptionReason.INVALID_LATEX_INPUT);
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
                bracket = SequenceTranslator.ifIsBracketTransform(mt, null);
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
                            throw abstractTranslator.buildException(
                                    "Open and close parentheses does not match!",
                                    TranslationExceptionReason.WRONG_PARENTHESIS);
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
                    Limits nextL = extractLimitsWithoutParsing(
                            nextLimits,
                            new LinkedList<>(),
                            BlueprintMaster.LIMITED,
                            abstractTranslator.getConfig().getLimitParser(),
                            abstractTranslator
                    );
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
                        TranslatedExpression argTe = abstractTranslator.translateInnerExp(argPTE, list);
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

    private static LinkedList<PomTaggedExpression> isDiffFrac(PomTaggedExpression pte) {
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