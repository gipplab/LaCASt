package gov.nist.drmf.interpreter.cas.translation.components;

import gov.nist.drmf.interpreter.cas.logging.TranslatedExpression;
import gov.nist.drmf.interpreter.cas.translation.AbstractListTranslator;
import gov.nist.drmf.interpreter.cas.translation.AbstractTranslator;
import gov.nist.drmf.interpreter.common.exceptions.TranslationException;
import gov.nist.drmf.interpreter.common.exceptions.TranslationExceptionReason;
import gov.nist.drmf.interpreter.common.grammar.Brackets;
import gov.nist.drmf.interpreter.common.grammar.ExpressionTags;
import gov.nist.drmf.interpreter.common.grammar.MathTermTags;
import mlp.PomTaggedExpression;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @see ExpressionTags
 * @author Andre Greiner-Petter
 */
public class TaggedExpressionTranslator extends AbstractTranslator {
    private static final Logger LOG = LogManager.getLogger(TaggedExpressionTranslator.class.getName());

    private TranslatedExpression localTranslations;

    public TaggedExpressionTranslator(AbstractTranslator abstractTranslator) {
        super(abstractTranslator);
        this.localTranslations = new TranslatedExpression();
    }

    @Nullable
    @Override
    public TranslatedExpression getTranslatedExpressionObject() {
        return localTranslations;
    }

    @Override
    public TranslatedExpression translate( PomTaggedExpression expression ) throws TranslationException {
        LOG.debug("Triggers empty expression translator process.");
        // switch-case over tags
        String tag = expression.getTag();
        ExpressionTags expTag = ExpressionTags.getTagByKey(tag);

        // no tag shouldn't happen
        if ( expTag == null ) {
            throw buildException("Empty expression tag",
                    TranslationExceptionReason.UNKNOWN_OR_MISSING_ELEMENT);
        }

        // switch over all possible tags
        switch( expTag ) {
            case sub_super_script:
                // in case of sub-super scripts, we first normalize the order, subscript first!
                try {
                    expression = AbstractListTranslator.normalizeSubSuperScripts(expression);
                } catch (IndexOutOfBoundsException iobe) {
                    throw buildException(
                            "SubSuperScript does not have two children.",
                            TranslationExceptionReason.UNKNOWN_OR_MISSING_ELEMENT,
                            iobe
                    );
                }
                // than we fake it as a sequence, since there is no difference to a sequence anymore
                expression.setTag( ExpressionTags.sequence.tag() );
            case sequence: // in that case use the SequenceTranslator
                // this don't write into global_exp!
                // it only delegates the parsing process to the SequenceTranslator
                SequenceTranslator p = new SequenceTranslator(super.getSuperTranslator());
                localTranslations.addTranslatedExpression( p.translate( expression ) );
                break;
            case fraction:
            case binomial:
            case square_root:
            case general_root:
                // all of them has sub-elements.
                parseBasicFunction( expression, expTag );
                break;
            case balanced_expression:
                // balanced expressions are expressions in \left( x \right)
                List<PomTaggedExpression> sub_exps = expression.getComponents();
                TranslatedExpression tr = parseGeneralExpression( sub_exps.remove( 0 ), sub_exps );
                localTranslations.addTranslatedExpression( tr );
                break;
            case numerator:
            case denominator:
            case equation:
            default:
                throw buildException("Reached unknown or not yet supported expression tag: " + tag,
                        TranslationExceptionReason.UNKNOWN_OR_MISSING_ELEMENT);
        }

        return localTranslations;
    }

    private void parseBasicFunction( PomTaggedExpression top_exp, ExpressionTags tag )
            throws TranslationException {
        // extract all components from top expressions
        String[] comps = extractMultipleSubExpressions( top_exp );

        // first of all, translate components into translation
        localTranslations.addTranslatedExpression(
                // try to translate the basic function
                getConfig().getBasicFunctionsTranslator().translate(
                        comps,
                        tag.tag()
                )
        );

        // finally, global_exp needs to be updated
        // it doesn't contains sub expressions because
        // extractMultipleSubExpressions already deleted it.
        TranslatedExpression global = super.getGlobalTranslationList();
        global.addTranslatedExpression(
                localTranslations
        );
    }

    /**
     * A wrapper method for {@link #extractMultipleSubExpressions(List)}.
     *
     * @param top_expression parent expression of underlying sub-expressions.
     * @return true if the parsing process finished successful
     * @see #extractMultipleSubExpressions(List)
     */
    private String[] extractMultipleSubExpressions( PomTaggedExpression top_expression ){
        return extractMultipleSubExpressions(top_expression.getComponents());
    }

    /**
     * A helper method to extract some sub-expressions. Useful for short
     * functions like \frac{a}{b}. The given argument is the parent expression
     * of several children. As an example a fraction expression has two children,
     * the numerator and the denominator.
     *
     * @param sub_expressions parent expression of underlying sub-expressions.
     * @return true if the parsing process finished successful
     */
    private String[] extractMultipleSubExpressions( List<PomTaggedExpression> sub_expressions ) {
        ArrayList<TranslatedExpression> components = new ArrayList<>(sub_expressions.size());
        TranslatedExpression global = super.getGlobalTranslationList();

        while ( !sub_expressions.isEmpty() ){
            PomTaggedExpression exp = sub_expressions.remove(0);
            TranslatedExpression inner_exp = parseGeneralExpression(exp, sub_expressions);
            int num = inner_exp.mergeAll();
            components.add(inner_exp);
            global.removeLastNExps( num ); // remove all previous sub-elements
        }

        String[] output = new String[components.size()];
        for ( int i = 0; i < output.length; i++ )
            output[i] = AbstractListTranslator.stripMultiParentheses(components.get(i).toString());

        return output;
    }

    /**
     * Test on the hard way. If any exception throws, this method
     * catch it and returns false. It only returns true if the
     * first element and the last element are corresponding
     * brackets. Kind of the brackets doesn't matter but they
     * have to correspond to each other.
     * @param first for instance \left(
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