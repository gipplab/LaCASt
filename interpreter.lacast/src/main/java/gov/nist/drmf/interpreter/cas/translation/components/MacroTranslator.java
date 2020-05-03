package gov.nist.drmf.interpreter.cas.translation.components;

import gov.nist.drmf.interpreter.cas.common.DLMFPatterns;
import gov.nist.drmf.interpreter.cas.logging.TranslatedExpression;
import gov.nist.drmf.interpreter.cas.translation.AbstractListTranslator;
import gov.nist.drmf.interpreter.cas.translation.AbstractTranslator;
import gov.nist.drmf.interpreter.cas.translation.components.util.DerivativeAndPowerHolder;
import gov.nist.drmf.interpreter.cas.translation.components.util.MacroInfoHolder;
import gov.nist.drmf.interpreter.common.InformationLogger;
import gov.nist.drmf.interpreter.common.constants.GlobalConstants;
import gov.nist.drmf.interpreter.common.constants.Keys;
import gov.nist.drmf.interpreter.common.exceptions.TranslationException;
import gov.nist.drmf.interpreter.common.exceptions.TranslationExceptionReason;
import gov.nist.drmf.interpreter.common.grammar.DLMFFeatureValues;
import gov.nist.drmf.interpreter.common.grammar.ExpressionTags;
import gov.nist.drmf.interpreter.common.grammar.MathTermTags;
import gov.nist.drmf.interpreter.common.symbols.BasicFunctionsTranslator;
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

    private final TranslatedExpression localTranslations;

    private String macro;

    private boolean isWronskian = false;
    private boolean isDeriv = false;

    private List<PomTaggedExpression> tempArgList;
    private String varOfDiff;

    public MacroTranslator(AbstractTranslator superTranslator) {
        super(superTranslator);
        this.localTranslations = new TranslatedExpression();
        this.CAS = getConfig().getTO_LANGUAGE();
        this.tempArgList = new LinkedList<>();
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
        MacroInfoHolder info = new MacroInfoHolder(this, fset, CAS, macro);

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
            info = new MacroInfoHolder(this, fset, CAS, macro_term.getTermText());
        }

        // in case there are parameters, we parse them first
        // empty if none
        LinkedList<String> parameters = parseParameters(following_exps, info.getNumOfParams());

        // parse derivatives, lagrange notation or primes
        MacroDerivativesTranslator derivativesTranslator = new MacroDerivativesTranslator(this);
        DerivativeAndPowerHolder diffPowerHolder = derivativesTranslator.parseDerivatives(following_exps, info);

        LinkedList<String> arguments;
        if ( isWronskian ) arguments = parseArguments(tempArgList, info);
        else if ( isDeriv ) arguments = derivativesTranslator.parseDerivativeArguments(following_exps, info);
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
        if (diffPowerHolder.getMoveToEnd() != null) {
            following_exps.add(0, diffPowerHolder.getMoveToEnd());
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
        if ( isDeriv && derivativesTranslator.hasTranslatedInAdvancedComponent() ) {
            TranslatedExpression translatedInAdvance = derivativesTranslator.getTranslatedInAdvanceComponent();
            localTranslations.addTranslatedExpression(translatedInAdvance);
            getGlobalTranslationList().addTranslatedExpression(translatedInAdvance);

            // just in case, reset the variable
            derivativesTranslator.resetTranslatedInAdvancedComponent();
        }

        // now we are done
        return localTranslations;
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
     * Parses the argument of of the semantic macro
     * @param following_exps .
     * @param holder .
     * @return .
     */
    protected LinkedList<String> parseArguments(List<PomTaggedExpression> following_exps, MacroInfoHolder holder ) {
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
                int slot;
                try {
                    slot = Integer.parseInt(DLMFFeatureValues.slot.getFeatureValue(fset, CAS));
                } catch (NumberFormatException e) {
                    throw throwSlotError();
                }
                String arg_extractor_single = "\\{([^{}]*)}";
                Pattern arg_extractor_pattern = // capture all arguments of dlmf expression
                        Pattern.compile(
                                "@" + arg_extractor_single.repeat(Math.max(0, args)) // capture all arguments of dlmf expression
                        );
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
     * @param slotOfDifferentiation is only used if {@link DerivativeAndPowerHolder#getDifferentiation()} in {@param diffHolder}
     *                              is not null.
     */
    private void fillVars(
            String[] args,
            String info_key,
            MacroInfoHolder info,
            DerivativeAndPowerHolder diffHolder,
            int slotOfDifferentiation
    ) {
        // when the alternative mode is activated, it tries to translate
        // the alternative translation
        String pattern = (getConfig().isAlternativeMode() && !info.getAlternativePattern().isEmpty()) ?
                info.getAlternativePattern() : info.getTranslationPattern();

        // Eventually, we need to substitute an argument.
        String subbedExpression = null;
        if (slotOfDifferentiation >= 1 && diffHolder.getDifferentiation() != null) {
            // substitute out argument in slot of differentiation
            subbedExpression = args[slotOfDifferentiation - 1];
            args[slotOfDifferentiation - 1] = TEMPORARY_VARIABLE_NAME;
        }

        // if we translating a wronskian here, we need to be a bit more careful.
        if (isWronskian) { // plugs in variable of differentiation
            String[] newComponents = new String[args.length + 1];
            newComponents[0] = varOfDiff;
            System.arraycopy(args, 0, newComponents, 1, args.length);
            args = newComponents;
        }

        // finally, fill up pattern with arguments
        LOG.debug("Fill pattern: " + pattern);
        for (int i = 0; i < args.length; i++) {
            try {
                pattern = pattern.replace(
                        GlobalConstants.POSITION_MARKER + i,
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
                    diffHolder.getDifferentiation()
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

    private String createFurtherInformation(MacroInfoHolder info) {
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

    protected TranslationException throwMacroException(String message) {
        return TranslationException.buildExceptionObj(this, message, TranslationExceptionReason.DLMF_MACRO_ERROR, macro);
    }
}
