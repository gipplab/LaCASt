package gov.nist.drmf.interpreter.cas.translation.components;

import gov.nist.drmf.interpreter.cas.logging.TranslatedExpression;
import gov.nist.drmf.interpreter.cas.translation.AbstractListTranslator;
import gov.nist.drmf.interpreter.cas.translation.AbstractTranslator;
import gov.nist.drmf.interpreter.common.constants.Keys;
import gov.nist.drmf.interpreter.common.exceptions.TranslationException;
import gov.nist.drmf.interpreter.common.exceptions.TranslationExceptionReason;
import gov.nist.drmf.interpreter.common.grammar.DLMFFeatureValues;
import gov.nist.drmf.interpreter.common.grammar.MathTermTags;
import gov.nist.drmf.interpreter.common.symbols.BasicFunctionsTranslator;
import gov.nist.drmf.interpreter.common.symbols.Constants;
import gov.nist.drmf.interpreter.common.symbols.GreekLetters;
import gov.nist.drmf.interpreter.common.symbols.SymbolTranslator;
import gov.nist.drmf.interpreter.mlp.extensions.FeatureSetUtility;
import mlp.FeatureSet;
import mlp.MathTerm;
import mlp.PomTaggedExpression;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

import static gov.nist.drmf.interpreter.cas.common.DLMFPatterns.CHAR_BACKSLASH;

/**
 * @author Andre Greiner-Petter
 */
public class LetterTranslator extends AbstractListTranslator {
    private static final Logger LOG = LogManager.getLogger(LetterTranslator.class.getName());

    private static final String EXPONENTIAL_MLP_KEY = "exponential";

    private final TranslatedExpression localTranslations;
    private final String CAS;

    private final SymbolTranslator sT;

    protected LetterTranslator(AbstractTranslator abstractTranslator) {
        super(abstractTranslator);
        this.localTranslations = new TranslatedExpression();
        this.CAS = getConfig().getTO_LANGUAGE();
        this.sT = getConfig().getSymbolTranslator();
    }

    @Override
    public TranslatedExpression translate(PomTaggedExpression exp, List<PomTaggedExpression> following_exp) {
        MathTerm term = exp.getRoot();
        MathTermTags tag = MathTermTags.getTagByMathTerm(term);

        // If the current element is a constant, it has a constant FeatureSet
        FeatureSet constantSet =
                FeatureSetUtility.getSetByFeatureValue(
                        term,
                        Keys.FEATURE_ROLE,
                        Keys.FEATURE_VALUE_CONSTANT
                );

        TranslatedExpression te;
        switch (tag) {
            case dlmf_macro:
                // a dlmf-macro at this state will be simple translated as a command
                // so do nothing here and switch to command:
            case command:
                // a latex-command could be:
                //  1) Greek letter -> translate via GreekLetters.translate
                //  2) constant     -> translate via Constants.translate
                //  3) A DLMF Macro -> this translation cannot handle DLMF-Macros
                //  4) a function   -> Should parsed by FunctionTranslator and not here!
                te = parseCommand(term, constantSet, following_exp);
                break;
            case special_math_letter:
            case symbol:
                te = parseSymbol(term);
                break;
            case letter:
                // a letter can be one constant, but usually translate it simply
                if (constantSet != null) {
                    te = parseMathematicalConstant(constantSet, term.getTermText());
                } else {
                    localTranslations.addTranslatedExpression(term.getTermText());
                    getGlobalTranslationList().addTranslatedExpression(term.getTermText());
                    te = localTranslations;
                }
                break;
            case constant:
                // a constant in this state is simply not a command
                // so there is no \ in front of the text.
                // that's why a constant here is the same like a alphanumeric expression
                // ==> do nothing and switch to alphanumeric
            case alphanumeric:
                te = parseAlphanumeric(term, constantSet, following_exp);
                break;
            default:
                throw buildException(
                        "Letter translator only translates symbols and alphanumerics: " + term.getTermText(),
                        TranslationExceptionReason.IMPLEMENTATION_ERROR
                );
        }

        return te;
    }

    @Override
    public TranslatedExpression getTranslatedExpressionObject() {
        return localTranslations;
    }

    private TranslatedExpression parseCommand(
            MathTerm term,
            FeatureSet constantSet,
            List<PomTaggedExpression> following_exp
    ) {
        TranslatedExpression tmp = tryParseGreekOrConstant(
                term,
                constantSet,
                following_exp
        );
        if ( tmp != null ) return tmp;

        // translate as symbol
        if ( tryParsingSymbolDirectly(term) ) return localTranslations;

        // no it is a DLMF macro or function
        FeatureSet macro = term.getNamedFeatureSet(Keys.KEY_DLMF_MACRO);
        if (macro != null) {
            throw buildException(
                    "MathTermTranslator cannot translate DLMF-Macro: " +
                            term.getTermText(),
                    TranslationExceptionReason.IMPLEMENTATION_ERROR
            );
        }

        // last, fallback to greek letters
        // not all greek letters are in the global lexicon
        // so try again to translate it as a greek letter, just try...
        try {
            return parseGreekLetter(term.getTermText());
        } catch (TranslationException te) {
            throw buildExceptionObj(
                    "Reached unknown latex-command " + term.getTermText(),
                    TranslationExceptionReason.LATEX_MACRO_ERROR,
                    term.getTermText()
            );
        }
    }

    private TranslatedExpression parseSymbol(MathTerm term) {
        if ( tryParsingSymbolDirectly(term) ) {
            return localTranslations;
        }

        if ( tryFallbackSymbolTranslation(term) )
            return localTranslations;

        // if it didn't work, throw an error
        throw buildException(
                "Unknown symbol reached: " + term.getTermText(),
                TranslationExceptionReason.UNKNOWN_OR_MISSING_ELEMENT);
    }

    private boolean tryParsingSymbolDirectly(MathTerm term) {
        // translate as symbol
        String t = sT.translate(term.getTermText());
        if (t != null) {
            getInfoLogger().addGeneralInfo(
                    term.getTermText(),
                    "was translated to: " + t);
            localTranslations.addTranslatedExpression(t);
            getGlobalTranslationList().addTranslatedExpression(t);
            return true;
        } else return false;
    }

    private boolean tryFallbackSymbolTranslation(MathTerm term) {
        FeatureSet fset = term.getNamedFeatureSet(Keys.KEY_DLMF_MACRO);
        if (fset != null) {
            String trans = DLMFFeatureValues.CAS.getFeatureValue(fset, getConfig().getTO_LANGUAGE());
            if (trans != null) {
                getInfoLogger().addMacroInfo(
                        term.getTermText(), "was translated to: " + trans);
                localTranslations.addTranslatedExpression(trans);
                getGlobalTranslationList().addTranslatedExpression(trans);
                return true;
            }
        }
        return false;
    }

    private TranslatedExpression parseAlphanumeric(
            MathTerm term,
            FeatureSet constantSet,
            List<PomTaggedExpression> following_exp
    ) {
        TranslatedExpression te = tryParseGreekOrConstant(
                term,
                constantSet,
                following_exp
        );
        if ( te != null ) return te;

        String alpha = term.getTermText();
        String output;
        // add multiplication symbol between all letters
        for (int i = 0; i < alpha.length() - 1; i++) {
            output = alpha.charAt(i) + getConfig().getMULTIPLY();
            // add it to local and global
            localTranslations.addTranslatedExpression(output);
            getGlobalTranslationList().addTranslatedExpression(output);
        }

        // add the last one, but without space
        output = "" + alpha.charAt(alpha.length() - 1);
        localTranslations.addTranslatedExpression(output);
        getGlobalTranslationList().addTranslatedExpression(output);
        return localTranslations;
    }

    private TranslatedExpression tryParseGreekOrConstant(
            MathTerm term,
            FeatureSet constantSet,
            List<PomTaggedExpression> following_exp
    ) {
        // is it a Greek letter?
        if (FeatureSetUtility.isGreekLetter(term)) {
            // is this Greek letter also known constant?
            if (constantSet != null) {
                // inform the user about our choices
                constantVsLetter(constantSet, term);
            } // if not, simply translate it as a Greek letter
            return parseGreekLetter(term.getTermText());
        }

        // or is it a constant but not a Greek letter?
        if (constantSet != null) {
            // simply try to translate it as a constant
            return forceParseConstantOrExponential(
                    term.getTermText(),
                    constantSet,
                    following_exp
            );
        }

        return null;
    }

    private TranslatedExpression forceParseConstantOrExponential(
            String termText,
            FeatureSet constantSet,
            List<PomTaggedExpression> following_exp
    ) {
        try {
            PomTaggedExpression next = following_exp.get(0);
            MathTerm possibleCaret = next.getRoot();
            MathTermTags tagPossibleCaret =
                    MathTermTags.getTagByKey(possibleCaret.getTag());
            if (termText.matches("\\\\expe") &&
                    MathTermTags.caret.equals(tagPossibleCaret)) {
                // translate as exp(...) and not exp(1)^{...}
                return parseExponentialFunction(following_exp);
            } else {
                return parseMathematicalConstant(constantSet, termText);
            }
        } catch (IndexOutOfBoundsException | NullPointerException e) {
            return parseMathematicalConstant(constantSet, termText);
        }
    }

    /**
     * Inform the user about the fact of decision we made to translate this
     * constant or Greek letter.
     *
     * This method only will invoke, if the term is a Greek letter and maybe
     * a constant. So it is maybe a constant we know how to translate but don't
     * want to (like pi but the user should use \cpi instead)
     * or it is a completely unknown constant, then we can translate it as a Greek
     * letter anyway (for instance \alpha, could be the 2nd Feigenbaum constant).
     *
     * @param constantSet the constant feature set
     * @param term        the Greek letter term
     */
    private void constantVsLetter(FeatureSet constantSet, MathTerm term) {
        String dlmf = translateToDLMF(term.getTermText());
        if (dlmf == null) {
            getInfoLogger().addGeneralInfo(
                    term.getTermText(),
                    "Could be " + DLMFFeatureValues.meaning.getFeatureValue(constantSet, CAS) + "."
                            + System.lineSeparator() +
                            "But this system don't know how to translate it as a constant. " +
                            "It was translated as a general letter." + System.lineSeparator()
            );
        } else {
            getInfoLogger().addGeneralInfo(
                    term.getTermText(),
                    "Could be " + DLMFFeatureValues.meaning.getFeatureValue(constantSet, CAS) + "."
                            + System.lineSeparator() +
                            "But it is also a Greek letter. " +
                            "Be aware, that this program translated the letter " +
                            "as a normal Greek letter and not as a constant!" + System.lineSeparator() +
                            "Use the DLMF-Macro " +
                            dlmf + " to translate " + term.getTermText() + " as a constant." + System.lineSeparator()
            );
        }
    }

    /**
     * Translate a given LaTeX constant (starts with \ or not) to
     * the DLMF macro if possible.
     *
     * @param constant in LaTeX
     * @return null or the DLMF macro for the given latex constant
     */
    private String translateToDLMF(String constant) {
        Constants c = getConfig().getConstantsTranslator();
        if (!constant.startsWith(CHAR_BACKSLASH)) {
            constant = CHAR_BACKSLASH + constant;
        }
        return c.translate(Keys.KEY_LATEX, Keys.KEY_DLMF, constant);
    }

    /**
     * Parse the mathematical constant. If there are some problems we try
     * some typical other ways to translate them. Like add \ and translate
     * it to a CAS or to DLMF macro and inform the user about all of them.
     *
     * @param set      the feature set with information about the constant
     * @param constant the constant itself
     * @return true if everything was fine
     */
    private TranslatedExpression parseMathematicalConstant(FeatureSet set, String constant) {
        // get the translation first and try to translate it
        Constants c = getConfig().getConstantsTranslator();
        String translated_const = c.translate(constant);

        // if it wasn't translated try some other stuff
        if (translated_const == null) {
            // try from LaTeX to CAS
            translated_const = c.translate(Keys.KEY_LATEX, CAS, constant);
            if (translated_const != null) {
                // if this works, inform the user, that we use this translation now!
                String dlmf = c.translate(Keys.KEY_LATEX, Keys.KEY_DLMF, constant);
                getInfoLogger().addGeneralInfo(
                        constant,
                        "You use a typical letter for a constant [" +
                                DLMFFeatureValues.meaning.getFeatureValue(set, CAS) + "]." + System.lineSeparator() +
                                "We keep it like it is! But you should know that " + CAS +
                                " uses " + translated_const + " for this constant." + System.lineSeparator() +
                                "If you want to translate it as a constant, use the corresponding DLMF macro " +
                                dlmf + System.lineSeparator()
                );
                // and now, use this translation
                translated_const = constant;
            }
        }

        // still null? try to translate it as a Greek letter than if possible
        if (translated_const == null) {
            try {
                String alphabet = set.getFeature(Keys.FEATURE_ALPHABET).first();
                if (alphabet.contains(Keys.FEATURE_VALUE_GREEK)) {
                    getInfoLogger().addGeneralInfo(
                            constant,
                            "Unable to translate " + constant + " [" +
                                    DLMFFeatureValues.meaning.getFeatureValue(set, CAS) +
                                    "]. But since it is a Greek letter we translated it to a Greek letter in "
                                    + CAS + "."
                    );
                    return parseGreekLetter(constant);
                } else {
                    throw buildExceptionObj("Cannot translate mathematical constant " +
                                    constant + " - " + set.getFeature(Keys.FEATURE_MEANINGS),
                            TranslationExceptionReason.MISSING_TRANSLATION_INFORMATION,
                            constant);
                }
            } catch (NullPointerException npe) {/* ignore it */}
        }

        // anyway, finally we translated it...
        localTranslations.addTranslatedExpression(translated_const);
        // add getGlobalTranslationList() as well
        getGlobalTranslationList().addTranslatedExpression(translated_const);

        // if there wasn't a translation at all, return true
        if (translated_const == null) {
            return localTranslations;
        }

        if (translated_const.equals(constant)) {
            return localTranslations;
        }

        // otherwise inform the user about the translation
        getInfoLogger().addGeneralInfo(
                constant,
                DLMFFeatureValues.meaning.getFeatureValue(set, CAS) + " was translated to: " + translated_const
        );

        return localTranslations;
    }

    /**
     * Parsing a given Greek letter.
     *
     * @param GreekLetter the Greek letter
     * @return true if it was parsed
     */
    private TranslatedExpression parseGreekLetter(String GreekLetter) throws TranslationException {
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
            throw buildExceptionObj("Cannot translate Greek letter " + GreekLetter,
                    TranslationExceptionReason.MISSING_TRANSLATION_INFORMATION,
                    GreekLetter);
        }

        // otherwise add all
        localTranslations.addTranslatedExpression(translated_letter);
        getGlobalTranslationList().addTranslatedExpression(translated_letter);
        return localTranslations;
    }

    private TranslatedExpression parseExponentialFunction(List<PomTaggedExpression> following_exp) {
        PomTaggedExpression caretPomExp = following_exp.remove(0);
        PomTaggedExpression caretChild = caretPomExp.getComponents().get(0);

        TranslatedExpression power = parseGeneralExpression(caretChild, null);

        BasicFunctionsTranslator ft = getConfig().getBasicFunctionsTranslator();
        String translation = ft.translate(
                new String[] {
                        stripMultiParentheses(power.toString())
                },
                EXPONENTIAL_MLP_KEY
        );

        getGlobalTranslationList().removeLastNExps(power.clear());

        localTranslations.addTranslatedExpression(translation);
        getGlobalTranslationList().addTranslatedExpression(translation);

        getInfoLogger().addMacroInfo("\\expe", "Recognizes e with power as the exponential function. " +
                "It was translated as a function.");

        return localTranslations;
    }
}
