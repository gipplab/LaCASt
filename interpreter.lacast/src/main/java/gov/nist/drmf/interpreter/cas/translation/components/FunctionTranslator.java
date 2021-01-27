package gov.nist.drmf.interpreter.cas.translation.components;

import gov.nist.drmf.interpreter.cas.logging.TranslatedExpression;
import gov.nist.drmf.interpreter.cas.translation.AbstractListTranslator;
import gov.nist.drmf.interpreter.cas.translation.AbstractTranslator;
import gov.nist.drmf.interpreter.common.InformationLogger;
import gov.nist.drmf.interpreter.common.constants.Keys;
import gov.nist.drmf.interpreter.common.exceptions.TranslationException;
import gov.nist.drmf.interpreter.common.exceptions.TranslationExceptionReason;
import gov.nist.drmf.interpreter.common.symbols.BasicFunctionsTranslator;
import gov.nist.drmf.interpreter.pom.common.grammar.Brackets;
import gov.nist.drmf.interpreter.pom.common.grammar.MathTermTags;
import mlp.MathTerm;
import mlp.PomTaggedExpression;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

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

    public FunctionTranslator(AbstractTranslator superTranslator) {
        super(superTranslator);
        this.localTranslations = new TranslatedExpression();
        this.basicFT = getConfig().getBasicFunctionsTranslator();
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

        // remove the starting backslash
        String output;
        if (term.getTermText().startsWith(CHAR_BACKSLASH))
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

        // get first expression
        PomTaggedExpression first = following_exp.remove(0);

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
        }

        // translate the argument in the general way
        TranslatedExpression translation = parseGeneralExpression(first, following_exp);

        // find out if we should wrap parenthesis around or not
        int num = translation.getLength();
        String arg = Brackets.removeEnclosingBrackets(translation.toString());
        String translatedExpression = basicFT.translate(new String[]{arg}, Keys.MLP_KEY_FUNCTION_ARGS);

        // take over the parsed expression
        localTranslations.addTranslatedExpression(translatedExpression);
        localTranslations.mergeAll();

        // update global
        TranslatedExpression global = super.getGlobalTranslationList();
        // remove all variables and put them together as one object
        global.removeLastNExps(num);
        global.addTranslatedExpression(translatedExpression);

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
}
