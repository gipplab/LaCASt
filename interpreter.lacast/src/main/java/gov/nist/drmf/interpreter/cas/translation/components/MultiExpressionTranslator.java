package gov.nist.drmf.interpreter.cas.translation.components;

import gov.nist.drmf.interpreter.cas.logging.TranslatedExpression;
import gov.nist.drmf.interpreter.cas.translation.AbstractListTranslator;
import gov.nist.drmf.interpreter.cas.translation.AbstractTranslator;
import gov.nist.drmf.interpreter.common.exceptions.TranslationException;
import gov.nist.drmf.interpreter.common.exceptions.TranslationExceptionReason;
import gov.nist.drmf.interpreter.pom.common.PomTaggedExpressionUtility;
import gov.nist.drmf.interpreter.pom.common.grammar.ExpressionTags;
import mlp.PomTaggedExpression;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

/**
 * This translator handles multiple expressions, such as in equation arrays or multicase expressions.
 * @author Andre Greiner-Petter
 */
public class MultiExpressionTranslator extends AbstractTranslator {
    private static final Logger LOG = LogManager.getLogger(MultiExpressionTranslator.class.getName());

    private final TranslatedExpression localTranslations;

    protected MultiExpressionTranslator(AbstractTranslator abstractTranslator) {
        super(abstractTranslator);
        localTranslations = new TranslatedExpression();
    }

    @Override
    public TranslatedExpression translate(PomTaggedExpression exp) {
        ExpressionTags expTag = ExpressionTags.getTag(exp);

        if ( expTag == null ) {
            throw buildWrongExpressionTagError();
        }

        switch (expTag) {
            case equation:
                parseEquation(exp);
                break;
            case equation_array:
                parseEquationArray(exp);
                break;
            case multi_case:
            case multi_case_single_case:
                LOG.warn("Multi-cases are not yet implemented.");
                break;
            default: throw buildWrongExpressionTagError();
        }

        return localTranslations;
    }

    private void parseEquation(PomTaggedExpression equation) {
        List<PomTaggedExpression> equationElements = equation.getComponents();

        if ( equationElements.isEmpty() ) {
            LOG.debug("Encountered an empty equation expression. Simply ignore the equation");
            return;
        }

        int startIndex = 0;
        PomTaggedExpression first = equationElements.get(0);
        if ( first.isEmpty() ) {
            if ( getGlobalTranslationList().getLength() == 0 ) {
                throw TranslationException.buildException(this,
                        "Left-hand side of equation is empty.",
                        TranslationExceptionReason.INVALID_LATEX_INPUT);
            }
            TranslatedExpression te = getGlobalTranslationList().getElementsBeforeRelation();
            if ( te.getLastExpression().trim().equals( getConfig().getLineDelimiter() ) ) te.removeLastExpression();
            localTranslations.addTranslatedExpression(te);
            getGlobalTranslationList().addTranslatedExpression(te);
            startIndex = 1;
        }

        for ( int i = startIndex; i < equationElements.size(); i++ ) {
            fixSpacingBetweenEquations();
            PomTaggedExpression pte = equationElements.get(i);
            TranslatedExpression te = parseGeneralExpression(pte, null);
            localTranslations.addTranslatedExpression(te);
        }
    }

    private void fixSpacingBetweenEquations() {
        if ( !localTranslations.getTranslatedExpression().isBlank() ) {
            if ( !getGlobalTranslationList().getLastExpression().trim().equals(getConfig().getLineDelimiter()) )
                getGlobalTranslationList().replaceLastExpression(getGlobalTranslationList().getLastExpression() + " ");
            localTranslations.replaceLastExpression(localTranslations.getLastExpression() + " ");
        }
    }

    private void parseEquationArray(PomTaggedExpression equationArray) {
        LOG.debug("Translation equation array");
        List<PomTaggedExpression> arrayComponents = equationArray.getComponents();

        if ( arrayComponents.isEmpty() ) {
            LOG.debug("Encountered an empty equation array. Ignore the equation array.");
            return;
        }

        // the first element of the array is the top equation. All following elements are simply
        // additional translations
        for (PomTaggedExpression arrayComponent : arrayComponents) {
            TranslatedExpression elementTranslation = parseGeneralExpression(arrayComponent, null);
            // remove translation of additional expression from the global list
            LOG.debug("Translated single element in equation array to: " + elementTranslation.getTranslatedExpression());
            updateTranslationListsInArray(arrayComponent, elementTranslation);

            // and add this to the additional translation
            getListOfPartialTranslations().add(elementTranslation);
        }

        LOG.debug("Finished all translation of equation array");
    }

    private void updateTranslationListsInArray(PomTaggedExpression arrayComponent, TranslatedExpression elementTranslation) {
        localTranslations.addTranslatedExpression(elementTranslation);

        if ( arrayComponent.getNextSibling() != null ) {
            // add a line delimiter in front of new elements
            String delimiter = getConfig().getLineDelimiter();
            if ( !delimiter.equals("\n") ) delimiter += " ";
            getGlobalTranslationList().addTranslatedExpression(delimiter);
            localTranslations.addTranslatedExpression(delimiter);
        }
    }

    private TranslationException buildWrongExpressionTagError() {
        return TranslationException.buildException(this,
                "Multi-Expressions must contain an multi-expression tag. You may used the wrong translator.",
                TranslationExceptionReason.IMPLEMENTATION_ERROR);
    }

    @Override
    public TranslatedExpression getTranslatedExpressionObject() {
        return localTranslations;
    }

    @Override
    public List<TranslatedExpression> getListOfPartialTranslations() {
        return super.getListOfPartialTranslations();
    }
}
