package gov.nist.drmf.interpreter.cas.translation;

import gov.nist.drmf.interpreter.cas.logging.TranslatedExpression;
import gov.nist.drmf.interpreter.common.constants.GlobalConstants;
import gov.nist.drmf.interpreter.common.exceptions.TranslationException;
import gov.nist.drmf.interpreter.common.exceptions.TranslationExceptionReason;
import gov.nist.drmf.interpreter.common.grammar.Brackets;
import gov.nist.drmf.interpreter.common.grammar.MathTermTags;
import mlp.MathTerm;
import mlp.PomTaggedExpression;

import java.util.List;
import java.util.regex.Matcher;

import static gov.nist.drmf.interpreter.cas.common.DLMFPatterns.PATTERN_BASIC_OPERATIONS;

/**
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
        throw TranslationException.buildException(
                this,
                "List translators need the following arguments to translate them correctly.",
                TranslationExceptionReason.IMPLEMENTATION_ERROR
        );
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
        if (exp_list == null || exp_list.size() < 1) {
            return false;
        }

        MathTerm next = exp_list.get(0).getRoot();
        if (next.isEmpty()) {
            List<PomTaggedExpression> tmp = exp_list.get(0).getComponents();
            return addMultiply(currExp, tmp);
        }

        MathTerm curr = currExp.getRoot();
        MathTermTags currMathTag = MathTermTags.getTagByKey(curr.getTag());
        MathTermTags nextMathTag = MathTermTags.getTagByKey(next.getTag());

        try {
            Boolean tmp = checkCurrentAndNextTags(currMathTag, nextMathTag, currExp, exp_list);
            return tmp == null ? addMultiplyPreTerms(curr, next) : tmp;
        } catch ( Exception e ) {
            return true;
        }
    }

    private static boolean addMultiplyPreTerms(MathTerm curr, MathTerm next) {
        Brackets nextBracket = Brackets.getBracket(next.getTermText());
        if (nextBracket != null && !nextBracket.opened) {
            return false;
        }

        return checkMultiplyOnTerms(curr, next);
    }

    private static Boolean checkCurrentAndNextTags(
            MathTermTags currMathTag,
            MathTermTags nextMathTag,
            PomTaggedExpression currExp,
            List<PomTaggedExpression> exp_list
    ) {
        Boolean result = checkCurrentMathTag(currMathTag);
        if ( result == null ) result = checkNextMathTag(nextMathTag, currExp, exp_list);
        if ( result != null ) return result;

        boolean currentLetter = isLetter(currMathTag);
        boolean nextLetter = isLetter(nextMathTag);

        if (currentLetter && nextLetter) {
            return true;
        } else return null;
    }

    private static boolean isLetter(MathTermTags tag) {
        return MathTermTags.letter.equals(tag) || MathTermTags.alphanumeric.equals(tag) || MathTermTags.constant.equals(tag);
    }

    private static Boolean checkCurrentMathTag(MathTermTags currMathTag) {
        if (currMathTag != null) {
            switch (currMathTag) {
                case relation:
                case operation:
                case ellipsis:
                    return false;
            }
        }
        return null;
    }

    private static Boolean checkNextMathTag(MathTermTags nextMathTag, PomTaggedExpression currExp, List<PomTaggedExpression> exp_list) {
        if (nextMathTag != null) {
            switch (nextMathTag) {
                case relation:
                case operation:
                case ellipsis:
                case right_brace:
                case right_parenthesis:
                case right_bracket:
                    return false;
                case spaces:
                case non_allowed:
                    exp_list.remove(0); // remove the \! spaces
                    return addMultiply(currExp, exp_list);
            }
        }
        return null;
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

        boolean operation = checkOperation(curr, next);
        boolean parenthesis = checkBrackets(curr, next);

        return !(operation || parenthesis);
    }

    private static boolean checkOperation(MathTerm curr, MathTerm next) {
        return curr.getTermText().matches(PATTERN_BASIC_OPERATIONS)
                || next.getTermText().matches(PATTERN_BASIC_OPERATIONS);
    }

    private static boolean checkBrackets(MathTerm curr, MathTerm next) {
        boolean currPara = curr.getTermText().matches(Brackets.CLOSED_PATTERN)
                || curr.getTermText().matches(Brackets.OPEN_PATTERN);

        return currPara
                || next.getTermText().matches(Brackets.CLOSED_PATTERN)
                || next.getTermText().matches(Brackets.OPEN_PATTERN);
    }

    /**
     *
     * @param expr
     * @return
     */
    public static String stripMultiParentheses(String expr) {
        if ( expr == null || !expr.matches("\\(.*\\)") )
            return expr;

        int open = 1;
        for ( int i = 1; i < expr.length(); i++ ) {
            open = update(expr, i, open);
            if ( stop(expr, i, open) ) return expr;
        }

        return expr.substring(1, expr.length()-1);
    }

    private static int update(String expr, int idx, int open) {
        Character c = expr.charAt(idx);
        if ( c.equals(')') ) open--;
        if ( c.equals('(') ) open++;
        return open;
    }

    private static boolean stop(String expr, int idx, int open) {
        return open == 0 && idx < expr.length()-1;
    }
}
