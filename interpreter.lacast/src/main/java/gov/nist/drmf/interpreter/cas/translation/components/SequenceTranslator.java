package gov.nist.drmf.interpreter.cas.translation.components;

import gov.nist.drmf.interpreter.cas.logging.TranslatedExpression;
import gov.nist.drmf.interpreter.cas.translation.AbstractListTranslator;
import gov.nist.drmf.interpreter.cas.translation.AbstractTranslator;
import gov.nist.drmf.interpreter.common.constants.Keys;
import gov.nist.drmf.interpreter.common.exceptions.TranslationException;
import gov.nist.drmf.interpreter.common.exceptions.TranslationExceptionReason;
import gov.nist.drmf.interpreter.common.grammar.Brackets;
import gov.nist.drmf.interpreter.common.grammar.ExpressionTags;
import gov.nist.drmf.interpreter.common.grammar.MathTermTags;
import gov.nist.drmf.interpreter.common.symbols.BasicFunctionsTranslator;
import gov.nist.drmf.interpreter.mlp.FakeMLPGenerator;
import gov.nist.drmf.interpreter.mlp.FeatureSetUtility;
import mlp.MathTerm;
import mlp.PomTaggedExpression;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
    private Brackets openBracket;

    private boolean setMode = false;

    private final TranslatedExpression localTranslations;

    private final String MULTIPLY;
    private final Pattern MULTIPLY_PATTERN;

    /**
     * Uses only for a general sequence expression.
     * If the tag is sequence we don't need to check any parenthesis.
     */
    public SequenceTranslator(AbstractTranslator superTranslator) {
        super(superTranslator);
        localTranslations = new TranslatedExpression();
        MULTIPLY = getConfig().getMULTIPLY();
        MULTIPLY_PATTERN = Pattern.compile("(.*)"+Pattern.quote(MULTIPLY)+"\\s*");;
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
        this(superTranslator);
        this.openBracket = openBracket;
    }

    public SequenceTranslator(AbstractTranslator superTranslator, Brackets openBracket, boolean setMode) {
        this(superTranslator, openBracket);
        this.setMode = setMode;
    }

    @Override
    public TranslatedExpression getTranslatedExpressionObject() {
        return localTranslations;
    }

    @Override
    public TranslatedExpression translate(PomTaggedExpression exp, List<PomTaggedExpression> following) {
        if (exp == null) {
            return translate(following);
        } else if (following == null) {
            return translate(exp);
        } else {
            return localTranslations;
        }
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
        if (!ExpressionTags.sequence.tag().matches(expression.getTag())) {
            throw TranslationException.buildException(this,
                    "You used the wrong translation method. " +
                            "The given expression is not a sequence! " +
                            expression.getTag(),
                    TranslationExceptionReason.IMPLEMENTATION_ERROR
            );
        }

        // get all sub elements
        List<PomTaggedExpression> expList = expression.getComponents();

        // run through each element
        while (!expList.isEmpty()) {
            PomTaggedExpression exp = expList.remove(0);
            translateRest(exp, expList);
        }

        // finally return value
        return localTranslations;
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
    public TranslatedExpression translate(List<PomTaggedExpression> followingExp) {
        if (openBracket == null) {
            throw TranslationException.buildException(this, "Wrong translation method used. " +
                            "You have to specify an open bracket to translate it like a sequence.",
                    TranslationExceptionReason.IMPLEMENTATION_ERROR
            );
        }

        // iterate through all elements
        while (!followingExp.isEmpty()) {
            // take the next expression
            PomTaggedExpression exp = followingExp.remove(0);

            // otherwise investigate the term
            MathTerm term = exp.getRoot();

            // open or closed brackets
            Brackets bracket = Brackets.ifIsBracketTransform(term, openBracket);
            if (bracket != null) {
                TranslatedExpression t = handleBracket(exp, followingExp, bracket);
                if ( t != null ) return t;
                else continue;
            }

            translateRest(exp, followingExp);
        }

        // this should not happen. It means the algorithm reached the end but a bracket is left open.
        throw TranslationException.buildException(this,
                "Reached the end of sequence but a bracket is left open: " +
                        openBracket.symbol,
                TranslationExceptionReason.WRONG_PARENTHESIS);
    }

    private void translateRest(
            PomTaggedExpression exp,
            List<PomTaggedExpression> expList
    ) {
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

        part = checkMultiplyAddition(exp, expList, part);

        // finally add all elements to the inner list
        innerTranslation.replaceLastExpression(part);
        if (lastMerged) {
            localTranslations.replaceLastExpression(innerTranslation.toString());
        } else {
            localTranslations.addTranslatedExpression(innerTranslation);
        }
    }

    private boolean bracketMatchOrSetMode(Brackets bracket) {
        return openBracket.counterpart.equals(bracket.symbol) || setMode;
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
            SequenceTranslator sp = new SequenceTranslator(super.getSuperTranslator(), bracket, setMode);
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

    private TranslatedExpression handleClosingBracket(
            PomTaggedExpression exp,
            List<PomTaggedExpression> followingExp,
            Brackets bracket
    ) {
        // this sequence ends her
        // first of all, merge all elements together
        int num = localTranslations.mergeAll();

        // now, always wrap brackets around this sequence
        // if the brackets are |.| for absolute value, translate it as a function
        String seq;
        if (openBracket.equals(Brackets.left_latex_abs_val) ||
                openBracket.equals(Brackets.abs_val_open)) {
            BasicFunctionsTranslator bft = getConfig().getBasicFunctionsTranslator();
            seq = bft.translate(
                    new String[] {
                            stripMultiParentheses(localTranslations.removeLastExpression())
                    },
                    Keys.KEY_ABSOLUTE_VALUE
            );
        } else if (setMode) { // in set mode, both parenthesis may not match!
            seq = openBracket.getAppropriateString() + localTranslations.removeLastExpression() + bracket.getAppropriateString();
        } else { // otherwise, parenthesis must match each other, so close as it opened
            seq = openBracket.getAppropriateString() + localTranslations.removeLastExpression() + openBracket.getCounterPart().getAppropriateString();
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

    private String checkMultiplyAddition(PomTaggedExpression exp, List<PomTaggedExpression> exp_list, String part) {
        TranslatedExpression global = getGlobalTranslationList();

        exp = treatFirstExpression(part, exp);

        if (checkParenthesisFirst(exp_list)) {
            Matcher m = MULTIPLY_PATTERN.matcher(part);
            if ( m.matches() ) return m.group(1);
            else return part;
        }

        if (addMultiply(exp, exp_list) /*&& !part.matches(".*\\*\\s*")*/) {
            part += MULTIPLY;
            // the global list already got each element before,
            // so simply replace the last if necessary
            String tmp = global.getLastExpression();
            global.replaceLastExpression(tmp + MULTIPLY);
        } else if (addSpace(exp, exp_list)) {
            if ( !part.matches(".*\\s+$") )
                part += SPACE;

            // the global list already got each element before,
            // so simply replace the last if necessary
            String tmp = global.getLastExpression();
            if ( !tmp.matches(".*\\s+$") )
                tmp += SPACE;

            global.replaceLastExpression(tmp);
        }
        return part;
    }

    private PomTaggedExpression treatFirstExpression(
            String part, PomTaggedExpression exp
    ) {
        if (part.matches(STRING_END_TREAT_AS_CLOSED_PARANTHESIS)) {
            MathTerm tmp = FakeMLPGenerator.generateClosedParenthesesMathTerm();
            exp = new PomTaggedExpression(tmp);
        } else if (MULTIPLY_PATTERN.matcher(part).matches()) {
            exp = new PomTaggedExpression(new MathTerm(getConfig().getMULTIPLY(), MathTermTags.multiply.tag()));
        }
        return exp;
    }

    private boolean checkParenthesisFirst(List<PomTaggedExpression> expList) {
        return openBracket != null && (openBracket.equals(Brackets.abs_val_close) || openBracket.equals(Brackets.abs_val_open)) &&
                expList != null && !expList.isEmpty() && expList.get(0).getRoot().getTermText().matches(Brackets.ABSOLUTE_VAL_TERM_TEXT_PATTERN);
    }

    /**
     * Returns true if there has to be a space symbol following the current expression.
     *
     * @param currExp  the current expression
     * @param exp_list the following expressions
     * @return true if the current expressions needs an white space symbol behind its translation
     */
    private boolean addSpace(PomTaggedExpression currExp, List<PomTaggedExpression> exp_list) {
        try {
            if (exp_list == null || exp_list.size() < 1) {
                return false;
            }
            MathTerm curr = currExp.getRoot();
            MathTerm next = exp_list.get(0).getRoot();

            if (FeatureSetUtility.isConsideredAsRelation(curr) || FeatureSetUtility.isConsideredAsRelation(next))
                return true;

            return !(curr.getTag().matches(MathTermTags.PARENTHESIS_PATTERN)
                    || next.getTag().matches(MathTermTags.PARENTHESIS_PATTERN)
                    || next.getTermText().matches(SPECIAL_SYMBOL_PATTERN_FOR_SPACES)
            );
        } catch (Exception e) {
            return false;
        }
    }
}
