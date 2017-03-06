package gov.nist.drmf.interpreter.cas.translation.components;

import gov.nist.drmf.interpreter.cas.logging.TranslatedExpression;
import gov.nist.drmf.interpreter.cas.translation.AbstractTranslator;
import gov.nist.drmf.interpreter.cas.translation.SemanticLatexTranslator;
import gov.nist.drmf.interpreter.common.TranslationException;
import gov.nist.drmf.interpreter.common.grammar.Brackets;
import gov.nist.drmf.interpreter.common.grammar.ExpressionTags;
import gov.nist.drmf.interpreter.common.grammar.MathTermTags;
import mlp.PomTaggedExpression;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @see ExpressionTags
 * @author Andre Greiner-Petter
 */
public class EmptyExpressionTranslator extends AbstractTranslator {
    @Override
    public boolean translate(PomTaggedExpression expression ){
        // switch-case over tags
        String tag = expression.getTag();
        ExpressionTags expTag = ExpressionTags.getTagByKey(tag);

        // no tag shouldn't happen
        if ( expTag == null ){
            ERROR_LOG.warning("Could not find tag: " + tag);
            return false;
        }

        // switch over all possible tags
        switch( expTag ){
            // it's a sequence!
            case sequence: // in that case use the SequenceTranslator
                // this don't write into global_exp!
                // it only delegates the parsing process to the SequenceTranslator
                SequenceTranslator p = new SequenceTranslator();
                if ( p.translate( expression ) ){
                    local_inner_exp.addTranslatedExpression( p.getTranslatedExpressionObject() );
                    return true;
                } else return false;
            case fraction:
            case binomial:
            case square_root:
            case general_root:
                // all of them has sub-elements.
                return parseBasicFunction( expression, expTag );
            case balanced_expression:
                // balanced expressions are expressions in \left( x \right)
                return parseBalancedExpression( expression );
            case sub_super_script:
            case numerator:
            case denominator:
            case equation:
            default:
                ERROR_LOG.warning("Reached unknown or not yet supported expression tag: " + tag);
                return false;
        }
    }

    private boolean parseBasicFunction( PomTaggedExpression top_exp, ExpressionTags tag )
            throws TranslationException {
        // extract all components from top expressions
        String[] comps = extractMultipleSubExpressions( top_exp );
        if ( isInnerError() ){
            // something went wrong while extracting expressions
            return false;
        }

        // first of all, translate components into translation
        local_inner_exp.addTranslatedExpression(
                // try to translate the basic function
                SemanticLatexTranslator.getBasicFunctionParser().translate(
                        comps,
                        tag.tag()
                )
        );
        // finally, global_exp needs to be updated
        // it doesn't contains sub expressions because
        // extractMultipleSubExpressions already deleted it.
        global_exp.addTranslatedExpression(
                local_inner_exp
        );
        // everything goes well
        return true;
    }

    private boolean parseBalancedExpression( PomTaggedExpression top_exp ){
        // get subexpressions once more
        List<PomTaggedExpression> sub_exps = top_exp.getComponents();
        // the size is at least 2 because \left( and \right) are 2 elements
        if ( sub_exps.size() < 3 ){ // nothing between the parenthesis -> ignore
            ERROR_LOG.warning("Found empty expression and ignored it.");
            return true;
        }

        // get first and last (these are usually \left( and \right) )
        PomTaggedExpression first = sub_exps.remove(0);
        PomTaggedExpression last = sub_exps.remove( sub_exps.size()-1 );

        // test open-close style of first-last
        if ( !testParanthesis(first, last) ){
            ERROR_LOG.severe("Error in delimiters. " +
                    "The open delimiter doesn't fit with the closed delimiter.");
            return false;
        }

        // finally we can translate the inner part
        TranslatedExpression inner_translation =
                parseGeneralExpression( sub_exps.remove(0), sub_exps );

        if ( isInnerError() ) return false;

        // merge all together because it is a sub-expression
        int num = inner_translation.mergeAll();

        // wrap it with parenthesis
        local_inner_exp.addTranslatedExpression(
                Brackets.left_parenthesis.symbol +
                        inner_translation.toString() +
                        Brackets.left_parenthesis.counterpart
        );

        // clear temporary last objects
        global_exp.removeLastNExps( num );
        // and update the last object with the whole last expression
        global_exp.addTranslatedExpression( local_inner_exp );
        return true;
    }

    /**
     * A helper method to extract some sub-expressions. Useful for short
     * functions like \frac{a}{b}. The given argument is the parent expression
     * of several children. As an example a fraction expression has two children,
     * the numerator and the denominator.
     *
     * @param top_expression parent expression of underlying sub-expressions.
     * @return true if the parsing process finished successful
     */
    private String[] extractMultipleSubExpressions( PomTaggedExpression top_expression ){
        List<PomTaggedExpression> sub_expressions = top_expression.getComponents();
        ArrayList<TranslatedExpression> components = new ArrayList<>(sub_expressions.size());

        while ( !sub_expressions.isEmpty() ){
            PomTaggedExpression exp = sub_expressions.remove(0);
            TranslatedExpression inner_exp = parseGeneralExpression(exp, sub_expressions);
            int num = inner_exp.mergeAll();
            components.add(inner_exp);
            global_exp.removeLastNExps( num ); // remove all previous sub-elements
        }

        String[] output = new String[components.size()];
        for ( int i = 0; i < output.length; i++ )
            output[i] = components.get(i).toString();

        return output;
    }

    /**
     * Test on the hard way. If any exception throws, this method
     * catch it and returns false. It only returns true if the
     * first element and the last element are corresponding
     * brackets. Kind of the brackets doesn't matter but they
     * have to correspond to each other.
     * @param first for istance \left(
     * @param last for instance \right)
     * @return true if first and last matches else false
     */
    @SuppressWarnings( "all" )
    private boolean testParanthesis( PomTaggedExpression first, PomTaggedExpression last ){
        try {
            MathTermTags ftag = MathTermTags.getTagByKey( first.getRoot().getTag() );
            MathTermTags ltag = MathTermTags.getTagByKey( last.getRoot().getTag() );
            if ( !ftag.equals( MathTermTags.left_delimiter ) ) return false;
            if ( !ltag.equals( MathTermTags.right_delimiter ) ) return false;
            String left = first.getRoot().getTermText();
            left = left.substring( left.length()-1 ); // last symbol (
            String right = last.getRoot().getTermText();
            right = right.substring( right.length()-1 ); // last symbol )
            Brackets lBracket = Brackets.getBracket(left);
            Brackets rBracket = Brackets.getBracket(right);
            return Brackets.getBracket(lBracket.counterpart).equals(rBracket);
        } catch ( Exception e ){
            return false;
        }
    }
}
