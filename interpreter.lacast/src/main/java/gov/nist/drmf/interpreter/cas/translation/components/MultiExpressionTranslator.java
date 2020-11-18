package gov.nist.drmf.interpreter.cas.translation.components;

import gov.nist.drmf.interpreter.cas.logging.TranslatedExpression;
import gov.nist.drmf.interpreter.cas.translation.AbstractListTranslator;
import gov.nist.drmf.interpreter.cas.translation.AbstractTranslator;
import gov.nist.drmf.interpreter.common.exceptions.TranslationException;
import gov.nist.drmf.interpreter.common.exceptions.TranslationExceptionReason;
import gov.nist.drmf.interpreter.pom.common.MathTermUtility;
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

    /**
     * It sounds counter intuitive, but {@link ExpressionTags#equation} still have all
     * including relation symbols. Hence, for translating an equation, we simply concatenate
     * the translations of each component. We do not need to an equation symbol between the elements.
     * @param equation simple list of expressions. Might be even contain empty elements
     */
    private void parseEquation(PomTaggedExpression equation) {
        List<PomTaggedExpression> equationElements = equation.getComponents();

        if ( equationElements.isEmpty() ) {
            LOG.debug("Encountered an empty equation expression. Simply ignore the equation");
            return;
        }

        for (PomTaggedExpression equationElement : equationElements) {
            fixSpacingBetweenEquations();
            TranslatedExpression te = parseGeneralExpression(equationElement, null);
            localTranslations.addTranslatedExpression(te);
        }
    }

    private void fixSpacingBetweenEquations() {
        if ( !localTranslations.getTranslatedExpression().isBlank() ) {
            if ( getGlobalTranslationList().getLength() > 0 &&
                    !getGlobalTranslationList().getLastExpression().trim().equals(getConfig().getLineDelimiter()) )
                getGlobalTranslationList().replaceLastExpression(getGlobalTranslationList().getLastExpression() + " ");
            localTranslations.replaceLastExpression(localTranslations.getLastExpression() + " ");
        }
    }

    /**
     * Equation arrays working the same as {@link ExpressionTags#equation} in {@link #parseEquation(PomTaggedExpression)}.
     * We simply do not know if they represent multiple equations or just a long chain of a single equation.
     * Hence, we follow the same approach as for equations. We simply concatenate the element translations with one
     * exception. If the previous expression contained an equation and the next one contains an equation symbol as well,
     * we split them with an CAS-specific EOL symbol and both are added to {@link AbstractTranslator#addPartialTranslation(TranslatedExpression)} ()}.
     * @param equationArray array of equations
     * @throws TranslationException if the translation of the inner elements failed or one line ends with an relation symbol
     * and the next line started with a relation symbol.
     */
    private void parseEquationArray(PomTaggedExpression equationArray) throws TranslationException {
        LOG.debug("Translation equation array");
        List<PomTaggedExpression> arrayComponents = equationArray.getComponents();

        if ( arrayComponents.isEmpty() ) {
            LOG.debug("Encountered an empty equation array. Ignore the equation array.");
            return;
        }

        // the first element of the array is the top equation. All following elements are simply
        // additional translations
        boolean endedOnRelationSymbol = false;
        boolean previousElementContainedRelationSymbol = false;
        boolean previouslyAddedLineDelimiter = false;
        for (PomTaggedExpression arrayComponent : arrayComponents) {
            boolean nextBeginsWithRelation = PomTaggedExpressionUtility.beginsWithRelation(arrayComponent);
            // the next element starts on relation symbol, so we need to take previous elements to build a valid equation
            if ( nextBeginsWithRelation ) {
                if ( endedOnRelationSymbol ) throw TranslationException.buildException(
                        this, "Previous equation array element ended on relation symbol and next line begins on " +
                                "relation symbol. Invalid mathematical logic.",
                        TranslationExceptionReason.INVALID_LATEX_INPUT
                );
            }

            TranslatedExpression elementTranslation = parseGeneralExpression(arrayComponent, null);
            getGlobalTranslationList().removeLastNExps(elementTranslation.getLength());

            // ok we split the expressions here
            if ( split(previousElementContainedRelationSymbol, elementTranslation, nextBeginsWithRelation, endedOnRelationSymbol) ) {
                if ( !previouslyAddedLineDelimiter ) addPartialTranslation(localTranslations);
                addLineDelimiter(elementTranslation); // adds a line delimiter to translation lists
                localTranslations.addTranslatedExpression(elementTranslation);
                addPartialTranslation(elementTranslation);
                previouslyAddedLineDelimiter = true;
            } else {
                fixSpacingBetweenEquations();
                localTranslations.addTranslatedExpression( elementTranslation );
            }

            // remove translation of additional expression from the global list
            LOG.debug("Translated single element in equation array to: " + elementTranslation.getTranslatedExpression());
            endedOnRelationSymbol = elementTranslation.endedOnRelationSymbol();
            previousElementContainedRelationSymbol = elementTranslation.containsRelationSymbol();
        }

        LOG.debug("Finished all translation of equation array");
        getGlobalTranslationList().addTranslatedExpression(localTranslations);
    }

    private boolean split(boolean previousElementContainedRelationSymbol,
                          TranslatedExpression elementTranslation,
                          boolean nextBeginsWithRelation,
                          boolean endedOnRelationSymbol ) {
        return previousElementContainedRelationSymbol &&
                elementTranslation.containsRelationSymbol() &&
                !nextBeginsWithRelation && !endedOnRelationSymbol;
    }

    private void addLineDelimiter( TranslatedExpression elementTranslation ) {
        String delimiter = getConfig().getLineDelimiter();
        if ( !delimiter.equals("\n") ) delimiter += " ";

        localTranslations.addTranslatedExpression(delimiter);
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
