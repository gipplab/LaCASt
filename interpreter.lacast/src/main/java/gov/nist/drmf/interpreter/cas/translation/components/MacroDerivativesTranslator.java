package gov.nist.drmf.interpreter.cas.translation.components;

import gov.nist.drmf.interpreter.cas.logging.TranslatedExpression;
import gov.nist.drmf.interpreter.cas.translation.AbstractTranslator;
import gov.nist.drmf.interpreter.cas.translation.components.util.DerivativeAndPowerHolder;
import gov.nist.drmf.interpreter.cas.translation.components.util.MacroInfoHolder;
import gov.nist.drmf.interpreter.cas.translation.components.util.VariableExtractor;
import gov.nist.drmf.interpreter.common.constants.Keys;
import gov.nist.drmf.interpreter.common.exceptions.TranslationException;
import gov.nist.drmf.interpreter.common.exceptions.TranslationExceptionReason;
import gov.nist.drmf.interpreter.common.grammar.DLMFFeatureValues;
import gov.nist.drmf.interpreter.common.grammar.ExpressionTags;
import gov.nist.drmf.interpreter.common.grammar.MathTermTags;
import gov.nist.drmf.interpreter.mlp.FakeMLPGenerator;
import gov.nist.drmf.interpreter.mlp.MathTermUtility;
import gov.nist.drmf.interpreter.mlp.PomTaggedExpressionUtility;
import mlp.FeatureSet;
import mlp.MathTerm;
import mlp.PomTaggedExpression;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * @author Andre Greiner-Petter
 */
public class MacroDerivativesTranslator extends MacroTranslator {
    private static final Logger LOG = LogManager.getLogger(MacroDerivativesTranslator.class.getName());

    private TranslatedExpression translatedInAdvance;
    private int leadingReplacementMemory = 0;

    private final String CAS;

    MacroDerivativesTranslator(AbstractTranslator superTranslator) {
        super(superTranslator);
        resetTranslatedInAdvancedComponent();
        this.CAS = getConfig().getTO_LANGUAGE();
    }

    /**
     * The translation in advanced object covers the edge case, when we search for the arguments of a derivative.
     * Since the end of the argument may not be clear (e.g. in <code>\deriv{}{x} x^2 + 2</code> it is not clear if
     * <code>+2</code> is also the argument of deriv or not), we use a special approach that searches for the variable
     * (here <code>x</code>) in the following expressions. To search more easily, every following expression gets
     * translated and then checked for the variable. Hence, <code>x^2 + 2</code> is already translated, even though
     * <code>+2</code> was not part of the argument.
     *
     * This translation in advanced object covers this <code>+2</code> and tells the {@link MacroTranslator} that
     * there are already translated expressions that were logically removed from the {@link PomTaggedExpression} parse
     * tree. If we forget that, it means the translator would translate <code>+2</code> here, but never add it to the
     * final translation object.
     *
     * @return true if there are expressions that were translated in advance
     */
    public boolean hasTranslatedInAdvancedComponent() {
        return translatedInAdvance != null;
    }

    TranslatedExpression getTranslatedInAdvanceComponent() {
        return translatedInAdvance;
    }

    void resetTranslatedInAdvancedComponent() {
        this.translatedInAdvance = null;
    }

    public TranslatedExpression updateNegativeReplacement(TranslatedExpression te) {
        te.setNegativeReplacements( this.leadingReplacementMemory );
        this.leadingReplacementMemory = 0;
        return te;
    }

    /**
     * Parses derivative notations (primes and numeric lagrange notation), and carets if any.
     * @param following_exps the next expressions
     * @param info the information holder
     * @return information regarding differentiation and carets. Both might be null!
     */
    public DerivativeAndPowerHolder parseDerivatives(
            List<PomTaggedExpression> following_exps,
            MacroInfoHolder info
    ) {
        DerivativeAndPowerHolder holder = new DerivativeAndPowerHolder();
        int numberOfDerivative = 0; // no differentiation by default

        // check for optional arguments
        while (!following_exps.isEmpty()) {
            PomTaggedExpression exp = following_exps.get(0);

            // if the next element is neither @, ^ nor ', we can stop already.
            if (exp.isEmpty()) {
                return holder;
            }

            MathTermTags tag = MathTermTags.getTagByKey(exp.getRoot().getTag());
            if ( tag == null ) break;
            switch (tag) {
                case caret:
                    parseCaret(exp, following_exps, info, holder);
                    break;
                case prime:
                    // well, just count them up
                    following_exps.remove(0);
                    numberOfDerivative++;
                    break;
                case at: // we reached the end
                default: // in any other case, we also reached the end...
                    checkValidity(numberOfDerivative, holder);
                    return holder;
            }
        }

        return holder;
    }

    private void checkValidity(
            int numberOfDerivative,
            DerivativeAndPowerHolder holder
    ) {
        if ( numberOfDerivative > 0 ) {
            if ( holder.getDifferentiation() != null ) {
                throw TranslationException.buildException(
                        this,
                        "It's not allowed to mix prime and " +
                                "numeric differentiation notation within one function call.",
                        TranslationExceptionReason.INVALID_LATEX_INPUT);
            }
            holder.setDifferentiation(Integer.toString(numberOfDerivative));
        }
    }

    private void parseCaret(
            PomTaggedExpression exp,
            List<PomTaggedExpression> followingExps,
            MacroInfoHolder info,
            DerivativeAndPowerHolder holder
    ) {
        // check if it's the lagrange notation
        if (isLagrangeNotation(exp.getComponents())) {
            if ( info.getSlotOfDifferentiation() < 0 ) {
                throw throwDifferentiationException();
            } else if ( holder.getDifferentiation() != null ) {
                throw TranslationException.buildException(
                        this,
                        "Cannot parse lagrange notation twice for the same macro!",
                        TranslationExceptionReason.INVALID_LATEX_INPUT
                );
            } else {
                followingExps.remove(0);
                parseLagrangeNotation(exp.getComponents(), holder);
            }
        } else {
            // found a normal power. So move it to the end
            holder.setMoveToEnd( followingExps.remove(0) );
        }
    }

    /**
     * This method handles the special case of \deriv macros, since the first argument of a \deriv macro is often
     * empty. For example, you write more often
     * <code>d/dx x^2</code> rather then <code>dx^2/dx</code>
     *
     * Hence, this method follows the sum/product approach to find the first argument. There is one special case though,
     * sometimes you may want to write it in reverse order, such as <code>x^2 d/dx</code>, which is the opposite of the
     * sum/product approach. In such a case, the argument was already translated (which makes it even easier for us,
     * because of the special approach we used).
     *
     * @param followingExps the arguments of the deriv macro (must be at least of length two)
     * @param info information about the macro, such as translation patterns
     * @return the parsed arguments in the right order
     */
    public LinkedList<String> parseDerivativeArguments(List<PomTaggedExpression> followingExps, MacroInfoHolder info ){
        // there are two options here, one easy and one complex
        // first the easy, the next element is not empty:
        PomTaggedExpression next = followingExps.get(0);
        if ( !next.isEmpty() ) {
            // nothing special, just go ahead and parse it as usual
            return parseArguments(followingExps, info);
        }

        // first, get rid of the empty element
        followingExps.remove(0);

        // Since it's empty, we have a problem similar to sums. When does the argument ends?
        // so lets follow sums approach
        LinkedList<String> vars = new LinkedList<>();
        TranslatedExpression translatedPotentialArguments = getArgumentsBasedOnDiffVar(followingExps, vars);
        TranslatedExpression transArgs;

        if ( translatedPotentialArguments == null ) {
            // ok the argument is not following but was leading the deriv
            TranslatedExpression globalTranslations = getGlobalTranslationList();
            transArgs = globalTranslations.removeUntilFirstAppearanceOfVar(vars, getConfig().getMULTIPLY());
            globalTranslations.removeLastNExps(transArgs.getLength());
            leadingReplacementMemory = transArgs.getLength();
        } else {
            // clean up first
            getGlobalTranslationList().removeLastNExps(translatedPotentialArguments.getLength());

            // now, search for the next argument
            transArgs =
                    translatedPotentialArguments.removeUntilLastAppearanceOfVar(
                            vars,
                            getConfig().getMULTIPLY()
                    );

            translatedInAdvance = translatedPotentialArguments;
        }

        LinkedList<String> args = new LinkedList<>();
        args.add(transArgs.toString());
        args.add(vars.getFirst());
        return args;
    }

    private TranslatedExpression getArgumentsBasedOnDiffVar(List<PomTaggedExpression> followingExps, List<String> vars) {
        // otherwise! we have a problem similar to sums. When does the argument ends?
        // so lets follow sums approach
        PomTaggedExpression variablePTE = followingExps.remove(0);
        TranslatedExpression varTE = translateInnerExp(variablePTE, new LinkedList<>());

        vars.add(varTE.toString());

        List<PomTaggedExpression> potentialArgs = new LinkedList<>();
        try {
            potentialArgs = VariableExtractor.getPotentialArgumentsUntilEndOfScope(
                    followingExps,
                    vars,
                    this
            );
        } catch ( TranslationException te ) {
            if ( !TranslationExceptionReason.INVALID_LATEX_INPUT.equals(te.getReason()) ) throw te;
        }

        if ( !potentialArgs.isEmpty() ) {
            // the potential arguments is a theoretical sequence, so handle it as a sequence!
            PomTaggedExpression topSeqPTE = FakeMLPGenerator.generateEmptySequencePPTE();
            for ( PomTaggedExpression pte : potentialArgs ) topSeqPTE.addComponent(pte);

            SequenceTranslator p = new SequenceTranslator(getSuperTranslator());
            return p.translate( topSeqPTE );
        } else return null;
    }

    /**
     * In \<macro>^{(<order>)}@{...}, extracts the <order> as the order of differentiation for the macro
     *
     * @param following_exps .
     * @param holder .
     */
    private void parseLagrangeNotation(
            List<PomTaggedExpression> following_exps,
            DerivativeAndPowerHolder holder
    ) {
        // translate the order
        TranslatedExpression lagrangeExpr = parseGeneralExpression(following_exps.remove(0), following_exps);

        // clean up global translation list
        TranslatedExpression global = getGlobalTranslationList();
        global.removeLastNExps(lagrangeExpr.getLength());

        String diff = stripMultiParentheses(lagrangeExpr.toString());

        // update info holder
        holder.setDifferentiation(diff);
    }

    public String extractVariableOfDifferentiation(List<PomTaggedExpression> arguments) {
        PomTaggedExpression exp = arguments.get(0);
        Set<String> variableCandidates = new HashSet<>(extractVariableOfDiff(exp));

        for ( int i = 1; i < arguments.size(); i++ ) {
            updateSetOfCandidates(arguments.get(i), variableCandidates);
        }

        if ( variableCandidates.size() != 1 )
            throw throwMacroException("Unable to extract unique variable of differentiation. Found: " + variableCandidates);

        return variableCandidates.stream().findFirst().get();
    }

    private void updateSetOfCandidates(PomTaggedExpression exp, Set<String> variableCandidates) {
        if ( variableCandidates.isEmpty() ) {
            throw throwMacroException("Unable to extract variable of differentiation");
        }

        Set<String> set = extractVariableOfDiff(exp);
        variableCandidates.retainAll(set);
    }

    private Set<String> extractVariableOfDiff(List<PomTaggedExpression> expressions) {
        Set<String> variableCandidates = new HashSet<>();
        for ( int i = 0; i < expressions.size(); i++ ) {
            PomTaggedExpression p = expressions.get(i);
            MathTerm mt = p.getRoot();

            if ( isDLMFMacro(mt) ) {
                i = handleMacro(mt, expressions, i, variableCandidates);
            } else {
                variableCandidates.addAll(extractVariableOfDiff(p));
            }

        }
        return variableCandidates;
    }

    private int handleMacro(MathTerm mt, List<PomTaggedExpression> expressions, int i, Set<String> variableCandidates) {
        FeatureSet fset = mt.getNamedFeatureSet(Keys.KEY_DLMF_MACRO);
        Integer slot;
        Integer numberOfVariables;
        try {
            slot = Integer.parseInt(DLMFFeatureValues.SLOT_DERIVATIVE.getFeatureValue(fset, CAS));
            numberOfVariables = Integer.parseInt(DLMFFeatureValues.NUMBER_OF_VARIABLES.getFeatureValue(fset, CAS));
        } catch (NumberFormatException e) {
            LOG.warn("No slot of differentiation found for " + mt.getTermText() + ", assuming its 1.");
            slot = 1;
            numberOfVariables = 1;
        }

        try {
            i = skipAllAts(expressions, i);
            int idx = i + slot - 1; // the slot is 1 not 0, so we must add -1
            variableCandidates.addAll(extractVariableOfDiff(expressions.get(idx)));
            return i + numberOfVariables;
        } catch (IndexOutOfBoundsException e) {
            throw throwMacroException("Unable to find @ after a DLMF macro. That is invalid syntax");
        }
    }

    private int skipAllAts(List<PomTaggedExpression> list, int startIdx) {
        boolean passedAtYet = false;
        for ( int i = startIdx; i < list.size(); i++ ) {
            PomTaggedExpression tmp = list.get(i);
            boolean isAt = MathTermUtility.equals(tmp.getRoot(), MathTermTags.at);
            if ( isAt && !passedAtYet ) passedAtYet = true;
            else if ( !isAt && passedAtYet ) {
                // so passed ats and thats the first that is not an at... we found it
                return i;
            }
        }
        throw new IndexOutOfBoundsException();
    }

    private Set<String> extractVariableOfDiff(PomTaggedExpression exp) {
        if (PomTaggedExpressionUtility.isSequence(exp)) {
            return extractVariableOfDiff(exp.getComponents());
        }

        // ok in this case, its a single expression
        // now we have the good old identifier problem.
        MathTermTags tag = MathTermTags.getTagByExpression(exp);
        Set<String> set = new HashSet<>();
        switch (tag) {
            case alphanumeric:
            case special_math_letter:
            case symbol:
            case letter:
                set.add(exp.getRoot().getTermText());
        }
        return set;
    }

    /**
     * Checks weather the first element is a differentiation in lagrange notation. That means
     * the order is given in parentheses.
     *
     * @param following_exps the children of caret
     * @return .
     */
    public static boolean isLagrangeNotation(List<PomTaggedExpression> following_exps) {
        try {
            PomTaggedExpression exp = following_exps.get(0);
            ExpressionTags eTag = ExpressionTags.getTagByKey(exp.getTag());
            if (!eTag.equals(ExpressionTags.sequence)) {
                return false;
            }

            List<PomTaggedExpression> children = exp.getComponents();
            MathTerm firstElement = children.get(0).getRoot();
            MathTermTags firstTag = MathTermTags.getTagByKey(firstElement.getTag());

            MathTerm lastElement = children.get(children.size() - 1).getRoot();
            MathTermTags lastTag = MathTermTags.getTagByKey(lastElement.getTag());

            return (
                    firstTag.equals(MathTermTags.left_parenthesis) || firstTag.equals(MathTermTags.left_delimiter)
            ) && (
                    lastTag.equals(MathTermTags.right_parenthesis) || lastTag.equals(MathTermTags.right_delimiter)
            );
        } catch ( NullPointerException | IndexOutOfBoundsException e ) {
            return false;
        }
    }

    private TranslationException throwSlotError() throws TranslationException {
        return throwMacroException(
                "No information in lexicon for slot of differentiation of macro."
        );
    }

    private TranslationException throwDifferentiationException() throws TranslationException {
        return throwMacroException(
                "Cannot combine prime differentiation notation with Leibniz notation differentiation "
        );
    }
}
