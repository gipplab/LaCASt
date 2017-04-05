package gov.nist.drmf.interpreter.cas.translation;

import javax.annotation.Nullable;

import gov.nist.drmf.interpreter.common.GlobalConstants;
import gov.nist.drmf.interpreter.common.TranslationException;
import gov.nist.drmf.interpreter.common.grammar.Brackets;
import gov.nist.drmf.interpreter.common.grammar.MathTermTags;
import mlp.MathTerm;
import mlp.PomTaggedExpression;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;

/**
 * TODO
 *
 * @author Andre Greiner-Petter
 */
public abstract class AbstractListTranslator extends AbstractTranslator {
    public static final String SPECIAL_SYMBOL_PATTERN_FOR_SPACES =
            "[\\^\\/\\_\\!]";

    public static final String PATTERN_BASIC_OPERATIONS =
            ".*[+\\-*/\\^_!{}\\[\\]<>\\s=]|\\\\[ci]dot.*";

    // Array of parsed strings
    protected String[] components;

    /**
     * Use this method only when you know what you are doing.
     *
     * @param exp single expression gets wrapped into a list
     * @return true if the parsing process finished correctly
     */
    @Override
    public boolean translate(PomTaggedExpression exp){
        List<PomTaggedExpression> list = new LinkedList<>();
        list.add(exp);
        return translate(exp);
    }

    public abstract boolean translate( PomTaggedExpression exp, List<PomTaggedExpression> following_exp );

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
    //public abstract boolean translate(List<PomTaggedExpression> following_exp);

    /**
     * Returns parsed components. Be aware this could be null or
     * could contain older results!
     * @return array of components.
     */
    @Nullable
    public String[] getComponents(){
        return components;
    }

    public static boolean addMultiply( PomTaggedExpression currExp, List<PomTaggedExpression> exp_list ){
        try {
            if ( exp_list == null || exp_list.size() < 1) return false;
            MathTerm curr = currExp.getRoot();
            MathTerm next = exp_list.get(0).getRoot();

            if ( next.getTermText().matches( Brackets.CLOSED_PATTERN ) )
                return false;

            Matcher m1 = GlobalConstants.LATEX_MULTIPLY_PATTERN.matcher(curr.getTermText());
            Matcher m2 = GlobalConstants.LATEX_MULTIPLY_PATTERN.matcher(next.getTermText());
            if ( m1.matches() || m2.matches() ) return false;

            //System.out.println(curr.getTermText() + " <-> " + next.getTermText());
            if ( curr.getTermText().matches( Brackets.CLOSED_PATTERN ) ) {
                return !next.getTermText().matches( PATTERN_BASIC_OPERATIONS );
            } else if ( next.getTermText().matches( Brackets.OPEN_PATTERN ) ){
                return !curr.getTermText().matches( PATTERN_BASIC_OPERATIONS );
            }

            return !(
                    curr.getTermText().matches( PATTERN_BASIC_OPERATIONS )
                            || next.getTermText().matches( PATTERN_BASIC_OPERATIONS )
                            || curr.getTag().matches( MathTermTags.operation.tag() )
                            || next.getTag().matches( MathTermTags.operation.tag() )
                            || curr.getTag().matches( MathTermTags.ellipsis.tag() )
                            || next.getTag().matches( MathTermTags.ellipsis.tag() )
                            || curr.getTermText().matches( Brackets.CLOSED_PATTERN )
                            || curr.getTermText().matches( Brackets.OPEN_PATTERN )
                            || next.getTermText().matches( Brackets.CLOSED_PATTERN )
                            || next.getTermText().matches( Brackets.OPEN_PATTERN )
            );
        } catch ( Exception e ){ return true; }
    }
}
