package gov.nist.drmf.interpreter.cas.translation;

import gov.nist.drmf.interpreter.common.constants.GlobalConstants;
import gov.nist.drmf.interpreter.common.grammar.Brackets;
import gov.nist.drmf.interpreter.common.grammar.MathTermTags;
import mlp.MathTerm;
import mlp.PomTaggedExpression;

import javax.annotation.Nullable;
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
    public boolean translate(PomTaggedExpression exp) {
        List<PomTaggedExpression> list = new LinkedList<>();
        list.add(exp);
        return translate(exp);
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
     * @param following_exp the descendants of a previous expression
     * @return true if the parsing process finished successful
     */
    public abstract boolean translate(PomTaggedExpression exp, List<PomTaggedExpression> following_exp);

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

            if (currMathTag != null) {
                switch (currMathTag) {
                    case relation:
                    case operation:
                    case ellipsis:
                        return false;
                }
            }
            if (nextMathTag != null) {
                switch (nextMathTag) {
                    case relation:
                    case operation:
                    case ellipsis:
                        return false;
                    case spaces:
                    case non_allowed:
                        exp_list.remove(0); // remove the \! spaces
                        return addMultiply(currExp, exp_list);
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
                return true;
            }

            Brackets nextBracket = Brackets.getBracket(next.getTermText());
            if (nextBracket != null && !nextBracket.opened) {
                return false;
            }

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
        } catch (Exception e) {
            return true;
        }
    }

    public static String stripMultiParentheses(String expr) {
        if ( !expr.matches("\\(.*\\)") ) return expr;
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
