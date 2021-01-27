package gov.nist.drmf.interpreter.cas.translation.components.util;

import gov.nist.drmf.interpreter.cas.blueprints.BlueprintMaster;
import gov.nist.drmf.interpreter.cas.blueprints.MathematicalEssentialOperatorMetadata;
import gov.nist.drmf.interpreter.cas.logging.TranslatedExpression;
import gov.nist.drmf.interpreter.cas.translation.AbstractListTranslator;
import gov.nist.drmf.interpreter.cas.translation.AbstractTranslator;
import gov.nist.drmf.interpreter.common.exceptions.TranslationException;
import gov.nist.drmf.interpreter.common.exceptions.TranslationExceptionReason;
import gov.nist.drmf.interpreter.pom.common.MeomArgumentLimitChecker;
import gov.nist.drmf.interpreter.pom.common.grammar.Brackets;
import gov.nist.drmf.interpreter.pom.common.grammar.ExpressionTags;
import gov.nist.drmf.interpreter.pom.common.grammar.LimitedExpressions;
import gov.nist.drmf.interpreter.pom.common.grammar.MathTermTags;
import gov.nist.drmf.interpreter.pom.extensions.PrintablePomTaggedExpression;
import mlp.MathTerm;
import mlp.PomTaggedExpression;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.LinkedList;
import java.util.List;

/**
 * @author Andre Greiner-Petter
 */
public class MeomArgumentExtractor {
    private static final Logger LOG = LogManager.getLogger(MeomArgumentExtractor.class.getName());

    private enum RETURN_VAL {
        CACHE, CONTINUE, NONE
    }

    private final List<PomTaggedExpression> list;
    private final List<String> currVars;
    private final AbstractTranslator abstractTranslator;
    private int innerInts;

    private MeomArgumentExtractor(
            List<PomTaggedExpression> list,
            List<String> currVars,
            AbstractTranslator abstractTranslator
    ) {
        this.list = list;
        this.currVars = currVars;
        this.abstractTranslator = abstractTranslator;
        this.innerInts = 0;
    }

    private RETURN_VAL handleNonEmptyTag(
            LinkedList<PomTaggedExpression> cache,
            LinkedList<Brackets> parenthesisCache,
            MathTerm mt
    ) {
        Brackets bracket = Brackets.ifIsBracketTransform(mt, parenthesisCache.isEmpty() ? null : parenthesisCache.getLast());
        RETURN_VAL value = RETURN_VAL.NONE;
        // check for brackets
        if ( handleBracket(bracket, parenthesisCache, abstractTranslator) ) {
            value = RETURN_VAL.CACHE;
        } else if ( !parenthesisCache.isEmpty() ) {
            cache.addLast(list.remove(0));
            value = RETURN_VAL.CONTINUE;
        } else if ( isSumOrProduct(mt) ) {
            value = handleSumAndProd(list, cache, currVars, abstractTranslator);
        } else if ( mt.getTermText().matches("\\\\diffd?") ) {
            value = updateDiffD();
        } else if ( LimitedExpressions.isIntegral(mt) ) {
            innerInts++;
        } else {
            value = MeomArgumentLimitChecker.isGeneralBreakPoint(mt) ? RETURN_VAL.CACHE : RETURN_VAL.NONE;
        }
        return value;
    }

    private boolean isSumOrProduct(MathTerm mt) {
        return LimitedExpressions.isSum(mt) || LimitedExpressions.isProduct(mt);
    }

    private RETURN_VAL updateDiffD() {
        if ( innerInts > 0 ) innerInts--;
        else {
            list.remove(0); // diff or diffd... so get next for arg
            PomTaggedExpression argPTE = list.remove(0);
            TranslatedExpression argTe = abstractTranslator.translateInnerExp(argPTE, list);
            currVars.add(argTe.toString());
            return RETURN_VAL.CACHE;
        }
        return RETURN_VAL.NONE;
    }

    private RETURN_VAL handleSumAndProd(
            List<PomTaggedExpression> list,
            LinkedList<PomTaggedExpression> cache,
            List<String> currVars,
            AbstractTranslator abstractTranslator
    ) {
        // there is a special case where we also have to stop...
        // if a new limited expression is coming that shares the same variable

        // in this case, the next element are the limits. So lets analyze them in advance
        PomTaggedExpression nextLimits = list.get(1);
        LimitAnalyzer limitAnalyzer = new LimitAnalyzer(abstractTranslator);
        MathematicalEssentialOperatorMetadata nextL = limitAnalyzer.extractLimitsWithoutParsing(
                nextLimits,
                new LinkedList<>(),
                BlueprintMaster.LIMITED
        );

        for ( String nextVar : nextL.getVars() ){
            if ( currVars.contains(nextVar) ) {
                LOG.debug("Limited expression breakpoint reached (reason: sharing variables)");
                // there is a match in variables... so, we reached a breakpoint
                updateListWithCache(cache, list);
                return RETURN_VAL.CACHE;
            }
        }
        return RETURN_VAL.NONE;
    }

    private static void updateListWithCache(LinkedList<PomTaggedExpression> cache, List<PomTaggedExpression> list) {
        if ( !cache.isEmpty() ) {
            PomTaggedExpression last = cache.getLast();
            MathTerm t = last.getRoot();
            if ( !t.isEmpty() && t.getTermText().matches("\\s*[+-.,;^/*]\\s*") ) {
                cache.removeLast();
                list.add(0, last);
            }
        }
    }

    private boolean checkDifferentiationFraction(
            PomTaggedExpression currentP,
            List<PomTaggedExpression> updateList,
            List<PomTaggedExpression> elementsList
    ) {
        LinkedList<PomTaggedExpression> fracList = isDiffFrac(currentP);
        if ( fracList != null ) {
            updateList.remove(0);
            while (!fracList.isEmpty())
                elementsList.add(0, fracList.removeLast());
//            updateList.add(elementsList.remove(0));
            return true;
        } else return false;
    }

    private LinkedList<PomTaggedExpression> isDiffFrac(PomTaggedExpression pte) {
        ExpressionTags pt = ExpressionTags.getTagByKey(pte.getTag());
        if ( ExpressionTags.fraction.equals(pt) ) {
            PomTaggedExpression numeratorPTE = pte.getComponents().get(0);
            ExpressionTags it = ExpressionTags.getTagByKey(numeratorPTE.getTag());

            if ( !ExpressionTags.sequence.equals(it) )
                return null; // \\diff{x} is always a sequence

            LinkedList<PomTaggedExpression> list = createListWithDiffAtEnd(numeratorPTE);

            list.addFirst(pte);
            return list;
        }
        return null;
    }

    private static LinkedList<PomTaggedExpression> createListWithDiffAtEnd(PomTaggedExpression numeratorPTE) {
        LinkedList<PomTaggedExpression> list = new LinkedList<>();
        LinkedList<PomTaggedExpression> temp = new LinkedList<>();

        List<PomTaggedExpression> seq = new LinkedList<>(numeratorPTE.getComponents());
        while ( !seq.isEmpty() ) {
            PomTaggedExpression e = seq.remove(0);
            MathTerm mt = e.getRoot();
            if ( !mt.isEmpty() && mt.getTermText().matches("\\\\diffd?") ) {
                list.addLast(e);
                list.addLast(seq.remove(0));
                continue;
            }

            temp.addLast(e);
        }

        if ( !temp.isEmpty() ) {
            numeratorPTE.setComponents(temp);
        } else {
            MathTerm oneMT = new MathTerm("1", "digit", "numeric", "numerator");
            numeratorPTE.setComponents(new PrintablePomTaggedExpression(oneMT));
        }

        return list;
    }

    /**
     * Removes and returns all elements that may potentially be in the scope of a limited expression.
     * This does not check for appearances of variables, it simply takes all expressions until a
     * breakpoint is reached. Breakpoints are relations (equal signs etc.) and closed brackets (if
     * the brackets were not opened within the scope).
     * @param list following expressions (will be modified by this function)
     * @param currVars list of variables
     * @return a sublist of {@param list}, note that {@param list} will be shortened
     */
    public static List<PomTaggedExpression> getPotentialArgumentsUntilEndOfScope(
            List<PomTaggedExpression> list,
            List<String> currVars,
            AbstractTranslator abstractTranslator
    ) {
        LinkedList<PomTaggedExpression> cache = new LinkedList<>();
        LinkedList<Brackets> parenthesisCache = new LinkedList<>();

        checkListValidity(list, abstractTranslator);

        // the very next element is always part of the argument if it is not a breakpoint
        if ( MeomArgumentLimitChecker.isBreakPoint(list.get(0)) ) {
            return cache;
        }

        PomTaggedExpression first = list.remove(0);
        cache.add(first);

        // now add all until there is a stop expression
        MeomArgumentExtractor variableExtractor = new MeomArgumentExtractor(list, currVars, abstractTranslator);

        // first element could be a parenthesis also... than take all elements until this parenthesis is closed
        if (!checkFirstBracket(parenthesisCache, first, abstractTranslator)) {
            if ( !first.getRoot().isEmpty() && LimitedExpressions.isIntegral(first.getRoot())){
                variableExtractor.innerInts++;
            }

            if ( variableExtractor.checkDifferentiationFraction(first, cache, list) ) {
                cache.add(list.remove(0));
            }
        }

        iterateOverList(variableExtractor, cache, list, parenthesisCache);

        // well, it might be the entire expression until the end, of course
        return cache;
    }

    private static void iterateOverList(
            MeomArgumentExtractor variableExtractor,
            LinkedList<PomTaggedExpression> cache,
            List<PomTaggedExpression> list,
            LinkedList<Brackets> parenthesisCache
    ) {
        while ( !list.isEmpty() ) {
            PomTaggedExpression curr = list.get(0); // do not remove yet!
            MathTerm mt = curr.getRoot();
            // if the tag is null, it might be a fraction. if not, there are multiple options
            RETURN_VAL val = RETURN_VAL.NONE;
            list.remove(0);
            if ( AbstractListTranslator.upcomingConstraint(curr, list) ) {
                list.add(0, curr);
                return;
            }
            list.add(0, curr);

            if ( mt.getTag() != null ) {
                val = variableExtractor.handleNonEmptyTag(cache, parenthesisCache, mt);
            } else {
                variableExtractor.checkDifferentiationFraction(curr, list, list);
            }

            if ( RETURN_VAL.CACHE.equals(val) ) return;
            else if ( RETURN_VAL.CONTINUE.equals(val) ) continue;

            // if no stopper is found, just add it to the potential list
            cache.addLast(list.remove(0));
        }
    }

    private static void checkListValidity(List<PomTaggedExpression> list, AbstractTranslator abstractTranslator) {
        if ( list.isEmpty() ) {
            throw TranslationException.buildException(
                    abstractTranslator,
                    "A limited expression ends with no argument left.",
                    TranslationExceptionReason.INVALID_LATEX_INPUT);
        }
    }

    private static boolean checkFirstBracket(LinkedList<Brackets> parenthesisCache, PomTaggedExpression first, AbstractTranslator abstractTranslator) {
        Brackets bracket = Brackets.ifIsBracketTransform(first.getRoot(), null);
        if ( bracket != null ) {
            if ( !bracket.opened ) throw TranslationException.buildException(
                    abstractTranslator,
                    "Empty arguments for limited expressions are invalid math.",
                    TranslationExceptionReason.INVALID_LATEX_INPUT);
            parenthesisCache.addLast(bracket);
            return true;
        }
        return false;
    }

    private static boolean handleBracket(Brackets bracket, LinkedList<Brackets> parenthesisCache, AbstractTranslator abstractTranslator) {
        if ( bracket == null ) return false;

        // if new bracket opens, add it to cache
        if ( bracket.opened ) {
            parenthesisCache.addLast(bracket);
        } else {
            if ( parenthesisCache.isEmpty() ) {
                // in case a bracket is closed that was not opened in this scope, we reached a breakpoint
                LOG.debug("Limited expression breakpoint reached (reason: outer closing parenthesis)");
                return true;
            } else if ( parenthesisCache.getLast().counterpart.equals(bracket.symbol) ) {
                // in case the bracket closes a previously opened bracket, update cache
                parenthesisCache.removeLast();
            } else { // well, that's an illegal situation, parentheses does not match
                throw TranslationException.buildException(
                        abstractTranslator,
                        "Open and close parentheses does not match!",
                        TranslationExceptionReason.WRONG_PARENTHESIS);
            }
        }
        return false;
    }
}
