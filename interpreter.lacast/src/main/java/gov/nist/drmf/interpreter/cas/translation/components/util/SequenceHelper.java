package gov.nist.drmf.interpreter.cas.translation.components.util;

import gov.nist.drmf.interpreter.cas.translation.AbstractListTranslator;
import gov.nist.drmf.interpreter.cas.translation.AbstractTranslator;
import gov.nist.drmf.interpreter.common.exceptions.TranslationException;
import gov.nist.drmf.interpreter.common.exceptions.TranslationExceptionReason;
import gov.nist.drmf.interpreter.pom.common.FeatureSetUtility;
import gov.nist.drmf.interpreter.pom.common.MeomArgumentLimitChecker;
import gov.nist.drmf.interpreter.pom.common.PomTaggedExpressionUtility;
import gov.nist.drmf.interpreter.pom.common.grammar.Brackets;
import gov.nist.drmf.interpreter.pom.common.grammar.ExpressionTags;
import gov.nist.drmf.interpreter.pom.common.grammar.MathTermTags;
import mlp.MathTerm;
import mlp.PomTaggedExpression;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Supplier;

import static gov.nist.drmf.interpreter.cas.common.DLMFPatterns.SPECIAL_SYMBOL_PATTERN_FOR_SPACES;

/**
 * @author Andre Greiner-Petter
 */
public class SequenceHelper {
    private static final Logger LOG = LogManager.getLogger(SequenceHelper.class.getName());

    private final AbstractListTranslator abstractListTranslator;

    private final Supplier<Boolean> isSetMode;

    public SequenceHelper(AbstractListTranslator abstractListTranslator, Supplier<Boolean> isSetMode) {
        this.abstractListTranslator = abstractListTranslator;
        this.isSetMode = isSetMode;
    }

    public void throwIfIsNotSequence(PomTaggedExpression expression) throws TranslationException {
        if (!ExpressionTags.sequence.tag().matches(expression.getTag())) {
            throw TranslationException.buildException(abstractListTranslator,
                    "You used the wrong translation method. " +
                            "The given expression is not a sequence! " +
                            expression.getTag(),
                    TranslationExceptionReason.IMPLEMENTATION_ERROR
            );
        }
    }

    public boolean handleAsBracket(Brackets bracket, List<PomTaggedExpression> followingExps) {
        if ( bracket == null || isSetMode.get() ) return false;

        boolean result = true;
        if ( Brackets.left_angle_brackets.equals(bracket) ) result = checkLeftAngledBracket(bracket, followingExps);
        else if ( Brackets.right_angle_brackets.equals(bracket) ) result = false;
        else if ( !bracket.opened ) {
            throw TranslationException.buildException(abstractListTranslator,
                    "Encountered closing bracket without opening it.",
                    TranslationExceptionReason.WRONG_PARENTHESIS
            );
        }
        return result;
    }

    private boolean checkLeftAngledBracket(Brackets bracket, List<PomTaggedExpression> followingExps) {
        // that might be a relation symbol, so let's check it
        LinkedList<Brackets> stack = new LinkedList<>();
        stack.add( bracket );
        for ( PomTaggedExpression pte : followingExps ) {
            if ( stack.isEmpty() ) return true;

            Brackets b = Brackets.getBracket(pte);
            if ( b != null ) updateBracketStack(stack, b);
            else if ( MeomArgumentLimitChecker.isGeneralBreakPoint(pte.getRoot()) ) break;
        }

        if ( stack.size() > 1 ) {
            LOG.debug("If considering current left-angle-bracket as an open bracket, we encounter a bracket mismatch. " +
                    "Hence we assume it is a relation and continue");
        }

        // so we did not reach an end of this "opened" bracket, like x * <2+3> + x
        return stack.isEmpty();
    }

    private void updateBracketStack(LinkedList<Brackets> stack, Brackets b) {
        if ( b.opened ) {
            stack.addLast(b);
        } else {
            Brackets leastOpened = stack.getLast();
            if (leastOpened.isCounterPart(b) ) {
                stack.removeLast();
            } else {
                // hmm... we discovered mismatched parenthesis
                throw TranslationException.buildException(abstractListTranslator,
                        "Encountered mismatched bracket. Open bracket '"
                                + leastOpened + "' does not match closing '" + b  +"' bracket",
                        TranslationExceptionReason.WRONG_PARENTHESIS
                );
            }
        }
    }

    public boolean isRelationSymbol( PomTaggedExpression exp, List<PomTaggedExpression> expList ) {
        if ( !PomTaggedExpressionUtility.isRelationSymbol(exp) || !isRelationInValidPosition(exp) ) return false;

        Brackets b = Brackets.getBracket(exp);
        if ( b != null ) return !handleAsBracket(b, expList);
        return true;
    }

    private boolean isRelationInValidPosition( PomTaggedExpression exp ) {
        PomTaggedExpression parent = exp.getParent();
        if ( parent == null ) return false;

        if ( PomTaggedExpressionUtility.isSequence(parent) && parent.getParent() == null ){
            return true;
        }

        return PomTaggedExpressionUtility.isSequence(parent) && PomTaggedExpressionUtility.isEquationArray(parent.getParent());
    }

    /**
     * Returns true if there has to be a space symbol following the current expression.
     *
     * @param currExp  the current expression
     * @param expList the following expressions
     * @return true if the current expressions needs an white space symbol behind its translation
     */
    public boolean addSpace(PomTaggedExpression currExp, List<PomTaggedExpression> expList) {
        try {
            Boolean tmp = addSpaceSizeOperatorCheck(currExp, expList);
            if ( tmp != null ) return tmp;

            MathTerm curr = currExp.getRoot();
            MathTerm next = expList.get(0).getRoot();
            return addSpaceParenthesisCheck(curr, next);
        } catch (Exception e) {
            return false;
        }
    }

    private Boolean addSpaceSizeOperatorCheck(PomTaggedExpression currExp, List<PomTaggedExpression> expList) {
        if (expList == null || expList.size() < 1) {
            return false;
        }

        if ( abstractListTranslator.isOpSymbol(currExp) || abstractListTranslator.isOpSymbol(expList.get(0)) )
            return true;

        return null;
    }

    private boolean addSpaceParenthesisCheck(MathTerm curr, MathTerm next) {
        if (FeatureSetUtility.isConsideredAsRelation(curr) || FeatureSetUtility.isConsideredAsRelation(next))
            return true;

        return !(curr.getTag().matches(MathTermTags.PARENTHESIS_PATTERN)
                || next.getTag().matches(MathTermTags.PARENTHESIS_PATTERN)
                || curr.getTermText().matches(SPECIAL_SYMBOL_PATTERN_FOR_SPACES)
                || next.getTermText().matches(SPECIAL_SYMBOL_PATTERN_FOR_SPACES)
        );
    }
}
