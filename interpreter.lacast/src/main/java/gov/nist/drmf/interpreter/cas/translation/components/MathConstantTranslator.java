package gov.nist.drmf.interpreter.cas.translation.components;

import gov.nist.drmf.interpreter.cas.logging.TranslatedExpression;
import gov.nist.drmf.interpreter.cas.translation.AbstractTranslator;
import gov.nist.drmf.interpreter.common.constants.Keys;
import gov.nist.drmf.interpreter.common.exceptions.TranslationException;
import gov.nist.drmf.interpreter.common.exceptions.TranslationExceptionReason;
import gov.nist.drmf.interpreter.pom.common.grammar.DLMFFeatureValues;
import gov.nist.drmf.interpreter.common.symbols.Constants;
import gov.nist.drmf.interpreter.pom.common.FeatureSetUtility;
import mlp.FeatureSet;
import mlp.MathTerm;
import mlp.PomTaggedExpression;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author Andre Greiner-Petter
 */
public class MathConstantTranslator extends AbstractTranslator {
    private static final Logger LOG = LogManager.getLogger(MathConstantTranslator.class.getName());

    private TranslatedExpression localTranslations = new TranslatedExpression();
    private final String CAS;

    /**
     * Every translator has an abstract super translator object, which should be
     * unique for each translation process. Thus, this
     *
     * @param superTranslator the super translator object
     */
    protected MathConstantTranslator(AbstractTranslator superTranslator) {
        super(superTranslator);
        this.CAS = getConfig().getTO_LANGUAGE();
    }

    @Override
    public TranslatedExpression translate(PomTaggedExpression expression) {
        MathTerm term = expression.getRoot();

        FeatureSet constantSet =
                FeatureSetUtility.getSetByFeatureValue(
                        term,
                        Keys.FEATURE_ROLE,
                        Keys.FEATURE_VALUE_CONSTANT
                );

        if ( constantSet == null ) {
            throw TranslationException.buildExceptionObj(
                    this,
                    "MathConstantTranslator require the existence of the role 'constant'",
                    TranslationExceptionReason.IMPLEMENTATION_ERROR,
                    term
            );
        }

        parseMathematicalConstant(expression, constantSet, term.getTermText());
        return localTranslations;
    }

    @Override
    public TranslatedExpression getTranslatedExpressionObject() {
        return localTranslations;
    }

    /**
     * Parse the mathematical constant. If there are some problems we try
     * some typical other ways to translate them. Like add \ and translate
     * it to a CAS or to DLMF macro and inform the user about all of them.
     *
     * @param exp      current expression
     * @param set      the feature set with information about the constant
     * @param constant the constant itself
     */
    private void parseMathematicalConstant(PomTaggedExpression exp, FeatureSet set, String constant) {
        // get the translation first and try to translate it
        Constants c = getConfig().getConstantsTranslator();
        String translation = c.translate(constant);

        // if it wasn't translated try some other stuff
        if (translation == null) {
            translation = alternativeConstantTranslation(c, set, constant);
        }

        if ( translation != null ) {
            // anyway, finally we translated it...
            localTranslations.addTranslatedExpression(translation);
            // add getGlobalTranslationList() as well
            getGlobalTranslationList().addTranslatedExpression(translation);
            getInfoLogger().addGeneralInfo(
                    constant,
                    DLMFFeatureValues.MEANING.getFeatureValue(set, CAS) + " was translated to: " + translation
            );
            return;
        }

        // still null? try to translate it as a Greek letter than if possible
        if ( !tryGreekLetterTranslation(exp, set, constant) ) {
            throw TranslationException.buildExceptionObj(
                    this, "Unable to translate constant " +
                            constant + " - " + set.getFeature(Keys.FEATURE_MEANINGS),
                    TranslationExceptionReason.MISSING_TRANSLATION_INFORMATION,
                    constant);
        }
    }

    private String alternativeConstantTranslation(Constants c, FeatureSet set, String constant) {
        // try from LaTeX to CAS (instead of DLMF to CAS)
        LOG.debug("Cannot translate math constant by MLP key. Try latex instead.");
        String translation = c.translate(Keys.KEY_LATEX, CAS, constant);
        if (translation != null) {
            // if this works, inform the user, that we use this translation now!
            String dlmf = c.translate(Keys.KEY_LATEX, Keys.KEY_DLMF, constant);
            getInfoLogger().addGeneralInfo(
                    translation,
                    "You use a typical letter for a constant [" +
                            DLMFFeatureValues.MEANING.getFeatureValue(set, CAS) + "]." + System.lineSeparator() +
                            "We keep it like it is! But you should know that " + CAS +
                            " uses " + translation + " for this constant." + System.lineSeparator() +
                            "If you want to translate it as a constant, use the corresponding DLMF macro " +
                            dlmf + System.lineSeparator()
            );
            // and now, use this translation
            translation = constant;
            getInfoLogger().getFreeVariables().addFreeVariable(translation);
        }
        return translation;
    }

    private boolean tryGreekLetterTranslation(PomTaggedExpression exp, FeatureSet set, String constant) {
        LOG.debug("Still unable to translate math constant as a constant. If its greek letter, fallback to greek translation");
        try {
            String alphabet = set.getFeature(Keys.FEATURE_ALPHABET).first();
            if (alphabet.contains(Keys.FEATURE_VALUE_GREEK)) {
                LOG.debug("Indeed a greek letter, inform user and translate as greek letter.");
                getInfoLogger().addGeneralInfo(
                        constant,
                        "Unable to translate " + constant + " [" + DLMFFeatureValues.MEANING.getFeatureValue(set, CAS) +
                                "]. But since it is a Greek letter we translated it to a Greek letter in "
                                + CAS + "."
                );
                GreekLetterTranslator glt = new GreekLetterTranslator(getSuperTranslator());
                localTranslations = glt.translate(exp);
                getInfoLogger().getFreeVariables().addFreeVariable(localTranslations.getTranslatedExpression());
                return true;
            } else {
                throw TranslationException.buildExceptionObj(this,
                        "Cannot translate mathematical constant " + constant + " - " +
                                set.getFeature(Keys.FEATURE_MEANINGS),
                        TranslationExceptionReason.MISSING_TRANSLATION_INFORMATION, constant);
            }
        } catch (NullPointerException npe) {
            return false;
        }
    }
}
