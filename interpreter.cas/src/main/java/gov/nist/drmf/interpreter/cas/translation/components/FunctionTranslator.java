package gov.nist.drmf.interpreter.cas.translation.components;

import gov.nist.drmf.interpreter.cas.logging.TranslatedExpression;
import gov.nist.drmf.interpreter.cas.translation.AbstractListTranslator;
import gov.nist.drmf.interpreter.cas.translation.AbstractTranslator;
import gov.nist.drmf.interpreter.common.grammar.Brackets;
import gov.nist.drmf.interpreter.common.grammar.MathTermTags;
import mlp.MathTerm;
import mlp.PomTaggedExpression;

import java.util.List;

/**
 * The function translation parses simple functions and not special functions.
 * These "simple" functions are functions without a DLMF macro. We don't
 * really know how to translate these functions. So we will translate them
 * by simply remove the backslash.
 *
 * If the global-lexicon doesn't contains the cosine function it is just a
 * simple function than. When our lexicon is complete, this translation becomes
 * a bit redundant.
 *
 * Like the MacroTranslator, the function translation should translate the start expression
 * as well (the function itself) and after that the argument.
 *
 * For instance: cos{2}
 *  1) translate the expression cos first
 *  2) after that the list of arguments, here 2
 *
 * @see Brackets
 * @see AbstractTranslator
 * @see AbstractListTranslator
 * @see TranslatedExpression
 * @author Andre Greiner-Petter
 */
public class FunctionTranslator extends AbstractListTranslator {
    /**
     *
     * @param exp
     * @param following
     * @return
     */
    @Override
    public boolean translate( PomTaggedExpression exp, List<PomTaggedExpression> following ){
        boolean return_value;
        return_value = translate(exp);
        return_value &= parse(following);
        int num = local_inner_exp.mergeAll(); // a bit redundant, num is always 2!
        global_exp.mergeLastNExpressions( num );
        return return_value;
    }

    /**
     * This translate method has to be invoked before {@link #parse(List)}.
     * It only parses the function itself (like cos(2), cos is the first part).
     *
     * @param exp the first expression that contains the function
     *            (it contains cos, for instance)
     * @return true when everything is good
     */
    @Override
    public boolean translate(PomTaggedExpression exp){
        MathTerm term = exp.getRoot();

        // remove the starting backslash
        String output;
        if ( term.getTermText().startsWith( CHAR_BACKSLASH ) )
            output = term.getTermText().substring(1);
        else output = term.getTermText();

        // add it to global and local
        local_inner_exp.addTranslatedExpression(output);
        global_exp.addTranslatedExpression(output);

        // inform the user that we usually don't know how to handle it.
        INFO_LOG.addGeneralInfo(
                term.getTermText(),
                "Function without DLMF-Definition. " +
                        "We cannot translate it and keep it like it is (but delete prefix \\ if necessary)."
        );
        return true;
    }

    /**
     * The second part of the translation function simply parses the argument part of
     * an unknown function. For instance if \cos(2+2), this translate method gets
     * 2+2 as argument list.
     * @param following_exp the descendants of a previous function {@link #translate(PomTaggedExpression)}
     * @return true if everything was fine
     */
    private boolean parse(List<PomTaggedExpression> following_exp) {
        // get first expression
        PomTaggedExpression first = following_exp.remove(0);

        // if it starts with a caret, we have a little problem.
        // classical case \cos^b(a). This is typical and easy
        // to read for people but hard to understand for CAS.
        // usually we translate it the way around: \cos(a)^b.
        // That's why we need to check this here!
        boolean caret = false;
        TranslatedExpression powerExp = null;
        if ( containsTerm(first) ){
            MathTermTags tag = MathTermTags.getTagByKey(first.getRoot().getTag());
            //noinspection ConstantConditions
            if (tag.equals( MathTermTags.caret )){
                // damn it, it's really a caret -.-'
                caret = true;

                // since the MathTermTranslator handles this, use this class
                MathTermTranslator mp = new MathTermTranslator();
                if ( !mp.translate( first ) ) return false;
                powerExp = mp.getTranslatedExpressionObject();

                // remove the power from global_exp first
                global_exp.removeLastNExps( powerExp.getLength() );

                // and now, merge the power to one object and put parenthesis around it
                // if necessary
                if ( powerExp.getLength() > 1 )
                    powerExp.mergeAllWithParenthesis();
                else powerExp.mergeAll();

                // finally, take the real starting point!
                first = following_exp.remove(0);
            }
        }

        // translate the argument in the general way
        TranslatedExpression translation = parseGeneralExpression(first, following_exp);

        // find out if we should wrap parenthesis around or not
        int num;
        if ( !testBrackets( translation.toString() ) ){
            num = translation.mergeAllWithParenthesis();
        } else {
            num = translation.mergeAll();
        }
        local_inner_exp.addTranslatedExpression( translation );
        // remove all variables and put them together as one object
        global_exp.removeLastNExps( num );
        global_exp.addTranslatedExpression( translation );

        // shit, if there was a caret before the arguments, we need to add
        // these now
        if ( caret ){
            local_inner_exp.replaceLastExpression( translation + powerExp.toString() );
            global_exp.replaceLastExpression( translation + powerExp.toString() );
        }

        return true;
    }
}
