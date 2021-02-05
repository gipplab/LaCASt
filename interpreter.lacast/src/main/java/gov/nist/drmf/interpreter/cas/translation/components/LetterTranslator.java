package gov.nist.drmf.interpreter.cas.translation.components;

import gov.nist.drmf.interpreter.cas.logging.TranslatedExpression;
import gov.nist.drmf.interpreter.cas.translation.AbstractListTranslator;
import gov.nist.drmf.interpreter.cas.translation.AbstractTranslator;
import gov.nist.drmf.interpreter.common.constants.Keys;
import gov.nist.drmf.interpreter.common.exceptions.TranslationException;
import gov.nist.drmf.interpreter.common.exceptions.TranslationExceptionReason;
import gov.nist.drmf.interpreter.common.latex.FreeVariables;
import gov.nist.drmf.interpreter.pom.common.grammar.DLMFFeatureValues;
import gov.nist.drmf.interpreter.pom.common.grammar.MathTermTags;
import gov.nist.drmf.interpreter.common.symbols.BasicFunctionsTranslator;
import gov.nist.drmf.interpreter.common.symbols.Constants;
import gov.nist.drmf.interpreter.common.symbols.SymbolTranslator;
import gov.nist.drmf.interpreter.pom.common.FeatureSetUtility;
import gov.nist.drmf.interpreter.pom.common.MathTermUtility;
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

    private static final String GREEK_MSG = "Could be %s.\n But it is also a Greek letter. Be aware, that this program " +
            "translated the letter as a normal Greek letter and not as a constant!\n Use the DLMF-Macro %s to translate %s as a constant.\n";

    private static final String LETTER_MSG = "Could be %s.\n But this system don't know how to translate it as a constant. It was translated as a general letter.\n";

    private static final String EXPONENTIAL_MLP_KEY = "exponential";

    private final TranslatedExpression localTranslations = new TranslatedExpression();
    private final String CAS;
    private final SymbolTranslator sT;

    private PomTaggedExpression exp;
    private List<PomTaggedExpression> following_exp;

    protected LetterTranslator(AbstractTranslator abstractTranslator) {
        super(abstractTranslator);
        this.CAS = getConfig().getTO_LANGUAGE();
        this.sT = getConfig().getSymbolTranslator();
    }

    @Override
    public TranslatedExpression translate(PomTaggedExpression exp, List<PomTaggedExpression> following_exp) {
        this.exp = exp;
        this.following_exp = following_exp;
        MathTerm term = exp.getRoot();

        // If the current element is a constant, it has a constant FeatureSet
        FeatureSet constantSet =
                FeatureSetUtility.getSetByFeatureValue(
                        term,
                        Keys.FEATURE_ROLE,
                        Keys.FEATURE_VALUE_CONSTANT
                );

        TranslatedExpression tmp = broadcastTranslation(exp, term, constantSet);
        this.exp = null;
        this.following_exp = null;
        return tmp;
    }

    @Override
    public TranslatedExpression getTranslatedExpressionObject() {
        return localTranslations;
    }

    private TranslatedExpression broadcastTranslation(PomTaggedExpression pte, MathTerm term, FeatureSet constantSet) {
        MathTermTags tag = MathTermTags.getTagByMathTerm(term);
        TranslatedExpression te;
        switch (tag) {
            case dlmf_macro: case command:
                // a dlmf-macro at this state will be simple translated as a command
                // so do nothing here and switch to command:
                te = parseCommand(term, constantSet);
                break;
            case special_math_letter: case symbol:
                te = parseSymbol(term);
                break;
            case letter:
                translateLetter(term, constantSet);
                return localTranslations;
            case operator:
                logOperator(term);
            case constant: case abbreviation: case alphanumeric:
                // a constant in this state is simply not a command
                // so there is no \ in front of the text.
                // that's why a constant here is the same like a alphanumeric expression
                // ==> do nothing and switch to alphanumeric
                te = parseAlphanumeric(pte, term, constantSet);
                break;
            default:
                throw TranslationException.buildException(this,
                        "Letter translator only translates symbols and alphanumerics: " + term.getTermText(),
                        TranslationExceptionReason.IMPLEMENTATION_ERROR
                );
        }

        return te;
    }

    private void logOperator(MathTerm term) {
        getInfoLogger().addGeneralInfo(
                exp.getRoot().getTermText(),
                "Unable to translate operator (" +
                        FeatureSetUtility.getPossibleMeaning(term) +
                        "). Interpret it as a sequence of multiplications instead."
        );
    }

    private void translateLetter(MathTerm term, FeatureSet constantSet) {
        if (constantSet != null) {
            MathConstantTranslator mct = new MathConstantTranslator(getSuperTranslator());
            localTranslations.addTranslatedExpression(mct.translate(exp));
            // no global adjustment necessary, already performed by constant translator
        } else {
            perform(TranslatedExpression::addTranslatedExpression, term.getTermText());
            mapPerform(TranslatedExpression::getFreeVariables, FreeVariables::addFreeVariable, term.getTermText());
        }
    }

    private TranslatedExpression parseCommand(MathTerm term, FeatureSet constantSet) {
        // a latex-command could be:
        //  1) Greek letter -> translate via GreekLetters.translate
        //  2) constant     -> translate via Constants.translate
        //  3) A DLMF Macro -> this translation cannot handle DLMF-Macros
        //  4) a function   -> Should parsed by FunctionTranslator and not here!
        TranslatedExpression tmp = tryParseGreekOrConstant(term, constantSet);
        if ( tmp != null ) return tmp;

        // translate as symbol
        if ( tryParsingSymbolDirectly(term) ) return localTranslations;

        // no it is a DLMF macro or function
        FeatureSet macro = term.getNamedFeatureSet(Keys.KEY_DLMF_MACRO);
        if (macro != null) {
            throw TranslationException.buildException(
                    this,
                    "MathTermTranslator cannot translate DLMF-Macro: " +
                            term.getTermText(),
                    TranslationExceptionReason.IMPLEMENTATION_ERROR
            );
        }

        // last, fallback to greek letters
        // not all greek letters are in the global lexicon
        // so try again to translate it as a greek letter, just try...
        try {
            GreekLetterTranslator glt = new GreekLetterTranslator(getSuperTranslator());
            return glt.translate(exp);
        } catch (TranslationException te) {
            throw TranslationException.buildExceptionObj(
                    this,
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
        throw TranslationException.buildException(
                this,
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
            perform(TranslatedExpression::addTranslatedExpression, t);

            // the only special math letter which is a free variable is \ell...
            if ( MathTermTags.special_math_letter.equals(MathTermTags.getTagByMathTerm(term)) &&
                    "\\ell".equals(term.getTermText())) {
                mapPerform(TranslatedExpression::getFreeVariables, FreeVariables::addFreeVariable, t);
            }

            return true;
        } else return false;
    }

    private boolean tryFallbackSymbolTranslation(MathTerm term) {
        FeatureSet fset = term.getNamedFeatureSet(Keys.KEY_DLMF_MACRO);
        if (fset != null) {
            String trans = DLMFFeatureValues.CAS_TRANSLATIONS.getFeatureValue(fset, getConfig().getTO_LANGUAGE());
            getInfoLogger().addMacroInfo(
                    term.getTermText(), "was translated to: " + trans);
            perform(TranslatedExpression::addTranslatedExpression, trans);
            return true;
        }
        return false;
    }

    private TranslatedExpression parseAlphanumeric(PomTaggedExpression pte, MathTerm term, FeatureSet constantSet) {
        TranslatedExpression te = tryParseGreekOrConstant(term, constantSet);
        if ( te != null ) return te;

        boolean isUnderscore = false;
        if ( pte.getParent() != null && MathTermUtility.equals( pte.getParent().getRoot(), MathTermTags.underscore ) )
            isUnderscore = true;

        String alpha = term.getTermText();
        String var, output;
        // add multiplication symbol between all letters
        for (int i = 0; i < alpha.length() - 1; i++) {
            var = ""+alpha.charAt(i);
            if ( isUnderscore ) {
                // in case of underscore, it is a list rather than a multiplication...
                output = var + ", ";
            } else output = var + getConfig().getMULTIPLY();
            // add it to local and global

            perform(TranslatedExpression::addTranslatedExpression, output);
            mapPerform(TranslatedExpression::getFreeVariables, FreeVariables::addFreeVariable, var);
        }

        // add the last one, but without space
        output = "" + alpha.charAt(alpha.length() - 1);
        perform(TranslatedExpression::addTranslatedExpression, output);
        mapPerform(TranslatedExpression::getFreeVariables, FreeVariables::addFreeVariable, output);
        return localTranslations;
    }

    private TranslatedExpression tryParseGreekOrConstant(MathTerm term, FeatureSet constantSet) {
        // is it a Greek letter?
        if (MathTermUtility.isGreekLetter(term)) {
            // is this Greek letter also known constant?
            if (constantSet != null) {
                // inform the user about our choices
                constantVsLetter(constantSet, term);
            } // if not, simply translate it as a Greek letter
            GreekLetterTranslator glt = new GreekLetterTranslator(getSuperTranslator());
            TranslatedExpression te = glt.translate(exp);
            // thats meta right? :)
            te.getFreeVariables().addFreeVariable(te.getTranslatedExpression());
            return te;
        }

        // or is it a constant but not a Greek letter?
        if (constantSet != null) {
            // simply try to translate it as a constant
            return forceParseConstantOrExponential(
                    term.getTermText()
            );
        }

        return null;
    }

    private TranslatedExpression forceParseConstantOrExponential(String termText) {
        try {
            PomTaggedExpression next = following_exp.get(0);
            MathTerm possibleCaret = next.getRoot();
            MathTermTags tagPossibleCaret =
                    MathTermTags.getTagByKey(possibleCaret.getTag());
            if (termText.matches("\\\\expe") &&
                    MathTermTags.caret.equals(tagPossibleCaret)) {
                // translate as exp(...) and not exp(1)^{...}
                return parseExponentialFunction();
            } else {
                MathConstantTranslator mct = new MathConstantTranslator(getSuperTranslator());
                return mct.translate(exp);
            }
        } catch (IndexOutOfBoundsException | NullPointerException e) {
            MathConstantTranslator mct = new MathConstantTranslator(getSuperTranslator());
            return mct.translate(exp);
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
                    String.format(LETTER_MSG, DLMFFeatureValues.MEANING.getFeatureValue(constantSet, CAS))
            );
        } else if ( !super.getConfig().translateLettersAsConstantsMode() ) {
            getInfoLogger().addGeneralInfo(
                    term.getTermText(),
                    String.format(GREEK_MSG, DLMFFeatureValues.MEANING.getFeatureValue(constantSet, CAS), dlmf, term.getTermText())
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

    private TranslatedExpression parseExponentialFunction() {
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

        perform(TranslatedExpression::addTranslatedExpression, translation);

        getInfoLogger().addMacroInfo("\\expe", "Recognizes e with power as the exponential function. " +
                "It was translated as a function.");

        return localTranslations;
    }
}
