package gov.nist.drmf.interpreter.cas.translation.components;

import gov.nist.drmf.interpreter.cas.logging.TranslatedExpression;
import gov.nist.drmf.interpreter.cas.translation.AbstractTranslator;
import gov.nist.drmf.interpreter.common.exceptions.TranslationException;
import gov.nist.drmf.interpreter.common.exceptions.TranslationExceptionReason;
import gov.nist.drmf.interpreter.common.symbols.GreekLetters;
import gov.nist.drmf.interpreter.mlp.FeatureSetUtility;
import gov.nist.drmf.interpreter.mlp.MathTermUtility;
import mlp.MathTerm;
import mlp.PomTaggedExpression;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static gov.nist.drmf.interpreter.cas.common.DLMFPatterns.CHAR_BACKSLASH;

/**
 * @author Andre Greiner-Petter
 */
public class GreekLetterTranslator extends AbstractTranslator {
    private static final Logger LOG = LogManager.getLogger(GreekLetterTranslator.class.getName());

    private TranslatedExpression localTranslations = new TranslatedExpression();

    /**
     * Every translator has an abstract super translator object, which should be
     * unique for each translation process. Thus, this
     *
     * @param superTranslator the super translator object
     */
    protected GreekLetterTranslator(AbstractTranslator superTranslator) {
        super(superTranslator);
    }

    @Override
    public TranslatedExpression translate(PomTaggedExpression expression) {
        MathTerm term = expression.getRoot();

        if ( !MathTermUtility.isGreekLetter(term) ) {
            throw TranslationException.buildExceptionObj(
                    this,
                    "GreekLetterTranslator only translate greek letters but was called for: " + term.getTermText(),
                    TranslationExceptionReason.IMPLEMENTATION_ERROR,
                    term
            );
        }

        parseGreekLetter(term.getTermText());
        return localTranslations;
    }

    @Override
    public TranslatedExpression getTranslatedExpressionObject() {
        return localTranslations;
    }

    /**
     * Parsing a given Greek letter.
     *
     * @param GreekLetter the Greek letter
     */
    private void parseGreekLetter(String GreekLetter) throws TranslationException {
        // try to translate
        GreekLetters l = getConfig().getGreekLettersTranslator();
        String translated_letter = l.translate(GreekLetter);

        // if it's null, maybe a \ is missing
        if (translated_letter == null) {
            if (!GreekLetter.startsWith(CHAR_BACKSLASH)) {
                GreekLetter = CHAR_BACKSLASH + GreekLetter;
                translated_letter = l.translate(GreekLetter);
            }
        }

        // still null? inform the user, we cannot do more here
        if (translated_letter == null) {
            throw TranslationException.buildExceptionObj(
                    this,
                    "Cannot translate Greek letter " + GreekLetter,
                    TranslationExceptionReason.MISSING_TRANSLATION_INFORMATION,
                    GreekLetter);
        }

        // otherwise add all
        localTranslations.addTranslatedExpression(translated_letter);
        getGlobalTranslationList().addTranslatedExpression(translated_letter);
    }
}
