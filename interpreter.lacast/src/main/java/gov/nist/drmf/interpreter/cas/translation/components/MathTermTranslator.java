package gov.nist.drmf.interpreter.cas.translation.components;

import gov.nist.drmf.interpreter.cas.logging.TranslatedExpression;
import gov.nist.drmf.interpreter.cas.translation.AbstractListTranslator;
import gov.nist.drmf.interpreter.cas.translation.AbstractTranslator;
import gov.nist.drmf.interpreter.common.constants.Keys;
import gov.nist.drmf.interpreter.common.exceptions.TranslationException;
import gov.nist.drmf.interpreter.common.exceptions.TranslationExceptionReason;
import gov.nist.drmf.interpreter.pom.common.MathTermUtility;
import gov.nist.drmf.interpreter.pom.common.grammar.Brackets;
import gov.nist.drmf.interpreter.pom.common.grammar.MathTermTags;
import gov.nist.drmf.interpreter.common.symbols.BasicFunctionsTranslator;
import gov.nist.drmf.interpreter.common.symbols.Constants;
import gov.nist.drmf.interpreter.common.symbols.GreekLetters;
import gov.nist.drmf.interpreter.common.symbols.SymbolTranslator;
import mlp.MathTerm;
import mlp.PomTaggedExpression;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * The math term translation parses only math terms.
 * It is a inner translation and switches through all different
 * kinds of math terms. All registered math terms can be
 * found in {@link MathTermTags}.
 *
 * TODO once we update to next LTS Java 17 we should update these ugly lengthy switch cases
 *
 * @author Andre Greiner-Petter
 * @see AbstractTranslator
 * @see Constants
 * @see GreekLetters
 * @see MathTermTags
 */
public class MathTermTranslator extends AbstractListTranslator {
    private static final Logger LOG = LogManager.getLogger(MathTermTranslator.class.getName());
    private final TranslatedExpression localTranslations;

    private final SymbolTranslator sT;
    private final BasicFunctionsTranslator bfT;

    public MathTermTranslator(AbstractTranslator superTranslator) {
        super(superTranslator);
        this.localTranslations = new TranslatedExpression();
        this.sT = getConfig().getSymbolTranslator();
        this.bfT = getConfig().getBasicFunctionsTranslator();
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
        MathTermTags tag = MathTermTags.getTagByMathTerm(term);

        // no tag shouldn't happen
        if (tag == null) {
            throw TranslationException.buildException(
                    this,
                    "Empty math term tag",
                    TranslationExceptionReason.UNKNOWN_OR_MISSING_ELEMENT);
        }

        // check if the element should be translated by the term translator
        handleInvalidTags(tag, term);

        TranslatedExpression te = translateIndependentElement(tag, term);
        if ( te == null )
            te = translateDependentElement(tag, term, exp, following_exp);
        if ( te == null )
            te = translateDirectly(tag, term, following_exp);
        if ( te != null ) {
            tagLastElement(term, te);
            return te;
        }

        // translate others must be last, it translates directly to
        // localTranslations object
        translateOthers(tag, term);
        tagLastElement(term, localTranslations);
        return localTranslations;
    }

    private void tagLastElement(MathTerm term, TranslatedExpression te) {
        if (MathTermUtility.isRelationSymbol(term)) {
            te.tagLastElementAsRelation();
            getGlobalTranslationList().tagLastElementAsRelation();
        }
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
                throwError(
                        "MathTermTranslator cannot translate functions. Use the FunctionTranslator instead: "
                                + term.getTermText(), TranslationExceptionReason.IMPLEMENTATION_ERROR );
            case prime:
            case primes:
                throw TranslationException.buildException(
                        this, "Primes can only be translated behind semantic macros " +
                                "(differentiation primes) but not in other places.",
                        TranslationExceptionReason.INVALID_LATEX_INPUT
                );
            case macro:
                throwError("There shouldn't be a macro in MathTermTranslator: " + term.getTermText(), TranslationExceptionReason.IMPLEMENTATION_ERROR);
            case abbreviation:
                if ( term.getTermText().matches(".*\\.\\s*$") )
                    throw TranslationException.buildExceptionObj(
                            this,
                            "This program cannot translate abbreviations like " + term.getTermText(),
                            TranslationExceptionReason.MISSING_TRANSLATION_INFORMATION,
                            term.getTermText()
                    );
                else
                    getInfoLogger().addGeneralInfo(
                            term.getTermText(),
                            "Found a potential abbreviation. This program cannot translate abbreviations. Hence it was " +
                                    "interpreted as a sequence of variable multiplications, e.g. 'etc' -> 'e*t*c'."
                    );
        }
    }

    private void throwError(String error, TranslationExceptionReason reason) {
        throw TranslationException.buildException(
                this,
                error,
                reason
        );
    }

    /**
     * Translates elements that can be performed independently
     * of the current element.
     * @param tag the {@link MathTermTags} of the current element
     * @param term the math term
     * @return the translated expression or null
     */
    private TranslatedExpression translateIndependentElement(
            MathTermTags tag, MathTerm term
    ) {
        String translation = "";
        switch (tag) {
            case left_delimiter:
            case right_delimiter:
            case left_parenthesis:
            case left_bracket:
            case left_brace:
            case right_parenthesis:
            case right_bracket:
            case right_brace:
                if ( !super.isSetMode() ) throwError("Found unexpected bracket: " + term.getTermText() + ". We are not in set-mode so parenthesis logic must be valid!",
                        TranslationExceptionReason.WRONG_PARENTHESIS);
                Brackets b = Brackets.getBracket( term );
                LOG.info("Encounter a unlogical bracket but since we are in set mode, we translate it anyway.");
                translation = b.getAppropriateString();
                break;
            case multiply:
                translation = getConfig().getMULTIPLY();
                break;
            default: return null;
        }
        perform(TranslatedExpression::addTranslatedExpression, translation);
        return localTranslations;
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
        TranslatedExpression te;
        switch (tag) {
            case factorial:
                te = parseFactorial(tag, following_exp);
                break;
            case fence:
                te = parseFences(term, following_exp);
                break;
            case relation:
                te = parseRelation(term, following_exp);
                break;
            default: // all other cases, try operation, sub-super-scripts and letters
                te = broadcastOperationScriptLetter(tag, exp, following_exp);
        }
        return te;
    }

    private TranslatedExpression broadcastOperationScriptLetter(
            MathTermTags tag,
            PomTaggedExpression exp,
            List<PomTaggedExpression> following_exp
    ){
        TranslatedExpression te = null;
        switch (tag) {
            case modulo: case operation:
                te = translateOperation(exp, following_exp);
                break;
            case caret: case underscore:
                SubSuperScriptTranslator sssT = new SubSuperScriptTranslator(getSuperTranslator());
                te = sssT.translate(exp, following_exp);
                break;
            case operator:
                if ( exp.getRoot().getTermText().startsWith("\\") ) {
                    throw TranslationException.buildExceptionObj(
                            this, "No translation available for the operator " + exp.getRoot().getTermText(),
                            TranslationExceptionReason.MISSING_TRANSLATION_INFORMATION,
                            exp.getRoot().getTermText()
                    );
                }
            case dlmf_macro: case command:
            case alphanumeric: case abbreviation:
            case special_math_letter: case symbol: case constant: case letter:
                LetterTranslator letterT = new LetterTranslator(getSuperTranslator());
                te = letterT.translate(exp, following_exp);
                break;
        }
        return te;
    }

    private TranslatedExpression translateOperation(PomTaggedExpression exp, List<PomTaggedExpression> following_exp) {
        OperationTranslator opParser = new OperationTranslator(getSuperTranslator());
        localTranslations.addTranslatedExpression(
                opParser.translate(exp, following_exp)
        );
        return localTranslations;
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
            case point: case comma: case semicolon:
                if ( following_exp.isEmpty() ) {
                    LOG.debug("Expression sequence ends on punctuation. Ignoring this symbol.");
                    return new TranslatedExpression(); // empty expression
                }
            case digit:
            case numeric:
            case minus:
            case plus:
            case equals:
            case less_than:
            case greater_than: // all above should translated directly
                String translation = translateSymbol(term);
                perform(TranslatedExpression::addTranslatedExpression, translation);

                te = localTranslations;
                break;
        }
        return te;
    }

    private String translateSymbol(MathTerm term) {
        String translation = sT.translate(term.getTermText());
        if ( translation != null && !translation.isBlank() )
            return translation;
        return term.getTermText();
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
                translateEllipsis(tag, term);
                break;
            case spaces:
            case non_allowed:
                LOG.debug("Skip controlled space, such as \\!");
                break;
            default:
                throw TranslationException.buildExceptionObj(
                        this,
                        "Unknown MathTerm Tag: "
                                + term.getTag() + " for " + term.getTermText(),
                        TranslationExceptionReason.UNKNOWN_OR_MISSING_ELEMENT,
                        term.getTermText());
        }
    }

    private void translateEllipsis(MathTermTags tag, MathTerm term) {
        String symbol;
        if (tag.equals(MathTermTags.ordinary)) {
            symbol = sT.translate(term.getTermText());
        } else {
            symbol = sT.translateFromMLPKey(tag.tag());
        }
        perform(TranslatedExpression::addTranslatedExpression, symbol);
    }

    private TranslatedExpression parseFences(MathTerm term, List<PomTaggedExpression> following_exp) {
        Brackets start = Brackets.ifIsBracketTransform(term, null);
        SequenceTranslator sq = new SequenceTranslator(getSuperTranslator(), start);
        this.localTranslations.addTranslatedExpression(sq.translate(null, following_exp));
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

    private TranslatedExpression parseRelation(MathTerm term, List<PomTaggedExpression> followingExps) {
        String termText = term.getTermText();

        if ( termText.equals("\\in") ) {
            return handleSets(term, followingExps);
        }

        if ( termText.matches(Brackets.ABSOLUTE_VAL_TERM_TEXT_PATTERN) )
            return null;

        String translation = termText.equals("\\to") ?
                translateToCommand() :
                sT.translate(termText);

        if (translation == null) {
            throw TranslationException.buildExceptionObj(
                    this,
                    "Unknown relation. Cannot translate: " + termText,
                    TranslationExceptionReason.MISSING_TRANSLATION_INFORMATION,
                    termText
            );
        }

        perform(TranslatedExpression::addTranslatedExpression, translation);
        return localTranslations;
    }

    private TranslatedExpression handleSets(MathTerm term, List<PomTaggedExpression> followingExps) {
        // first, what ever comes next, we expecting set-logic (i.e. mismatched parenthesis are allowed)
        super.activateSetMode();
        checkFollowingElementValidity(followingExps);

        PomTaggedExpression next = followingExps.get(0);
        Brackets bracket = Brackets.getBracket(next);

        // in case of non-open bracket, nothing special, just translate term and return it.
        // Following expressions are handled by someone else
        if ( returnBracketSet(bracket, term, next.getRoot()) ) return localTranslations;

        String firstArgument = getGlobalTranslationList().removeLastExpression();

        // time to translate the sequence
        followingExps.remove(0);
        SequenceTranslator sequenceTranslator = new SequenceTranslator(this, bracket);
        TranslatedExpression translatedExpression = sequenceTranslator.translate(null, followingExps);

        // remove last translated expressions from global list
        getGlobalTranslationList().removeLastNExps(translatedExpression.getLength());

        String intervalTranslation = buildIntervalTranslation(translatedExpression, bracket, firstArgument);
        perform(TranslatedExpression::addTranslatedExpression, intervalTranslation);
        return localTranslations;
    }

    private String buildIntervalTranslation(TranslatedExpression translatedExpression, Brackets bracket, String firstArgument) {
        String closingSymbol = translatedExpression.removeLastExpression();
        String[] arguments = translatedExpression.splitOn(",");
        checkArgumentValidity(arguments);

        // ok we know its an open bracket and it is either ( or [
        boolean leftOpen = bracket.symbol.endsWith("(");
        boolean rightOpen = closingSymbol.matches("\\s*\\)\\s*");

        String mlpKey = Keys.MLP_KEY_SET_PREFIX;
        mlpKey += Keys.MLP_KEY_SET_LEFT_PREFIX  + (leftOpen  ? "open" : "closed") + "-";
        mlpKey += Keys.MLP_KEY_SET_RIGHT_PREFIX + (rightOpen ? "open" : "closed");

        return bfT.translate(
                new String[]{
                        firstArgument.trim(),
                        arguments[0].trim(), // delete the open parenthesis
                        arguments[1].trim() // delete the closed parenthesis
                },
                mlpKey
        );
    }

    private boolean returnBracketSet(Brackets bracket, MathTerm term, MathTerm nextTerm) {
        String relationTranslation = sT.translate(term.getTermText());
        if (bracket == null || !Brackets.isOpenedSetBracket(bracket)) {
            // nothing special, just translate term and return it. Following expressions are handled by someone else
            perform(TranslatedExpression::addTranslatedExpression, relationTranslation);
            return true;
        } else if ( !bracket.opened ) {
            throw TranslationException.buildExceptionObj(
                    this, "Mismatch parenthesis. Closing parenthesis after '\\in' is not allowed.",
                    TranslationExceptionReason.INVALID_LATEX_INPUT, nextTerm.getTermText()
            );
        } else if ( getGlobalTranslationList().getLength() < 1 ) {
            throw TranslationException.buildExceptionObj(
                    this, "No element specified in front of '\\in'.",
                    TranslationExceptionReason.INVALID_LATEX_INPUT, term.getTermText()
            );
        }
        return false;
    }

    private void checkFollowingElementValidity(List<PomTaggedExpression> followingExps) {
        if ( followingExps == null || followingExps.isEmpty() ) {
            throw TranslationException.buildException(
                    this, "Expected set information after '\\in' but found no following expressions.",
                    TranslationExceptionReason.INVALID_LATEX_INPUT
            );
        }
    }

    private void checkArgumentValidity(String[] arguments) {
        if ( arguments.length != 2 )
            throw TranslationException.buildExceptionObj(
                    this, "Expecting to arguments, separated by a comma, for an interval but got " + arguments.length,
                    TranslationExceptionReason.INVALID_LATEX_INPUT, Arrays.toString(arguments)
            );
    }

    private String translateToCommand() {
        String CAS = getConfig().getTO_LANGUAGE();
        switch ( CAS ) {
            case Keys.KEY_MATHEMATICA:
                return "->";
            case Keys.KEY_MAPLE:
                return "=";
            default:
                throw TranslationException.buildException(
                        this,
                        "Translation for '\\to' is not implemented for CAS " + CAS,
                        TranslationExceptionReason.MISSING_TRANSLATION_INFORMATION
                );
        }
    }
}
