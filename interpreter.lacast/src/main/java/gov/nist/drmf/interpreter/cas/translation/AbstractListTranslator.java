package gov.nist.drmf.interpreter.cas.translation;

import gov.nist.drmf.interpreter.cas.logging.TranslatedExpression;
import gov.nist.drmf.interpreter.cas.translation.components.MathTermTranslator;
import gov.nist.drmf.interpreter.common.constants.GlobalConstants;
import gov.nist.drmf.interpreter.common.exceptions.TranslationException;
import gov.nist.drmf.interpreter.common.exceptions.TranslationExceptionReason;
import gov.nist.drmf.interpreter.common.grammar.Brackets;
import gov.nist.drmf.interpreter.common.grammar.MathTermTags;
import mlp.MathTerm;
import mlp.PomTaggedExpression;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;

import static gov.nist.drmf.interpreter.cas.common.DLMFPatterns.PATTERN_BASIC_OPERATIONS;

/**
 * TODO
 *
 * @author Andre Greiner-Petter
 */
public abstract class AbstractListTranslator extends AbstractTranslator {
    protected AbstractListTranslator(AbstractTranslator abstractTranslator) {
        super(abstractTranslator);
    }

    /**
     * Use this method only when you know what you are doing.
     *
     * @param exp single expression gets wrapped into a list
     * @return true if the parsing process finished correctly
     */
    @Override
    public TranslatedExpression translate(PomTaggedExpression exp) {
        throw buildException("List translators need the following arguments to translate them correctly.",
                TranslationExceptionReason.IMPLEMENTATION_ERROR);
    }

    /**
     * The general method to translate a list of descendants.
     * The list should not contain the first element.
     *
     * For instance, let us assume our list contains:
     *      ( 2 + 3 )
     * Than this method should translate only the following commands:
     *        2 + 3 )
     *
     * @param exp the current expression
     * @param following_exp the descendants of a previous expression
     * @return the translated expression
     */
    public abstract TranslatedExpression translate(
            PomTaggedExpression exp,
            List<PomTaggedExpression> following_exp
    );

    /**
     * Checks weather a multiplication symbol should be added after the current {@param currExp} and
     * the following element (which is the first element of {@param exp_list}).
     * @param currExp the current node
     * @param exp_list the following siblings
     * @return true if between current and the next sibling should be a multiplication symbol, otherwise false
     */
    public static boolean addMultiply(PomTaggedExpression currExp, List<PomTaggedExpression> exp_list) {
        try {
            if (exp_list == null || exp_list.size() < 1) {
                return false;
            }
            MathTerm curr = currExp.getRoot();
            MathTerm next = exp_list.get(0).getRoot();
            if (next.isEmpty()) {
                List<PomTaggedExpression> tmp = exp_list.get(0).getComponents();
                return addMultiply(currExp, tmp);
            }

            MathTermTags currMathTag = MathTermTags.getTagByKey(curr.getTag());
            MathTermTags nextMathTag = MathTermTags.getTagByKey(next.getTag());

            int tmp = checkCurrentAndNextTags(currMathTag, nextMathTag, currExp, exp_list);
            if ( tmp == 1 ) return true;
            else if ( tmp == 0 ) return false;
            // in case of tmp == 2, just continue

            Brackets nextBracket = Brackets.getBracket(next.getTermText());
//            if ( thisBracket != null && nextBracket != null ) {
//                if ( thisBracket.equals(Brackets.abs_val) && nextBracket.equals(Brackets.abs_val) )
//                    return true;
//                if ( thisBracket.equals(Brackets.abs_val) )
//                    return nextBracket.opened; // if next is open, add *, otherwise not
//                if ( nextBracket.equals(Brackets.abs_val) )
//                    return !thisBracket.opened;
//            }

            if (nextBracket != null && !nextBracket.opened) {
                return false;
            }

//            if ( thisBracket != null && nextBracket != null ) {
//                return !thisBracket.opened && nextBracket.opened;
//            }

            return checkMultiplyOnTerms(curr, next);
        } catch (Exception e) {
            return true;
        }
    }

    private static short checkCurrentAndNextTags(
            MathTermTags currMathTag,
            MathTermTags nextMathTag,
            PomTaggedExpression currExp,
            List<PomTaggedExpression> exp_list
    ) {
        if (currMathTag != null) {
            switch (currMathTag) {
                case relation:
                case operation:
                case ellipsis:
                    return 0;
            }
        }
        if (nextMathTag != null) {
            switch (nextMathTag) {
                case relation:
                case operation:
                case ellipsis:
                case right_brace:
                case right_parenthesis:
                case right_bracket:
                    return 0;
                case spaces:
                case non_allowed:
                    exp_list.remove(0); // remove the \! spaces
                    if (addMultiply(currExp, exp_list))
                        return 1;
                    else return 0;
            }
        }

        if (
                (
                        currMathTag.equals(MathTermTags.letter) ||
                                currMathTag.equals(MathTermTags.alphanumeric) ||
                                currMathTag.equals(MathTermTags.constant)
                ) && (
                        nextMathTag.equals(MathTermTags.letter) ||
                                nextMathTag.equals(MathTermTags.alphanumeric) ||
                                nextMathTag.equals(MathTermTags.constant)
                )) {
            return 1;
        }

        return 2;
    }

    private static boolean checkMultiplyOnTerms(MathTerm curr, MathTerm next) {
        Matcher m1 = GlobalConstants.LATEX_MULTIPLY_PATTERN.matcher(curr.getTermText());
        Matcher m2 = GlobalConstants.LATEX_MULTIPLY_PATTERN.matcher(next.getTermText());
        if (m1.matches() || m2.matches()) {
            return false;
        }

        //System.out.println(curr.getTermText() + " <-> " + next.getTermText());
        if (curr.getTermText().matches(Brackets.CLOSED_PATTERN)) {
            return
                    !(next.getTermText().matches(PATTERN_BASIC_OPERATIONS));
        } else if (next.getTermText().matches(Brackets.OPEN_PATTERN)) {
            return !curr.getTermText().matches(PATTERN_BASIC_OPERATIONS);
        }

        return !(
                curr.getTermText().matches(PATTERN_BASIC_OPERATIONS)
                        || next.getTermText().matches(PATTERN_BASIC_OPERATIONS)
                        || curr.getTermText().matches(Brackets.CLOSED_PATTERN)
                        || curr.getTermText().matches(Brackets.OPEN_PATTERN)
                        || next.getTermText().matches(Brackets.CLOSED_PATTERN)
                        || next.getTermText().matches(Brackets.OPEN_PATTERN)
        );
    }

    /**
     * Sub-Super scripts will be normalized, so that the subscript is always in front
     * @param pte
     * @return
     */
    public static PomTaggedExpression normalizeSubSuperScripts( PomTaggedExpression pte ) {
        List<PomTaggedExpression> comps = pte.getComponents();

        PomTaggedExpression first = comps.remove(0);
        PomTaggedExpression second = comps.remove(0);

        MathTerm firstMT = first.getRoot();
        MathTermTags ftag = MathTermTags.getTagByKey(firstMT.getTag());
        if ( ftag.equals(MathTermTags.caret) ) {
            // caret first, switch the order!
            pte.addComponent(second);
            pte.addComponent(first);
        } else {
            pte.addComponent(first);
            pte.addComponent(second);
        }
        return pte;
    }

    public static String stripMultiParentheses(String expr) {
        if ( expr == null || !expr.matches("\\(.*\\)") ) return expr;
        int open = 1;
        for ( int i = 1; i < expr.length(); i++ ) {
            Character c = expr.charAt(i);
            if ( c.equals(')') ) open--;
            if ( c.equals('(') ) open++;
            if ( open == 0 && i < expr.length()-1 ) return expr;
        }
        return expr.substring(1, expr.length()-1);
    }
}
