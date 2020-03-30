package gov.nist.drmf.interpreter.cas.translation.components;

import gov.nist.drmf.interpreter.cas.logging.TranslatedExpression;
import gov.nist.drmf.interpreter.cas.translation.AbstractListTranslator;
import gov.nist.drmf.interpreter.cas.translation.AbstractTranslator;
import gov.nist.drmf.interpreter.common.constants.GlobalConstants;
import gov.nist.drmf.interpreter.common.constants.Keys;
import gov.nist.drmf.interpreter.common.exceptions.TranslationException;
import gov.nist.drmf.interpreter.common.exceptions.TranslationExceptionReason;
import gov.nist.drmf.interpreter.common.grammar.Brackets;
import gov.nist.drmf.interpreter.common.grammar.DLMFFeatureValues;
import gov.nist.drmf.interpreter.common.grammar.MathTermTags;
import gov.nist.drmf.interpreter.common.symbols.BasicFunctionsTranslator;
import gov.nist.drmf.interpreter.common.symbols.Constants;
import gov.nist.drmf.interpreter.common.symbols.GreekLetters;
import gov.nist.drmf.interpreter.common.symbols.SymbolTranslator;
import gov.nist.drmf.interpreter.mlp.extensions.FakeMLPGenerator;
import gov.nist.drmf.interpreter.mlp.extensions.FeatureSetUtility;
import mlp.FeatureSet;
import mlp.MathTerm;
import mlp.PomTaggedExpression;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.LinkedList;
import java.util.List;

import static gov.nist.drmf.interpreter.cas.common.DLMFPatterns.ABSOLUTE_VAL_TERM_TEXT_PATTERN;
import static gov.nist.drmf.interpreter.cas.common.DLMFPatterns.CHAR_BACKSLASH;

/**
 * The math term translation parses only math terms.
 * It is a inner translation and switches through all different
 * kinds of math terms. All registered math terms can be
 * found in {@link MathTermTags}.
 *
 * @author Andre Greiner-Petter
 * @see AbstractTranslator
 * @see Constants
 * @see GreekLetters
 * @see MathTermTags
 */
public class MathTermTranslator extends AbstractListTranslator {
    private static final Logger LOG = LogManager.getLogger(MathTermTranslator.class.getName());
    private static final String EXPONENTIAL_MLP_KEY = "exponential";
    private final TranslatedExpression localTranslations;
    private final String CAS;

    private final SymbolTranslator sT;

    public MathTermTranslator(AbstractTranslator superTranslator) {
        super(superTranslator);
        this.localTranslations = new TranslatedExpression();
        this.CAS = getConfig().getTO_LANGUAGE();
        this.sT = getConfig().getSymbolTranslator();
    }

    @Override
    public TranslatedExpression getTranslatedExpressionObject() {
        return localTranslations;
    }

    @Override
    public TranslatedExpression translate(PomTaggedExpression exp) {
        return translate(exp, new LinkedList<>());
    }

    /**
     * This translation only parses MathTerms. Only use this
     * when your expression has a non-empty term and
     * cannot translate by any other specialized translation!
     *
     * @param exp           has a not empty term!
     * @param following_exp the following elements (might be empty)
     * @return true when everything is fine and there was no error
     */
    @Override
    public TranslatedExpression translate(PomTaggedExpression exp, List<PomTaggedExpression> following_exp) {
        // it has to be checked before that this exp has a not empty term
        // get the MathTermTags object
        MathTerm term = exp.getRoot();
        String termTag = term.getTag();
        MathTermTags tag = MathTermTags.getTagByKey(termTag);

        // no tag shouldn't happen
        if (tag == null) {
            throw buildException("Empty math term tag",
                    TranslationExceptionReason.UNKNOWN_OR_MISSING_ELEMENT);
        }

        // check if the element should be translated by the term translator
        handleInvalidTags(tag, term);

        TranslatedExpression te = translateIndependentElement(tag);
        if ( te == null )
            te = translateDependentElement(tag, term, exp, following_exp);
        if ( te == null )
            te = translateDirectly(tag, term, following_exp);
        if ( te != null ) return te;

        // translate others must be last, it translates directly to
        // localTranslations object
        translateOthers(tag, term);
        return localTranslations;
    }

    /**
     * Checks if the element is invalid for the {@link MathTermTranslator}.
     * Throws an {@link TranslationException} if the element is invalid. Otherwise nothing happens.
     * @param tag the {@link MathTermTags} of the current element
     * @param term the {@link MathTerm} of the current element
     * @throws TranslationException throws if the element is not valid for this translator
     */
    private void handleInvalidTags(MathTermTags tag, MathTerm term) throws TranslationException {
        switch (tag) {
            case function:
                throw buildException(
                        "MathTermTranslator cannot translate functions. Use the FunctionTranslator instead: "
                                + term.getTermText(),
                        TranslationExceptionReason.IMPLEMENTATION_ERROR
                );
            case left_delimiter:
            case right_delimiter:
            case left_parenthesis:
            case left_bracket:
            case left_brace:
            case right_parenthesis:
            case right_bracket:
            case right_brace:
                throw buildException(
                        "MathTermTranslator don't expected brackets but found " + term.getTermText(),
                        TranslationExceptionReason.IMPLEMENTATION_ERROR
                );
            case macro:
                throw buildException(
                        "There shouldn't be a macro in MathTermTranslator: " + term.getTermText(),
                        TranslationExceptionReason.IMPLEMENTATION_ERROR
                );
            case abbreviation:
                throw buildExceptionObj(
                        "This program cannot translate abbreviations like " + term.getTermText(),
                        TranslationExceptionReason.MISSING_TRANSLATION_INFORMATION,
                        term.getTermText()
                );
        }
    }

    /**
     * Translates elements that can be performed independently
     * of the current element.
     * @param tag the {@link MathTermTags} of the current element
     * @return the translated expression or null
     */
    private TranslatedExpression translateIndependentElement(
            MathTermTags tag
    ) {
        TranslatedExpression te = null;
        if (tag == MathTermTags.multiply) {
            localTranslations.addTranslatedExpression(getConfig().getMULTIPLY());
            getGlobalTranslationList().addTranslatedExpression(getConfig().getMULTIPLY());
            te = localTranslations;
        }
        return te;
    }

    /**
     * Translates expressions that are dependent of the current {@link PomTaggedExpression}
     * element.
     * @param tag the {@link MathTermTags} of the current element
     * @param exp the current {@link PomTaggedExpression} element
     * @param following_exp the following elements of {@param exp}
     * @return the translated expression or null
     */
    private TranslatedExpression translateDependentElement(
            MathTermTags tag,
            MathTerm term,
            PomTaggedExpression exp,
            List<PomTaggedExpression> following_exp
    ) {
        TranslatedExpression te = null;
        switch (tag) {
            case fence:
                te = parseFences(term, following_exp);
                break;
            case modulo:
            case operation:
                OperationTranslator opParser = new OperationTranslator(getSuperTranslator());
                localTranslations.addTranslatedExpression(
                        opParser.translate(exp, following_exp)
                );
                te = localTranslations;
                break;
            case factorial:
                te = parseFactorial(tag, following_exp);
                break;
            case caret:
            case underscore:
                SubSuperScriptTranslator sssT = new SubSuperScriptTranslator(getSuperTranslator());
                te = sssT.translate(exp, following_exp);
                break;
            case dlmf_macro:
            case command:
            case alphanumeric:
            case special_math_letter:
            case symbol:
            case constant:
            case letter:
                LetterTranslator letterT = new LetterTranslator(getSuperTranslator());
                te = letterT.translate(exp, following_exp);
                break;
        }
        return te;
    }

    /**
     * Translates elements that can be directly translated, i.e., the
     * translation is an identity mapping (i->i).
     * @param tag the {@link MathTermTags} of the current element
     * @param term the {@link MathTerm} of the current element
     * @param following_exp the following elements
     * @return the translated expression or null
     */
    private TranslatedExpression translateDirectly(
            MathTermTags tag,
            MathTerm term,
            List<PomTaggedExpression> following_exp
    ) {
        TranslatedExpression te = null;
        switch (tag) {
            case divide:
                // must be followed by letter -> digit -> ... -> translate directly
                te = handleDivide(term, following_exp);
                if ( te != null ) break;
            case digit:
            case numeric:
            case comma:
            case minus:
            case plus:
            case equals:
            case less_than:
            case greater_than: // all above should translated directly
                localTranslations.addTranslatedExpression(term.getTermText());
                getGlobalTranslationList().addTranslatedExpression(term.getTermText());
                te = localTranslations;
                break;
        }
        return te;
    }

    /**
     * If nothing else was triggered before, there are some special
     * other cases we have to handle. This method writes the translation
     * directly to {@link #localTranslations}.
     * @param tag the {@link MathTermTags} of the current element
     * @param term the {@link MathTerm} of the current element
     */
    private void translateOthers(
            MathTermTags tag,
            MathTerm term
    ) {
        switch (tag) {
            case at:
                // simply ignore it...
                break;
            case ordinary:
            case ellipsis:
                String symbol;
                if (tag.equals(MathTermTags.ordinary)) {
                    symbol = sT.translate(term.getTermText());
                } else {
                    symbol = sT.translateFromMLPKey(tag.tag());
                }
                localTranslations.addTranslatedExpression(symbol);
                getGlobalTranslationList().addTranslatedExpression(symbol);
                break;
            case spaces:
            case non_allowed:
                LOG.debug("Skip controlled space, such as \\!");
                break;
            case relation:
                handleRelation(term);
                break;
            default:
                throw buildExceptionObj("Unknown MathTerm Tag: "
                                + term.getTag() + " for " + term.getTermText(),
                        TranslationExceptionReason.UNKNOWN_OR_MISSING_ELEMENT,
                        term.getTermText());
        }
    }

    private TranslatedExpression parseFences(MathTerm term, List<PomTaggedExpression> following_exp) {
        Brackets start = SequenceTranslator.ifIsBracketTransform(term, null);
        SequenceTranslator sq = new SequenceTranslator(getSuperTranslator(), start);
        this.localTranslations.addTranslatedExpression(sq.translate(following_exp));
        return localTranslations;
    }



    private TranslatedExpression handleDivide(MathTerm term, List<PomTaggedExpression> following_exp) {
        if ( following_exp == null || following_exp.isEmpty() )
            return null;

        // if divide is followed by balanced expression, we may need to wrap parenthesis around the next expr
        PomTaggedExpression next = following_exp.get(0);
        if ( isTaggedExpression(next) ) { // is empty, so we need to wrap parenthesis around it
            TranslatedExpression te = parseGeneralExpression(following_exp.remove(0), following_exp);
            getGlobalTranslationList().removeLastNExps(te.getLength());
            String innerTranslation = te.toString();
            if ( !innerTranslation.matches("^\\s*\\(.*\\)\\s*$") ) {
                innerTranslation = "(" + innerTranslation + ")";
            }
            localTranslations.addTranslatedExpression(term.getTermText());
            localTranslations.addTranslatedExpression(innerTranslation);
            getGlobalTranslationList().addTranslatedExpression(localTranslations);
            return localTranslations;
        } else return null;
    }


    private TranslatedExpression parseFactorial(MathTermTags tag, List<PomTaggedExpression> following_exp) {
        String last = getGlobalTranslationList().removeLastExpression();
        last = stripMultiParentheses(last);
        BasicFunctionsTranslator translator = getConfig().getBasicFunctionsTranslator();

        String prefix = "";
        try {
            PomTaggedExpression next = following_exp.get(0);
            MathTermTags nextTag = MathTermTags.getTagByKey(next.getRoot().getTag());
            if (nextTag != null && nextTag.equals(tag)) {
                following_exp.remove(0);
                prefix = "double ";
            }
        } catch (Exception e) {
            prefix = "";
        }
        String translation = translator.translate(new String[] {last}, prefix + tag.tag());

        // caution! do not add elements to localTranslations if your replaced global before
        getGlobalTranslationList().addTranslatedExpression(translation);
        return localTranslations;
    }

    private void handleRelation(MathTerm term) {
        String termText = term.getTermText();
        if ( termText.matches(ABSOLUTE_VAL_TERM_TEXT_PATTERN) )
            return;

        String translation = termText.equals("\\to") ?
                translateToCommand() :
                sT.translate(termText);

        if (translation == null) {
            throw buildExceptionObj(
                    "Unknown relation. Cannot translate: " + termText,
                    TranslationExceptionReason.MISSING_TRANSLATION_INFORMATION,
                    termText
            );
        }

        localTranslations.addTranslatedExpression(translation);
        getGlobalTranslationList().addTranslatedExpression(translation);
    }

    private String translateToCommand() {
        switch ( CAS ) {
            case Keys.KEY_MATHEMATICA:
                return "->";
            case Keys.KEY_MAPLE:
                return "=";
            default:
                throw buildException(
                        "Translation for '\\to' is not implemented for CAS " + CAS,
                        TranslationExceptionReason.MISSING_TRANSLATION_INFORMATION
                );
        }
    }
}
