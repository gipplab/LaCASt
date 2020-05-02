package gov.nist.drmf.interpreter.cas.translation.components;

import gov.nist.drmf.interpreter.cas.common.DLMFMacroInfoHolder;
import gov.nist.drmf.interpreter.cas.common.DLMFPatterns;
import gov.nist.drmf.interpreter.cas.logging.TranslatedExpression;
import gov.nist.drmf.interpreter.cas.translation.AbstractListTranslator;
import gov.nist.drmf.interpreter.cas.translation.AbstractTranslator;
import gov.nist.drmf.interpreter.common.InformationLogger;
import gov.nist.drmf.interpreter.common.constants.GlobalConstants;
import gov.nist.drmf.interpreter.common.constants.Keys;
import gov.nist.drmf.interpreter.common.exceptions.TranslationException;
import gov.nist.drmf.interpreter.common.exceptions.TranslationExceptionReason;
import gov.nist.drmf.interpreter.common.grammar.DLMFFeatureValues;
import gov.nist.drmf.interpreter.common.grammar.ExpressionTags;
import gov.nist.drmf.interpreter.common.grammar.MathTermTags;
import gov.nist.drmf.interpreter.common.symbols.BasicFunctionsTranslator;
import gov.nist.drmf.interpreter.mlp.FakeMLPGenerator;
import mlp.FeatureSet;
import mlp.MathTerm;
import mlp.PomTaggedExpression;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static gov.nist.drmf.interpreter.cas.common.DLMFPatterns.TEMPORARY_VARIABLE_NAME;

/**
 * This translation parses all of the DLMF macros. A DLMF macro
 * has always a feature set named dlmf-macro {@link Keys#KEY_DLMF_MACRO}.
 * This feature set has a lot of important features, like the number of
 * variables and links and so on.
 * <p>
 * This parsers parses first all of the components of the DLMF macro.
 * For instance, JacobiP has 3 parameter and 1 variable. It parses the
 * following 4 continuous expressions and store them in an array.
 * After that, it replaces all placeholder in the translation by these
 * stored expressions.
 *
 * @author Andre Greiner-Petter
 * @see Keys
 * @see AbstractTranslator
 * @see gov.nist.drmf.interpreter.cas.logging.TranslatedExpression
 * @see InformationLogger
 */
public class MacroTranslator extends AbstractListTranslator {
    private static final Logger LOG = LogManager.getLogger(MacroTranslator.class.getName());

    private static final Pattern optional_params_pattern =
            Pattern.compile("\\s*\\[(.*)]\\s*\\*?\\s*");

    private static final Pattern leibniz_notation_pattern =
            Pattern.compile("\\s*\\(([^@]*)\\)\\s*");

    private final String CAS;

    private TranslatedExpression localTranslations;

    private String macro;

    private boolean isWronskian = false;
    private boolean isDeriv = false;

    private List<PomTaggedExpression> tempArgList;
    private String varOfDiff;

    private TranslatedExpression translatedInAdvance;

    public MacroTranslator(AbstractTranslator superTranslator) {
        super(superTranslator);
        this.localTranslations = new TranslatedExpression();
        this.CAS = getConfig().getTO_LANGUAGE();
        this.tempArgList = new LinkedList<>();
        this.translatedInAdvance = null;
    }

    @Override
    public TranslatedExpression getTranslatedExpressionObject() {
        return localTranslations;
    }

    @Override
    public TranslatedExpression translate(PomTaggedExpression exp, List<PomTaggedExpression> following) {
        MathTerm mt = exp.getRoot();
        if (mt == null || mt.isEmpty()) {
            throw TranslationException.buildException(this, "The wrong translator is used, the expression is not a DLMF macro.",
                    TranslationExceptionReason.IMPLEMENTATION_ERROR);
        }

        isWronskian = mt.getTermText().equals("\\Wronskian");
        isDeriv = mt.getTermText().matches(DLMFPatterns.DERIV_NOTATION);

        if (isWronskian) {
            splitComma(following);
        }
        return parse(exp, following);
    }

    /**
     * Parses the following arguments after the DLMF macro.
     * <p>
     * A special macro is build as the following
     * \macro[]{}@{}
     * 1) macro name
     * 2) There are multiple options here. Note that 2.2-2.3 are ONLY valid after optional arguments and parameters!
     * 2.1) Optional arguments starting with []
     * 2.2) Carets, indicating a power (will be moved to the end) or the lagrange notation indicating derivative
     * 2.3) Prime symbol indicating derivative
     * 3) A certain number of @ symbols
     * 4) Arguments
     *
     * @param exp            the DLMF macro
     * @param following_exps following expressions of the DLMF macro
     * @return true if the translation was successful, otherwise false
     */
    private TranslatedExpression parse(PomTaggedExpression exp, List<PomTaggedExpression> following_exps) {
        MathTerm macro_term = exp.getRoot();
        this.macro = macro_term.getTermText();

        // ok first, get the feature set!
        FeatureSet fset = macro_term.getNamedFeatureSet(Keys.KEY_DLMF_MACRO);
        DLMFMacroInfoHolder info = getInfos(fset, macro);

        // first, lets check if this function has no arguments (single symbol)
        if (info.getNumOfAts() + info.getNumOfVars() + info.getNumOfParams() == 0) {
            // inform about the translation decision
            super.getInfoLogger().addMacroInfo(
                    macro_term.getTermText(),
                    createFurtherInformation(info)
            );

            // just add the translated representation extracted from feature set
            localTranslations.addTranslatedExpression(info.getTranslationPattern());
            super.getGlobalTranslationList()
                    .addTranslatedExpression(info.getTranslationPattern());

            // done
            return localTranslations;
        }

        // now check for optional parameters, if any
        LinkedList<String> optionalParas = parseOptionalParameters(following_exps);
        int extractedOptParameter = optionalParas.size();

        // in case of optional arguments, we have to retrieve other information from the lexicons
        if (extractedOptParameter > 0) {
            fset = macro_term.getNamedFeatureSet(Keys.KEY_DLMF_MACRO_OPTIONAL_PREFIX + optionalParas.size());
            info = getInfos(fset, macro_term.getTermText());
        }

        // in case there are parameters, we parse them first
        // empty if none
        LinkedList<String> parameters = parseParameters(following_exps, info.getNumOfParams());

        // parse derivatives, lagrange notation or primes
        DiffAndPowerHolder diffPowerHolder = parseDerivatives(following_exps, info);

        LinkedList<String> arguments;
        if ( isWronskian ) arguments = parseArguments(tempArgList, info);
        else if ( isDeriv ) arguments = parseDerivArguments(following_exps, info);
        else arguments = parseArguments(following_exps, info);

        if ( isWronskian ) {
            skipAts(following_exps);
        }

        // log information
        String info_key = macro_term.getTermText();
        if (extractedOptParameter != 0) {
            info_key += extractedOptParameter;
        }

        int slotOfDifferentiation = info.getSlotOfDifferentiation();
        if ( slotOfDifferentiation < 1 && diffPowerHolder.getDifferentiation() != null ) {
            String errorMsg = "No slot of differentiation available for " + macro_term.getTermText() + ", " +
                    "but found differentiation notation " + diffPowerHolder.getDifferentiation();
            throw throwMacroException(errorMsg);
        }

        // if we found optional parameter, the slot of differentiation changes
        // TODO check lexicon entries if that's actually necessary
        if (!optionalParas.isEmpty()) {
            slotOfDifferentiation += optionalParas.size();
        }

        String[] args = createArgumentArray(optionalParas, parameters, arguments);

        // in case we cached a power, it moves to the end. Let's fake this by
        // adding this cached expression back to the start of the remaining
        // expressions
        if (diffPowerHolder.moveToEnd != null) {
            following_exps.add(0, diffPowerHolder.moveToEnd);
        }

        // finally fill the placeholders by values
        fillVars(
                args,
                info_key,
                info,
                diffPowerHolder,
                slotOfDifferentiation
        );

        // in case we translated an expression in advance, we need to fill up the translation lists
        if ( isDeriv && translatedInAdvance != null ) {
            localTranslations.addTranslatedExpression(translatedInAdvance);
            getGlobalTranslationList().addTranslatedExpression(translatedInAdvance);

            // just in case, reset the variable
            translatedInAdvance = null;
        }

        // now we are done
        return localTranslations;
    }

    /**
     * A secure version to store information from an feature set.
     * @param fset future set
     * @param macro the macro
     * @return information about the macro
     */
    private DLMFMacroInfoHolder getInfos(FeatureSet fset, String macro) {
        if ( fset == null ) {
            throw TranslationException.buildExceptionObj(
                    this, "Cannot extract information from feature set: " + macro,
                    TranslationExceptionReason.MISSING_TRANSLATION_INFORMATION,
                    macro);
        }

        // try to extract the information
        try {
            DLMFMacroInfoHolder info = new DLMFMacroInfoHolder(fset, CAS, macro);

            if (info.getTranslationPattern() == null || info.getTranslationPattern().isEmpty()) {
                throw TranslationException.buildExceptionObj(
                        this, "There are no translation patterns available for: " + macro,
                        TranslationExceptionReason.MISSING_TRANSLATION_INFORMATION,
                        macro);
            }

            return info;
        } catch (NullPointerException | TranslationException npe) {
            throw TranslationException.buildExceptionObj(
                    this, "Cannot extract information from feature set: " + macro,
                    TranslationExceptionReason.MISSING_TRANSLATION_INFORMATION,
                    macro);
        }
    }

    /**
     * This function checks if the very next arguments are
     * 1) optional parameters (indicated by [])
     * <p>
     * This function manipulates the argument in case of optional arguments or carets!
     *
     * @param following_exps the expressions right after the macro itself
     */
    private LinkedList<String> parseOptionalParameters(List<PomTaggedExpression> following_exps) {
        LinkedList<String> optionalArguments = new LinkedList<>();

        // if the list is empty, we don't have to do something here
        // an error might be thrown later
        if (following_exps.isEmpty()) {
            return optionalArguments;
        }

        // check for optional arguments
        while (!following_exps.isEmpty()) {
            PomTaggedExpression first = following_exps.get(0);

            // if the next one is empty expression, it cannot be a prime, or caret, or
            if (first.isEmpty()) {
                break;
            }
            MathTerm first_term = first.getRoot();

            if (first_term != null && !first_term.isEmpty()) {
                MathTermTags tag = MathTermTags.getTagByKey(first_term.getTag());

                if (tag.equals(MathTermTags.left_bracket)) {
                    String optional = translateInnerExp(following_exps.remove(0), following_exps).toString();
                    Matcher m = optional_params_pattern.matcher(optional);
                    if (m.matches()) {
                        optionalArguments.add(m.group(1));
                    } else {
                        optionalArguments.add(optional);
                    }
                    // if there were one optional argument, there might be others also
                    continue;
                }
            }
            // always break here!
            break;
        }

        return optionalArguments;
    }

    private LinkedList<String> parseParameters(List<PomTaggedExpression> following_exps, int numberOfParameters) {
        LinkedList<String> parameters = new LinkedList<>();
        for (int i = 0; i < numberOfParameters; i++) {
            PomTaggedExpression exp = following_exps.remove(0);

            // check if that's valid notation
            MathTerm mt = exp.getRoot();
            if (mt != null && !mt.isEmpty()) {
                MathTermTags tag = MathTermTags.getTagByKey(mt.getTag());
                switch (tag) {
                    case prime:
                    case caret:
                        throw TranslationException.buildException(this, "Prime and carets are not allowed before parameters!",
                                TranslationExceptionReason.INVALID_LATEX_INPUT);
                }
            }

            // ok, everything's valid, lets move on
            String translatedPara = translateInnerExp(exp, following_exps).toString();
            parameters.addLast(translatedPara);
        }

        return parameters;
    }

    /**
     * Parses derivative notations (primes and numeric lagrange notation), and carets if any.
     * @param following_exps the next expressions
     * @param info the information holder
     * @return information regarding differentiation and carets. Both might be null!
     */
    private DiffAndPowerHolder parseDerivatives(List<PomTaggedExpression> following_exps, DLMFMacroInfoHolder info ) {
        DiffAndPowerHolder holder = new DiffAndPowerHolder();
        int numberOfDerivative = 0; // no differentiation by default

        // check for optional arguments
        while (!following_exps.isEmpty()) {
            PomTaggedExpression exp = following_exps.get(0);

            // if the next element is neither @, ^ nor ', we can stop already.
            if (exp.isEmpty()) {
                return holder;
            }

            MathTerm first_term = exp.getRoot();
            if (first_term != null && !first_term.isEmpty()) {
                MathTermTags tag = MathTermTags.getTagByKey(first_term.getTag());
                switch (tag) {
                    case caret:
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
                                following_exps.remove(0);
                                parseLagrangeNotation(exp.getComponents(), holder);
                            }
                        } else {
                            // found a normal power. So move it to the end
                            holder.setMoveToEnd( following_exps.remove(0) );
                        }
                        break;
                    case prime:
                        // well, just count them up
                        following_exps.remove(0);
                        numberOfDerivative++;
                        break;
                    case at: // we reached the end
                    default: // in any other case, we also reached the end...
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
                        return holder;
                }
            } else break;
        }

        return holder;
    }

    /**
     * Parses the argument of of the semantic macro
     * @param following_exps
     * @param holder
     * @return
     */
    private LinkedList<String> parseArguments(List<PomTaggedExpression> following_exps, DLMFMacroInfoHolder holder ) {
        LinkedList<String> arguments = new LinkedList<>();

        boolean passAts = false;
        boolean printedInfo = false;
        int atCounter = 0;

        for (int i = 0; !following_exps.isEmpty() && i < holder.getNumOfVars(); i++) {
            // get first expression
            PomTaggedExpression exp = following_exps.remove(0);

            // TODO well
            if ( isWronskian ) {
                extractVariableOfDiff(exp);
            }

            if (!passAts && containsTerm(exp)) {
                MathTerm term = exp.getRoot();
                MathTermTags tag = MathTermTags.getTagByKey(term.getTag());
                if ( tag.equals(MathTermTags.at) ) {
                    if ( atCounter > holder.getNumOfAts() && !printedInfo ) {
                        LOG.warn("Too many @'s in macro. This may throw an exception in future releases.");
                        printedInfo = true;
                    }
                    atCounter++;
                    i--;
                    continue;
                }
            }

            String translation = translateInnerExp(exp, following_exps).toString();
            arguments.addLast(translation);
            passAts = true;
        }

        return arguments;
    }

    /**
     *
     * @param followingExps
     * @param info
     * @return
     */
    private LinkedList<String> parseDerivArguments(List<PomTaggedExpression> followingExps, DLMFMacroInfoHolder info ){
        // there are two options here, one easy and one complex
        // first the easy, the next element is not empty:
        PomTaggedExpression next = followingExps.get(0);
        if ( !next.isEmpty() ) {
            // nothing special, just go ahead and parse it as usual
            return parseArguments(followingExps, info);
        }

        // first, get rid of the empty element
        followingExps.remove(0);

        // otherwise! we have a problem similar to sums. When does the argument ends?
        // so lets follow sums approach
        PomTaggedExpression variablePTE = followingExps.remove(0);
        TranslatedExpression varTE = translateInnerExp(variablePTE, new LinkedList<>());

        LinkedList<String> vars = new LinkedList<>();
        vars.add(varTE.toString());

        List<PomTaggedExpression> potentialArgs = LimitedTranslator.getPotentialArgumentsUntilEndOfScope(
                followingExps,
                vars,
                this
        );

        // the potential arguments is a theoretical sequence, so handle it as a sequence!
        PomTaggedExpression topSeqPTE = FakeMLPGenerator.generateEmptySequencePTE();
        for ( PomTaggedExpression pte : potentialArgs ) topSeqPTE.addComponent(pte);

        SequenceTranslator p = new SequenceTranslator(getSuperTranslator());
        TranslatedExpression translatedPotentialArguments = p.translate( topSeqPTE );

        // clean up first
        getGlobalTranslationList().removeLastNExps(translatedPotentialArguments.getLength());

        // now, search for the next argument
        TranslatedExpression transArgs =
                translatedPotentialArguments.removeUntilLastAppearanceOfVar(
                        vars,
                        getConfig().getMULTIPLY()
                );

        translatedInAdvance = translatedPotentialArguments;
        LinkedList<String> args = new LinkedList<>();
        args.add(transArgs.toString());
        args.add(vars.getFirst());
        return args;
    }

    private void extractVariableOfDiff(PomTaggedExpression exp) {
        while (!exp.isEmpty()) { // look for a macro term in the expression and infer variable of diff based on that
            if (exp.getTag() != null && exp.getTag().equals(ExpressionTags.sequence.tag())) {
                for (PomTaggedExpression expression : exp.getComponents()) {
                    if (isDLMFMacro(expression.getRoot())) {
                        exp = expression;
                    }
                }
            }
            if (isDLMFMacro(exp.getRoot())) { // found macro term
                FeatureSet fset = exp.getRoot().getNamedFeatureSet(Keys.KEY_DLMF_MACRO);
                // get variable of differentiation from variable used in dlmf expression of macro
                String dlmf_expression = DLMFFeatureValues.DLMF.getFeatureValue(fset, CAS);
                int args = Integer.parseInt(DLMFFeatureValues.variables.getFeatureValue(fset, CAS));
                int slot = 0;
                try {
                    slot = Integer.parseInt(DLMFFeatureValues.slot.getFeatureValue(fset, CAS));
                } catch (NumberFormatException e) {
                    throw throwSlotError();
                }
                String arg_extractor_single = "\\{([^{}]*)\\}";
                String arg_extractor_string = "@";
                for (int i = 0; i < args; i++) {
                    arg_extractor_string += arg_extractor_single;
                }
                Pattern arg_extractor_pattern = // capture all arguments of dlmf expression
                        Pattern.compile(arg_extractor_string);
                Matcher m = arg_extractor_pattern.matcher(dlmf_expression);
                if (m.find()) {
                    varOfDiff = m.group(slot); // extract argument that matches slot
                    return;
                } else {
                    throw throwMacroException("Unable to extract argument from " + dlmf_expression);
                }
            } else {
                exp = exp.getNextSibling();
            }
        }
    }

    /**
     * Checks weather the first element is a differentiation in lagrange notation. That means
     * the order is given in parentheses.
     *
     * @param following_exps the children of caret
     * @return
     */
    private boolean isLagrangeNotation(List<PomTaggedExpression> following_exps) {
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

    /**
     * In \<macro>^{(<order>)}@{...}, extracts the <order> as the order of differentiation for the macro
     *
     * @param following_exps
     * @param holder
     * @return
     */
    private DiffAndPowerHolder parseLagrangeNotation(List<PomTaggedExpression> following_exps, DiffAndPowerHolder holder) {
        // translate the order
        TranslatedExpression lagrangeExpr = parseGeneralExpression(following_exps.remove(0), following_exps);

        // clean up global translation list
        TranslatedExpression global = getGlobalTranslationList();
        global.removeLastNExps(lagrangeExpr.getLength());

        String diff = stripMultiParentheses(lagrangeExpr.toString());

        // update info holder
        holder.setDifferentiation(diff);
        return holder;
    }

    // Works for 2 argument Wronskians, can be expanded to more arguments
    private void splitComma(List<PomTaggedExpression> following) { // reads \Wronskian@{f1, f2} as if it were \Wronskian@{f1}{f2}
        PomTaggedExpression sequence = following.remove(1); // first element is "@"
        PomTaggedExpression firstHalf = new PomTaggedExpression();
        firstHalf.setTag(ExpressionTags.sequence.tag());
        PomTaggedExpression secondHalf = new PomTaggedExpression();
        secondHalf.setTag(ExpressionTags.sequence.tag());
        boolean passedComma = false;
        for (PomTaggedExpression exp : sequence.getComponents()) {
            MathTermTags tag = MathTermTags.getTagByKey(exp.getRoot().getTag());
            if (tag != null && tag.equals(MathTermTags.comma)) {
                passedComma = true;
                continue;
            }
            (passedComma ? secondHalf : firstHalf).addComponent(exp);
        }

        tempArgList = new LinkedList<>();
        tempArgList.add(firstHalf);
        tempArgList.add(secondHalf);
    }

    private void skipAts(List<PomTaggedExpression> following_exps) {
        // check for optional arguments
        while (!following_exps.isEmpty()) {
            PomTaggedExpression exp = following_exps.get(0);

            // if the next element is neither @, ^ nor ', we can stop already.
            if (exp.isEmpty()) return;

            MathTerm first_term = exp.getRoot();
            if (first_term != null && !first_term.isEmpty()) {
                MathTermTags tag = MathTermTags.getTagByKey(first_term.getTag());
                if ( tag.equals(MathTermTags.at) ) {
                    following_exps.remove(0); // delete it from list
                } else return;
            }
        }
    }

    private String[] createArgumentArray(
            LinkedList<String> optionalParas,
            LinkedList<String> parameters,
            LinkedList<String> arguments) {
        // create argument list
        String[] args = new String[
                optionalParas.size() + parameters.size() + arguments.size()
                ];

        for ( int i = 0; i < optionalParas.size(); i++ ) {
            args[i] = optionalParas.get(i);
        }

        for ( int i = optionalParas.size(), j = 0; i < optionalParas.size()+parameters.size(); i++, j++ ) {
            args[i] = parameters.get(j);
        }

        for ( int i = optionalParas.size()+parameters.size(), j = 0; i < args.length; i++, j++ ) {
            args[i] = arguments.get(j);
        }

        return args;
    }

    /**
     * Fills the translation pattern with arguments and adds everything to the global and local translation list.
     * @param args the arguments in right order and no null elements included
     * @param info the information about the macro that will be translated
     * @param diffHolder optional information about differentiation (might contain only null)
     * @param slotOfDifferentiation is only used if {@link DiffAndPowerHolder#differentiation} in {@param diffHolder}
     *                              is not null.
     */
    private void fillVars(
            String[] args,
            String info_key,
            DLMFMacroInfoHolder info,
            DiffAndPowerHolder diffHolder,
            int slotOfDifferentiation
    ) {
        // when the alternative mode is activated, it tries to translate
        // the alternative translation
        String pattern = (getConfig().isAlternativeMode() && !info.getAlternativePattern().isEmpty()) ?
                info.getAlternativePattern() : info.getTranslationPattern();

        // Eventually, we need to substitute an argument.
        String subbedExpression = null;
        if (slotOfDifferentiation >= 1 && diffHolder.differentiation != null) {
            // substitute out argument in slot of differentiation
            subbedExpression = args[slotOfDifferentiation - 1];
            args[slotOfDifferentiation - 1] = TEMPORARY_VARIABLE_NAME;
        }

        // if we translating a wronskian here, we need to be a bit more careful.
        if (isWronskian) { // plugs in variable of differentiation
            String[] newComponents = new String[args.length + 1];
            newComponents[0] = varOfDiff;
            for (int i = 0; i < args.length; i++) {
                newComponents[i + 1] = args[i];
            }
            args = newComponents;
        }

        // finally, fill up pattern with arguments
        LOG.debug("Fill pattern: " + pattern);
        for (int i = 0; i < args.length; i++) {
            try {
                pattern = pattern.replace(
                        GlobalConstants.POSITION_MARKER + Integer.toString(i),
                        stripMultiParentheses(args[i])
                );
            } catch (NullPointerException npe) {
                throw throwMacroException("Argument of macro seems to be missing for " + macro);
            }
        }
        LOG.debug("Translated DLMF macro to: " + pattern);

        // apply derivative and plug in the subbed out expression to replace temp during execution in CAS
        if (subbedExpression != null) {
            LOG.debug("Fill differentiation pattern for " + macro);
            BasicFunctionsTranslator bft = getConfig().getBasicFunctionsTranslator();

            String[] diffArgs = new String[]{
                    pattern,  // the argument for this pattern is the entire translation
                    subbedExpression,
                    diffHolder.differentiation
            };
            pattern = bft.translate(diffArgs, "derivative");
            LOG.debug("Translated diff: " + pattern);
        }

        // finally, update translation lists
        localTranslations.addTranslatedExpression(pattern);
        getGlobalTranslationList().addTranslatedExpression(pattern);

        // put all information to the info log
        getInfoLogger().addMacroInfo(
                info_key,
                createFurtherInformation(info)
        );
    }

    private String createFurtherInformation(DLMFMacroInfoHolder info) {
        String extraInformation = "";
        if (!info.getMeaning().isEmpty()) {
            extraInformation += info.getMeaning();
        } else if (!info.getDescription().isEmpty()) {
            extraInformation += info.getDescription();
        }

        extraInformation += "; Example: " + info.getDLMFExample() + System.lineSeparator();
        extraInformation += "Will be translated to: " + info.getTranslationPattern() + System.lineSeparator();

        if (!info.getCasComment().isEmpty()) {
            extraInformation += "Translation Information: " + info.getCasComment() + System.lineSeparator();
        }

        if (!info.getConstraints().isEmpty()) {
            extraInformation += "Constraints: " + info.getConstraints() + System.lineSeparator();
        }

        if (!info.getBranchCuts().isEmpty()) {
            extraInformation += "Branch Cuts: " + info.getBranchCuts() + System.lineSeparator();
        }

        if (!info.getCasBranchCuts().isEmpty()) {
            extraInformation += CAS + " uses other branch cuts: " + info.getCasBranchCuts()
                    + System.lineSeparator();
        }

        String TAB = getConfig().getTAB();
        String tab = TAB.substring(0, TAB.length() - ("DLMF: ").length());
        extraInformation += "Relevant links to definitions:" + System.lineSeparator() +
                "DLMF: " + tab + info.getDefDlmf() + System.lineSeparator();
        tab = TAB.substring(0,
                ((CAS + ": ").length() >= TAB.length() ?
                        0 : (TAB.length() - (CAS + ": ").length()))
        );
        extraInformation += CAS + ": " + tab + info.getDefCas();
        return extraInformation;
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

    private TranslationException throwMacroException(String message) {
        return TranslationException.buildExceptionObj(this, message, TranslationExceptionReason.DLMF_MACRO_ERROR, macro);
    }

    private class DiffAndPowerHolder {
        private String differentiation = null;
        private PomTaggedExpression moveToEnd = null;

        public DiffAndPowerHolder(){}

        public void setDifferentiation(String differentiation) {
            this.differentiation = differentiation;
        }

        public void setMoveToEnd(PomTaggedExpression moveToEnd) {
            this.moveToEnd = moveToEnd;
        }

        public String getDifferentiation() {
            return differentiation;
        }

        public PomTaggedExpression getMoveToEnd() {
            return moveToEnd;
        }
    }
}
