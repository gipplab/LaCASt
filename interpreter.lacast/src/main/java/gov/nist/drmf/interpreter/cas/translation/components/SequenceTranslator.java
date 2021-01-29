package gov.nist.drmf.interpreter.cas.translation.components;

import gov.nist.drmf.interpreter.cas.logging.TranslatedExpression;
import gov.nist.drmf.interpreter.cas.translation.AbstractListTranslator;
import gov.nist.drmf.interpreter.cas.translation.AbstractTranslator;
import gov.nist.drmf.interpreter.cas.translation.components.util.SequenceHelper;
import gov.nist.drmf.interpreter.common.constants.Keys;
import gov.nist.drmf.interpreter.common.exceptions.TranslationException;
import gov.nist.drmf.interpreter.common.exceptions.TranslationExceptionReason;
import gov.nist.drmf.interpreter.common.symbols.BasicFunctionsTranslator;
import gov.nist.drmf.interpreter.pom.common.FakeMLPGenerator;
import gov.nist.drmf.interpreter.pom.common.FeatureSetUtility;
import gov.nist.drmf.interpreter.pom.common.MathTermUtility;
import gov.nist.drmf.interpreter.pom.common.PomTaggedExpressionUtility;
import gov.nist.drmf.interpreter.pom.common.grammar.Brackets;
import gov.nist.drmf.interpreter.pom.common.grammar.ExpressionTags;
import gov.nist.drmf.interpreter.pom.common.grammar.MathTermTags;
import mlp.MathTerm;
import mlp.PomTaggedExpression;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static gov.nist.drmf.interpreter.cas.common.DLMFPatterns.*;

/**
 * There are two possible types of sequences in this code.
 * 1) It is an empty expression by itself, tagged with sequence.
 * In that case, a sequence is simply a row of elements where
 * each element is a stand alone element.
 * Be aware, a sequence can be one element by it self.
 * There will be no parenthesis added to this kind of sequence.
 * 2) It is a row of expressions wrapped by parenthesis.
 * In that case, it is not really a sequence object (from MLP)
 * but a sequence in parenthesis. It produces only one
 * TranslatedExpression.
 *
 * @author Andre Greiner-Petter
 * @see ExpressionTags
 * @see Brackets
 * @see AbstractListTranslator
 * @see AbstractTranslator
 */
public class SequenceTranslator extends AbstractListTranslator {

    private static final Logger LOG = LogManager.getLogger(SequenceTranslator.class.getName());

    // the open bracket if needed
    private final Brackets openBracket;

    private final TranslatedExpression localTranslations;

    private final String MULTIPLY;
    private final Pattern MULTIPLY_PATTERN;

    private boolean hasPassedPunctuation;

    private final SequenceHelper sequenceHelper;

    /**
     * Uses only for a general sequence expression.
     * If the tag is sequence we don't need to check any parenthesis.
     */
    public SequenceTranslator(AbstractTranslator superTranslator) {
        this(superTranslator, null);
    }

    /**
     * Use this if the sequence is wrapped by parenthesis.
     * In that we don't know the length of the sequence. The sequence
     * ends when we reach the next corresponding bracket, matches to
     * the open bracket.
     *
     * @param openBracket the following sequence is wrapped by brackets
     *                    the given bracket is the first open bracket of the following
     *                    sequence
     */
    public SequenceTranslator(AbstractTranslator superTranslator, Brackets openBracket) {
        super(superTranslator);
        localTranslations = new TranslatedExpression();
        MULTIPLY = getConfig().getMULTIPLY();
        MULTIPLY_PATTERN = Pattern.compile("(.*)"+Pattern.quote(MULTIPLY)+"\\s*");
        this.openBracket = openBracket;
        this.hasPassedPunctuation = false;
        this.sequenceHelper = new SequenceHelper(this, this::isSetMode);
    }

    @Override
    public TranslatedExpression getTranslatedExpressionObject() {
        return localTranslations;
    }

    /**
     * This method works slightly different than other translators.
     * A sequence might be a real {@link ExpressionTags#sequence} or just a sequence of elements wrapped in
     * brackets. In the second scenario, you have been initiated this class with
     * {@link SequenceTranslator#SequenceTranslator(AbstractTranslator, Brackets)} rather than with
     * {@link SequenceTranslator#SequenceTranslator(AbstractTranslator)}. In this case, the first element
     * {@param exp} is the bracket itself and should be ignored. Hence you provide null and the following list
     * <code>translate(null, followingExpressions);</code>.
     *
     * In the case of a real sequence node tagged with {@link ExpressionTags#sequence} the given parameter
     * {@param exp} is the sequence and should not have following tokens (it may have following tokens but they are
     * out of scope for this sequence and should be null). Hence you call
     * <code>translate(exp, null);</code>.
     *
     * If you do not follow this rule, the method tries to find the right translation for you.
     * If the given {@param exp} is a sequence node, it will ignore the following tokens.
     * If not and there was a bracket open, {@param exp} will be ignored and the following tokens are translated.
     * If neither is the case, a {@link TranslationException} is thrown with an
     * {@link TranslationExceptionReason#IMPLEMENTATION_ERROR}.
     *
     * In the totally strange case that both {@param exp} and {@param following} are null, the previously
     * translated expression is returned, which might be empty, of course.
     *
     * @param exp a sequence node, tagged with {@link ExpressionTags#sequence}. In this case the second
     *            argument {@param following} must be null because every following expressions are out of scope for this
     *            sequence.
     * @param following the following expressions. This is either null (if you provided {@param exp} or you
     *                  instantiated this class with an open bracket. In this case the {@param exp} must be null!
     * @return the translated expression
     */
    @Override
    public TranslatedExpression translate(PomTaggedExpression exp, List<PomTaggedExpression> following) {
        if ( exp != null ) {
            if (PomTaggedExpressionUtility.isSequence(exp)) return translate(exp);
            else if ( openBracket != null ) return translate(following);
            else throw TranslationException.buildExceptionObj(
                    this, "The sequence translator requires a sequence element but was provided with " + exp,
                    TranslationExceptionReason.IMPLEMENTATION_ERROR, exp
            );
        } else if ( following != null ) return translate(following);
        else return localTranslations;
    }

    /**
     * This method parses a PomTaggedExpression of type sequence and
     * only these expressions! There will be no parenthesis added
     *
     * @param expression with "sequence" tag!
     * @return true if the parsing process finish correctly
     * otherwise false
     * @see ExpressionTags#sequence
     */
    @Override
    public TranslatedExpression translate(PomTaggedExpression expression) {
        sequenceHelper.throwIfIsNotSequence(expression);

        // get all sub elements
        List<PomTaggedExpression> expList = new LinkedList<>(expression.getComponents());

        // run through each element
        PomTaggedExpression prev = null;
        while (!expList.isEmpty()) {
            PomTaggedExpression exp = expList.remove(0);
            Brackets bracket = Brackets.getBracket(exp);

            if ( PomTaggedExpressionUtility.isLongSpace(exp) && exp.getPreviousSibling() != null ) {
                handleConstraints(expList);
                break;
            } else if ( upcomingConstraint(exp, expList) ) {
                expList.remove(0);
                handleConstraints(expList);
                break;
            } else if ( isCaseSplit(exp, expList) ) {
                handleCaseSplit(expList);
                break;
            } else if ( sequenceHelper.handleAsBracket(bracket, expList) ) {
                translateAsBracket(prev, bracket, expList);
            } else {
                translateNext(exp, expList, true);
            }
            prev = exp;
        }

        // finally return value
        return localTranslations;
    }

    private boolean isCaseSplit( PomTaggedExpression exp, List<PomTaggedExpression> expList ) {
        String text = exp.getRoot().getTermText();
        if ( text.matches("[,;.]") ) {
            // if this comma is part of a sequence but the sequence is not the root of the parse tree
            // in this case the comma is nested and hence not a splitter
            if ( exp.getParent() != null && exp.getParent().getParent() != null ) return false;
            if ( !expList.isEmpty() && MathTermUtility.equals(expList.get(0).getRoot(), MathTermTags.newline) )
                expList.remove(0);
            return true;
        } else return MathTermUtility.equals(exp.getRoot(), MathTermTags.newline);
    }

    private void translateAsBracket(PomTaggedExpression prev, Brackets bracket, List<PomTaggedExpression> expList) {
        if ( prev != null ) {
            List<PomTaggedExpression> tmp = new LinkedList<>();
            tmp.add( FakeMLPGenerator.generateBracketExpression( Brackets.left_parenthesis ) );
            String lastPart = checkMultiplyAddition(prev, tmp, localTranslations.getLastExpression().trim());
            perform(TranslatedExpression::replaceLastExpression, lastPart);
        }

        SequenceTranslator st = new SequenceTranslator(super.getSuperTranslator(), bracket);
        localTranslations.addTranslatedExpression(st.translate(expList));
    }

    /**
     * Use this function ONLY when you created an object of this class
     * with a given bracket {@link SequenceTranslator#SequenceTranslator(AbstractTranslator, Brackets)}.
     * <p>
     * This method goes through a given list of expressions until it
     * reached the closed bracket that matches to the given open bracket
     * in the constructor.
     * <p>
     * Than it will return true and organize merges all parts in the
     * global list of translated expressions.
     *
     * @param followingExp the descendants of a previous expression
     *                      with an open bracket
     * @return true when the translation finished without an error.
     */
    private TranslatedExpression translate(List<PomTaggedExpression> followingExp) {
        if (openBracket == null) {
            throw TranslationException.buildException(this, "Wrong translation method used. " +
                            "You have to specify an open bracket to translate it like a sequence.",
                    TranslationExceptionReason.IMPLEMENTATION_ERROR
            );
        }

        // no new list instantiation of followingExp is required. This is now a private function
        // and hence the given list is already not the original list of components.
        // hence, we can safely manipulate the list as we wish

        // in this method we do not need to check for constraint starts (like above)
        // because this method is only used when an open bracket remains open. Hence there is a non closed
        // sequence still in progress. Hence long spaces do not indicate constraints here in this method, only above

        // iterate through all elements
        while (!followingExp.isEmpty()) {
            // take the next expression
            PomTaggedExpression exp = followingExp.remove(0);
            hasPassedPunctuation |= PomTaggedExpressionUtility.isListSetSeparationIndicator(exp);

            // otherwise investigate the term
            MathTerm term = exp.getRoot();

            // open or closed brackets
            Brackets bracket = Brackets.ifIsBracketTransform(term, openBracket);
            if (bracket != null) {
                TranslatedExpression t = handleBracket(exp, followingExp, bracket);
                if ( t != null ) {
                    return t;
                }
                else continue;
            }

            translateNext(exp, followingExp, false);
        }

        // this should not happen. It means the algorithm reached the end but a bracket is left open.
        throw TranslationException.buildException(this,
                "Reached the end of sequence but a bracket is left open: " +
                        openBracket.symbol,
                TranslationExceptionReason.WRONG_PARENTHESIS);
    }

    private void handleRelationSplit(PomTaggedExpression exp, List<PomTaggedExpression> expList) {
        if ( !sequenceHelper.isRelationSymbol(exp, expList) ) return;

        // alright, we finally encountered a relation symbol in exp... time to split it... left hand side is part
        // of the local translation already
        addRelationLatestSymbol();
        perform( TranslatedExpression::appendRelationalRelation, exp.getRoot().getTermText() );
//        mapPerform( TranslatedExpression::getRelationalComponents, RelationalComponents::addRelation, exp.getRoot().getTermText() );
    }

    private void addRelationLatestSymbol() {
        TranslatedExpression components = getGlobalTranslationList().getElementsAfterRelation();
        String part = components.getTranslatedExpression();
        perform( TranslatedExpression::appendRelationalComponent, part.trim() );
//        mapPerform( TranslatedExpression::getRelationalComponents, RelationalComponents::addComponent, part.trim() );
    }

    private void translateNext(
            PomTaggedExpression exp,
            List<PomTaggedExpression> expList,
            boolean skipMultiplyOnLongSpace
    ) {
        handleRelationSplit(exp, expList);
        TranslatedExpression innerTranslation = parseGeneralExpression(exp, expList);

        // only take the last object and check if it is
        // necessary to add a space character behind
        String part = innerTranslation.getLastExpression();
        TranslatedExpression global = super.getGlobalTranslationList();

        // the last expression was merged, if part is empty!
        boolean lastMerged = false;
        if (part == null) {
            part = global.getLastExpression();
            lastMerged = true;
        }

        if ( !skipMultiplyOnLongSpace || expList.isEmpty() || !PomTaggedExpressionUtility.isLongSpace(expList.get(0)) )
            part = checkMultiplyAddition(exp, expList, part);

        // finally add all elements to the inner list
        innerTranslation.replaceLastExpression(part);
        if (lastMerged) {
            localTranslations.replaceLastExpression(innerTranslation.toString());
            localTranslations.getFreeVariables().replaceFreeVariables(innerTranslation.getFreeVariables());
        } else {
            localTranslations.addTranslatedExpression(innerTranslation);
        }
    }

    private void handleConstraints(List<PomTaggedExpression> expList) {
        LOG.debug("Interpret following elements as constraints");
        TranslatedExpression copyOfLocal = new TranslatedExpression(localTranslations);
        localTranslations.clear();

        TranslatedExpression copyOfGlobal = new TranslatedExpression(super.getGlobalTranslationList());
        super.getGlobalTranslationList().clear();

        super.getGlobalTranslationList().lockRelationalComponents();
        while ( !expList.isEmpty() ) translateNext( expList.remove(0), expList, false );
        super.getGlobalTranslationList().releaseRelationalComponents();

        copyOfLocal.addConstraint( localTranslations.getTranslatedExpression() );
        copyOfLocal.getFreeVariables().addFreeVariables( localTranslations.getFreeVariables() );

        copyOfGlobal.addConstraint( localTranslations.getTranslatedExpression() );
        copyOfGlobal.getFreeVariables().addFreeVariables( localTranslations.getFreeVariables() );

        localTranslations.clear();
        localTranslations.addTranslatedExpression(copyOfLocal);

        super.getGlobalTranslationList().clear();
        super.getGlobalTranslationList().addTranslatedExpression(copyOfGlobal);
    }

    private void handleCaseSplit(List<PomTaggedExpression> expList) {
        LOG.debug("Encountered a case split. Move translations to subexpression list");
        TranslatedExpression copyOfGlobal = new TranslatedExpression(super.getGlobalTranslationList());
        copyOfGlobal.replaceLastExpression( copyOfGlobal.getLastExpression().trim() );
        super.addPartialTranslation(copyOfGlobal);

        localTranslations.clear();
        String last = super.getGlobalTranslationList().getLastExpression();
        last = last.trim() + getConfig().getLineDelimiter() + " ";
        super.getGlobalTranslationList().replaceLastExpression( last );
        while ( !expList.isEmpty() ) translateNext( expList.remove(0), expList, false );
        super.addPartialTranslation(localTranslations);
    }

    private boolean bracketMatchOrSetMode(Brackets bracket) {
        return openBracket.counterpart.equals(bracket.symbol) || super.isSetMode();
    }

    /**
     * If this term is a bracket there are three possible options
     *           1) another open bracket
     *               -> reached a new sub sequence
     *           2) a closed bracket which is the counterpart of the first open bracket
     *               -> this sequence ends here
     *           3) another closed bracket
     *               -> there is a bracket error in the sequence
     * @param exp current expression
     * @param followingExp following expressions
     * @param bracket the current bracket (not null)
     * @return null, translated expression or throws an exception
     */
    private TranslatedExpression handleBracket(
            PomTaggedExpression exp,
            List<PomTaggedExpression> followingExp,
            Brackets bracket
    ) {
        // another open bracket -> reached a new sub sequence
        // bracket cannot be null, because we checked the tag of the term before
        if (bracket.opened) {
            // create a new SequenceTranslator (2nd kind)
            SequenceTranslator sp = new SequenceTranslator(super.getSuperTranslator(), bracket);
            // translate the following expressions
            localTranslations.addTranslatedExpression(sp.translate(followingExp));
            return null;
        } else if ( bracketMatchOrSetMode(bracket) ) {
            return handleClosingBracket(exp, followingExp, bracket);
        } else { // otherwise there was an error in the bracket arrangements
            throw TranslationException.buildException(this, "Bracket-Error: open bracket "
                            + openBracket.symbol
                            + " reached " + bracket.symbol,
                    TranslationExceptionReason.WRONG_PARENTHESIS);
        }
    }

    private TranslatedExpression handleClosingSet(Brackets closingBracket) {
        localTranslations.addTranslatedExpression(closingBracket.getAppropriateString());
        return localTranslations;
    }

    private TranslatedExpression handleClosingBracket(
            PomTaggedExpression exp,
            List<PomTaggedExpression> followingExp,
            Brackets bracket
    ) {
        if ( super.isSetMode() ) return handleClosingSet(bracket);

        // this sequence ends her
        // first of all, merge all elements together
        int num = localTranslations.mergeAll();

        // now, always wrap brackets around this sequence
        // if the brackets are |.| for absolute value, translate it as a function
        String seq;
        if (openBracket.equals(Brackets.left_latex_abs_val) ||
                openBracket.equals(Brackets.abs_val_open)) {
            String argTranslation = localTranslations.removeLastExpression();
            Matcher m = MULTIPLY_PATTERN.matcher(argTranslation);
            if ( m.matches() ) argTranslation = m.group(1);

            BasicFunctionsTranslator bft = getConfig().getBasicFunctionsTranslator();
            seq = bft.translate(
                    new String[] {
                            stripMultiParentheses(argTranslation)
                    },
                    Keys.KEY_ABSOLUTE_VALUE
            );
        } else { // otherwise, parenthesis must match each other, so close as it opened
            bracket = openBracket;
            if ( normalizeBracket() ) {
                bracket = Brackets.left_parenthesis;
                exp = new PomTaggedExpression(new MathTerm(")", MathTermTags.left_brace.tag()));
            }
            seq = bracket.getAppropriateString() +
                    localTranslations.removeLastExpression().trim() +
                    bracket.getCounterPart().getAppropriateString();
        }

        seq = checkMultiplyAddition(exp, followingExp, seq);

        // wrap parenthesis around sequence, this is one component of the sequence now
        localTranslations.addTranslatedExpression(seq); // replaced it

        // same for global_exp. But first delete all elements of this sequence
        TranslatedExpression global = super.getGlobalTranslationList();
        global.removeLastNExps(num);
        global.addTranslatedExpression(seq);
        return localTranslations;
    }

    private boolean normalizeBracket() {
        if ( !openBracket.isNormalParenthesis() && !hasPassedPunctuation && !super.isSetMode() ) {
            LOG.warn("Normalizing brackets to normal parenthesis: ( )");
            return true;
        } else return false;
    }

    private String checkMultiplyAddition(PomTaggedExpression exp, List<PomTaggedExpression> exp_list, String part) {
        if ( part == null ) return "";
        TranslatedExpression global = getGlobalTranslationList();

        PomTaggedExpression specTreatExp = treatFirstExpression(part, exp);

        if (checkParenthesisFirst(exp_list)) {
            Matcher m = MULTIPLY_PATTERN.matcher(part);
            if ( m.matches() ) return m.group(1);
            else return part;
        }

        if (addMultiplySpecTreatment(exp, specTreatExp, exp_list) /*&& !part.matches(".*\\*\\s*")*/) {
            part += MULTIPLY;
            // the global list already got each element before,
            // so simply replace the last if necessary
            String tmp = global.getLastExpression();
            global.replaceLastExpression(tmp + MULTIPLY);
        } else if (sequenceHelper.addSpace(specTreatExp, exp_list)) {
            part = addSimpleSpace(part);

            // the global list already got each element before,
            // so simply replace the last if necessary
            String tmp = addSimpleSpace(global.getLastExpression());
            global.replaceLastExpression(tmp);
        }
        return part;
    }

    private String addSimpleSpace(String part) {
        if ( part == null || part.isBlank() ) return "";
        if ( !part.matches(".*\\s+$") )
            part += SPACE;
        return part;
    }

    private PomTaggedExpression treatFirstExpression(
            String part, PomTaggedExpression exp
    ) {
        if ( part == null ) {}
        else if (part.matches(STRING_END_TREAT_AS_CLOSED_PARANTHESIS)) {
            MathTerm tmp = FakeMLPGenerator.generateClosedParenthesesMathTerm();
            exp = new PomTaggedExpression(tmp);
        } else if (MULTIPLY_PATTERN.matcher(part).matches()) {
            exp = new PomTaggedExpression(new MathTerm(getConfig().getMULTIPLY(), MathTermTags.multiply.tag()));
        }
        return exp;
    }

    private boolean checkParenthesisFirst(List<PomTaggedExpression> expList) {
        return openBracket != null &&
                (openBracket.equals(Brackets.abs_val_close) || openBracket.equals(Brackets.abs_val_open)) &&
                expList != null &&
                !expList.isEmpty() &&
                expList.get(0).getRoot().getTermText().matches(Brackets.ABSOLUTE_VAL_TERM_TEXT_PATTERN);
    }
}
