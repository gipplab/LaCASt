package gov.nist.drmf.interpreter.cas.translation.components;

import gov.nist.drmf.interpreter.cas.logging.TranslatedExpression;
import gov.nist.drmf.interpreter.cas.translation.AbstractListTranslator;
import gov.nist.drmf.interpreter.cas.translation.AbstractTranslator;
import gov.nist.drmf.interpreter.common.InformationLogger;
import gov.nist.drmf.interpreter.common.constants.Keys;
import gov.nist.drmf.interpreter.common.exceptions.TranslationException;
import gov.nist.drmf.interpreter.common.exceptions.TranslationExceptionReason;
import gov.nist.drmf.interpreter.common.symbols.BasicFunctionsTranslator;
import gov.nist.drmf.interpreter.pom.common.FakeMLPGenerator;
import gov.nist.drmf.interpreter.pom.common.MathTermUtility;
import gov.nist.drmf.interpreter.pom.common.grammar.Brackets;
import gov.nist.drmf.interpreter.pom.common.grammar.MathTermTags;
import gov.nist.drmf.interpreter.pom.extensions.PrintablePomTaggedExpression;
import mlp.MathTerm;
import mlp.PomTaggedExpression;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static gov.nist.drmf.interpreter.cas.common.DLMFPatterns.CHAR_BACKSLASH;

/**
 * The function translation parses simple functions and not special functions!
 * These "simple" functions are functions without a DLMF macro. We don't
 * really know how to translate these functions. So we will translate them
 * by simply remove the backslash.
 * <p>
 * If the global-lexicon doesn't contains the cosine function it is just a
 * simple function than. When our lexicon is complete, this translation becomes
 * a bit redundant.
 * <p>
 * Like the MacroTranslator, the function translation should translate the start expression
 * as well (the function itself) and after that the argument.
 * <p>
 * For instance: cos{2}
 * 1) translate the expression cos first
 * 2) after that the list of arguments, here 2
 *
 * @author Andre Greiner-Petter
 * @see Brackets
 * @see AbstractTranslator
 * @see AbstractListTranslator
 * @see TranslatedExpression
 */
public class FunctionTranslator extends AbstractListTranslator {
    private static final Logger LOG = LogManager.getLogger(FunctionTranslator.class.getName());

    private final TranslatedExpression localTranslations;

    private final BasicFunctionsTranslator basicFT;

    private final Pattern ENDS_ON_MULTIPLY_PATTERN;

    public FunctionTranslator(AbstractTranslator superTranslator) {
        super(superTranslator);
        this.localTranslations = new TranslatedExpression();
        this.basicFT = getConfig().getBasicFunctionsTranslator();
        this.ENDS_ON_MULTIPLY_PATTERN = Pattern.compile("(.*)"+Pattern.quote(getConfig().getMULTIPLY())+"\\s*");
    }

    @Override
    public TranslatedExpression getTranslatedExpressionObject() {
        return localTranslations;
    }

    @Override
    public TranslatedExpression translate(PomTaggedExpression exp, List<PomTaggedExpression> following)
            throws TranslationException {
        LOG.debug("Trigger general function translator");
        if ( considerItAsAlphanumeric(exp, following) ) {
            LOG.debug("Detected function that looks like alphanumeric (no arguments and no leading backslash) continue as alphanumeric.");
            // inform the user that we take it as alphanumeric
            InformationLogger infoLogger = super.getInfoLogger();
            infoLogger.addGeneralInfo(
                    exp.getRoot().getTermText(),
                    "Was tagged as a function but it does not look like it and there are no arguments. " +
                            "Hence, we interpret it as a string, rather than an actual function. " +
                            "Use '\\operatorname' to forcefully translate it as a function."
            );
            exp.getRoot().setTag( MathTermTags.alphanumeric.tag() );
            MathTermTranslator mtt = new MathTermTranslator(this);
            localTranslations.addTranslatedExpression(mtt.translate(exp, following));
            return localTranslations;
        }

        translate(exp);
        parse(following);

        // a bit redundant, num is always 2!
        int num = localTranslations.mergeAll();

        TranslatedExpression global = super.getGlobalTranslationList();
        global.mergeLastNExpressions(num);
        return localTranslations;
    }

    private boolean considerItAsAlphanumeric(PomTaggedExpression exp, List<PomTaggedExpression> following) {
        if ( !exp.getRoot().getTermText().startsWith("\\") ) {
            // a function without leading backslash. Maybe we should take it as alphanumeric?
            // ok, lets take it as alphanumeric if, and only if there are no arguments (following is empty)
            return following == null || following.isEmpty();
        } else return false;
    }

    /**
     * This translate method has to be invoked before {@link #parse(List)}.
     * It only parses the function itself (like cos(2), cos is the first part).
     *
     * @param exp the first expression that contains the function
     *            (it contains cos, for instance)
     * @return true when everything is good
     */
    @Override
    public TranslatedExpression translate(PomTaggedExpression exp) {
        MathTerm term = exp.getRoot();
        if (term == null || term.isEmpty()) {
            throw TranslationException.buildException(this,
                    "Function has no MathTerm!",
                    TranslationExceptionReason.UNKNOWN_OR_MISSING_ELEMENT);
        }

        String output;
        if ( MathTermUtility.isGreekLetter(term) ) {
            GreekLetterTranslator glt = new GreekLetterTranslator(getSuperTranslator());
            TranslatedExpression ti = glt.translate(exp);
            getGlobalTranslationList().removeLastNExps(ti.getLength());
            output = ti.toString();
        } // remove the starting backslash
        else if (term.getTermText().startsWith(CHAR_BACKSLASH))
            output = term.getTermText().substring(1);
        else output = term.getTermText();

        // add it to global and local
        localTranslations.addTranslatedExpression(output);
        TranslatedExpression global = super.getGlobalTranslationList();
        global.addTranslatedExpression(output);

        // inform the user that we usually don't know how to handle it.
        InformationLogger infoLogger = super.getInfoLogger();
        infoLogger.addGeneralInfo(
                term.getTermText(),
                "Function without DLMF-Definition. " +
                        "We keep it like it is (but delete prefix \\ if necessary)."
        );

        return localTranslations;
    }

    /**
     * The second part of the translation function parses the argument part of
     * an unknown function. For instance if \cos(2+2), this translate method gets
     * 2+2 as argument list.
     *
     * @param following_exp the descendants of a previous function {@link #translate(PomTaggedExpression)}
     * @return true if everything was fine
     */
    private TranslatedExpression parse(List<PomTaggedExpression> following_exp) {
        if ( following_exp == null || following_exp.isEmpty() ) {
            throw TranslationException.buildException(
                    this, "Unable to retrieve argument of function.",
                    TranslationExceptionReason.INVALID_LATEX_INPUT);
        }

        int definitionIndex = getDefinitionIndex(0, following_exp);

        // get first expression
        PomTaggedExpression first = following_exp.remove(0);
        int idxReduction = 1;

        // if it starts with a caret, we have a little problem.
        // classical case \cos^b(a). This is typical and easy
        // to read for people but hard to understand for CAS.
        // usually we translate it the way around: \cos(a)^b.
        // That's why we need to check this here!
        PomTaggedExpression powerExp = null;
        if (MathTermTags.is(first, MathTermTags.caret)) {
            powerExp = first;
            if ( following_exp.isEmpty() ) throw TranslationException.buildException(
                    this, "Unable to retrieve argument of function.",
                    TranslationExceptionReason.INVALID_LATEX_INPUT);
            first = following_exp.remove(0);
            idxReduction++;
        }

        if ( MathTermUtility.isRelationSymbol(first.getRoot()) ) {
            throw TranslationException.buildExceptionObj( this,
                    "Encounter illegal function argument " + first.getRoot().getTermText(),
                    TranslationExceptionReason.LATEX_MACRO_ERROR, first);
        }

        // translate the argument in the general way
        List<PomTaggedExpression> nextElements;
        if ( definitionIndex > 0 ) {
            nextElements = new LinkedList<>(following_exp.subList(0, definitionIndex-idxReduction));
            following_exp.subList(0, definitionIndex+2-idxReduction).clear();
        } else nextElements = following_exp;

        TranslatedExpression translation = parseGeneralExpression(first, nextElements);
        if ( translation.toString().isBlank() ) {
            String updatedLastElement = getGlobalTranslationList().getLastExpression();
            localTranslations.replaceLastExpression(updatedLastElement);
            if ( nextElements.isEmpty() ) throw TranslationException.buildException(
                    this, "Unable to retrieve argument of the function " + localTranslations.toString(),
                    TranslationExceptionReason.INVALID_LATEX_INPUT);
            translation = parseGeneralExpression(nextElements.remove(0), nextElements);
        }

        // find out if we should wrap parenthesis around or not
        int num = translation.getLength();
        Matcher m = ENDS_ON_MULTIPLY_PATTERN.matcher(translation.toString());
        boolean endedOnMultiply = m.matches();
        String argTrans = endedOnMultiply ? m.group(1).trim() : translation.toString();
        String arg = Brackets.removeEnclosingBrackets(argTrans);

        String translatedExpression;
        if ( definitionIndex > 0 ) {
            SequenceTranslator sq = new SequenceTranslator(getSuperTranslator());
            PrintablePomTaggedExpression sequence = FakeMLPGenerator.generateEmptySequencePPTE();
            sequence.setPrintableComponents(following_exp);
            TranslatedExpression te = sq.translate(sequence);
            following_exp.clear();
            getGlobalTranslationList().removeLastNExps(te.getLength());
            getGlobalTranslationList().removeLastNExps(num); // the definition symbol

            String[] args = new String[]{
                    localTranslations.toString(),
                    normalizeArgumentList(arg),
                    te.getTranslatedExpression()
            };
            localTranslations.clear();
            translatedExpression = basicFT.translate(args, Keys.MLP_KEY_FUNCTION_DEF);
        } else {
            translatedExpression = basicFT.translate(new String[]{arg}, Keys.MLP_KEY_FUNCTION_ARGS);
        }

        // take over the parsed expression
        localTranslations.addTranslatedExpression(translatedExpression);
        num = localTranslations.getLength();
        if ( endedOnMultiply ) localTranslations.addTranslatedExpression(getConfig().getMULTIPLY());
        localTranslations.mergeAll();

        // update global
        TranslatedExpression global = super.getGlobalTranslationList();
        // remove all variables and put them together as one object
        global.removeLastNExps(num);
        global.addTranslatedExpression(localTranslations);

        // shit, if there was a caret before the arguments, we need to add
        // these now
        if (powerExp != null) {
            // since the MathTermTranslator handles this, use this class
            MathTermTranslator mp = new MathTermTranslator(getSuperTranslator());
            mp.translate(powerExp);
            localTranslations.replaceLastExpression(global.getLastExpression());
        }

        return localTranslations;
    }

    private String normalizeArgumentList(String args) {
        if ( args == null || args.isBlank() ) return args;

        String[] argElements = args.split("[,;]");
        boolean isMath = Keys.KEY_MATHEMATICA.equals(getConfig().getTO_LANGUAGE());
        for ( int i = 0; i < argElements.length; i++ ) {
            argElements[i] = argElements[i].trim();
            if ( isMath ) argElements[i] += "_";
        }
        return String.join(", ", argElements);
    }

    /**
     * Returns true if the right after the following expressions (skipping primes and parenthesis (argument) exprs).
     * This means, we skip all that stuff and check if := is coming right after the argument.
     * @param followingExps list of following arguments
     * @return true if a definition symbol is following the argument
     */
    private int getDefinitionIndex(int start, List<PomTaggedExpression> followingExps) {
        if ( getGlobalTranslationList().getLength() > 1 ) {
            // otherwise it might be an implicit definition which we do not support by now
            return -1;
        }
        if ( start >= followingExps.size() ) return -1;
        if ( followingExps.isEmpty() || followingExps.get(start) == null || followingExps.get(start).isEmpty() ) return -1;

        PomTaggedExpression next = followingExps.get(start);
        LinkedList<Brackets> bracketStack = new LinkedList<>();
        Brackets bracket = Brackets.ifIsBracketTransform(next.getRoot(), null);
        int idx = start+1;
        if ( bracket != null && bracket.opened ) {
            bracketStack.add(bracket);
            idx = skipBrackets(idx, followingExps, bracketStack);
        } else if ( MathTermUtility.equals(next.getRoot(), MathTermTags.primes) ) {
            return getDefinitionIndex(start + 1, followingExps);
        } else {
            // otherwise it is a single argument... so we simply skip it and check if := is following
            idx++;
        }

        if ( idx < 0 || idx >= followingExps.size() ) return -1;

        next = followingExps.get(idx);
        if ( next == null || next.getNextSibling() == null ) return -1;

        PomTaggedExpression nextNext = next.getNextSibling();
        if (MathTermUtility.equals(next.getRoot(), MathTermTags.colon) &&
                MathTermUtility.equals(nextNext.getRoot(), MathTermTags.equals)) {
            return idx;
        } else return -1;
    }
}
