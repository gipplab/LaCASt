package gov.nist.drmf.interpreter.cas.translation.components;

import gov.nist.drmf.interpreter.cas.common.DLMFPatterns;
import gov.nist.drmf.interpreter.cas.logging.TranslatedExpression;
import gov.nist.drmf.interpreter.cas.translation.AbstractListTranslator;
import gov.nist.drmf.interpreter.cas.translation.AbstractTranslator;
import gov.nist.drmf.interpreter.cas.translation.components.util.*;
import gov.nist.drmf.interpreter.common.InformationLogger;
import gov.nist.drmf.interpreter.common.constants.Keys;
import gov.nist.drmf.interpreter.common.exceptions.TranslationException;
import gov.nist.drmf.interpreter.common.exceptions.TranslationExceptionReason;
import gov.nist.drmf.interpreter.common.grammar.MathTermTags;
import gov.nist.drmf.interpreter.mlp.MathTermUtility;
import mlp.FeatureSet;
import mlp.MathTerm;
import mlp.PomTaggedExpression;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    private static final Pattern OPTIONAL_PARAMS_PATTERN =
            Pattern.compile("\\s*\\[(.*)]\\s*\\*?\\s*");

//    private static final Pattern leibniz_notation_pattern =
//            Pattern.compile("\\s*\\(([^@]*)\\)\\s*");

    private final String cas;

    private final TranslatedExpression localTranslations;

    private String macro;

    private boolean isDeriv = false;

    public MacroTranslator(AbstractTranslator superTranslator) {
        super(superTranslator);
        this.localTranslations = new TranslatedExpression();
        this.cas = getConfig().getTO_LANGUAGE();
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

        isDeriv = mt.getTermText().matches(DLMFPatterns.DERIV_NOTATION);
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
     * @param followingExps following expressions of the DLMF macro
     * @return true if the translation was successful, otherwise false
     */
    private TranslatedExpression parse(PomTaggedExpression exp, List<PomTaggedExpression> followingExps) {
        MathTerm macroTerm = exp.getRoot();
        this.macro = macroTerm.getTermText();

        FeatureSet fset = null;
        MacroInfoHolder info = null;
        TranslationException translationException = null;
        try {
            // ok first, get the feature set!
            fset = macroTerm.getNamedFeatureSet(Keys.KEY_DLMF_MACRO);
            info = new MacroInfoHolder(this, fset, cas, macro);

            // first, lets check if this function has no arguments (single symbol)
            if (info.hasNoArguments()) {
                return parseNoArgumentMacro(info);
            }
        } catch (TranslationException te) {
            // if there are no translation information available, we may want to check optional parameters first
            // the reason is simple, there might be no translations for the macro without optional parameters
            // but maybe there are translations with optional parameters
            translationException = te;
        }

        // now check for optional parameters, if any
        LinkedList<String> optionalParas = parseOptionalParameters(followingExps);

        // in case of optional arguments, we have to retrieve other information from the lexicons
        if (!optionalParas.isEmpty()) {
            fset = macroTerm.getNamedFeatureSet(Keys.KEY_DLMF_MACRO_OPTIONAL_PREFIX + optionalParas.size());
            info = new MacroInfoHolder(this, fset, cas, macro);
        } else if ( translationException != null ) {
            // if there are no optional parameters AND previously we caught an exception, its time to throw it now
            throw translationException;
        }

        // until now, everything parsed was optional, start the general parsing process
        return parse(followingExps, info, optionalParas);
    }

    /**
     * The general parsing process, assumes optional parameters were parsed before and the information
     * holder is final.
     * @param followingExps the following expression
     * @param info the information holder
     * @param optionalParas the optional parameters (empty if there are non)
     * @return the translated expression
     */
    private TranslatedExpression parse(
            List<PomTaggedExpression> followingExps,
            MacroInfoHolder info,
            LinkedList<String> optionalParas
    ) {
        // in case there are parameters, we parse them first
        // empty if none
        LinkedList<String> parameters = parseParameters(followingExps, info.getTranslationInformation().getNumOfParams());

        // parse derivatives, lagrange notation or primes
        MacroDerivativesTranslator derivativesTranslator = new MacroDerivativesTranslator(this);
        DerivativeAndPowerHolder diffPowerHolder = derivativesTranslator.parseDerivatives(followingExps, info);

        LinkedList<String> arguments = handleArguments(followingExps, info, derivativesTranslator);

        // log information
        String infoKey = macro;
        if (!optionalParas.isEmpty()) {
            infoKey += optionalParas.size();
        }

        PatternFiller patternFiller = getPatternFiller(info, diffPowerHolder, optionalParas);
        String[] args = createArgumentArray(optionalParas, parameters, arguments);

        // in case we cached a power, it moves to the end. Let's fake this by
        // adding this cached expression back to the start of the remaining
        // expressions
        if (diffPowerHolder.getMoveToEnd() != null) {
            followingExps.add(0, diffPowerHolder.getMoveToEnd());
        }

        // finally fill the placeholders by values
        fillVars(args, patternFiller);
        postTranslation(infoKey, info, derivativesTranslator);

        // now we are done
        return localTranslations;
    }

    private void postTranslation(String infoKey, MacroInfoHolder info, MacroDerivativesTranslator derivativesTranslator) {
        // put all information to the info log
        getInfoLogger().addMacroInfo(
                infoKey,
                createFurtherInformation(info)
        );

        // in case we translated an expression in advance, we need to fill up the translation lists
        if ( isDeriv && derivativesTranslator.hasTranslatedInAdvancedComponent() ) {
            TranslatedExpression translatedInAdvance = derivativesTranslator.getTranslatedInAdvanceComponent();
            localTranslations.addTranslatedExpression(translatedInAdvance);
            getGlobalTranslationList().addTranslatedExpression(translatedInAdvance);

            // just in case, reset the variable
            derivativesTranslator.resetTranslatedInAdvancedComponent();
        }

        if ( info.getTranslationInformation().requirePackages() ) {
            localTranslations.addRequiredPackages(info.getTranslationInformation().getRequiredPackages());
            getGlobalTranslationList().addRequiredPackages(info.getTranslationInformation().getRequiredPackages());
        }
    }

    private PatternFiller getPatternFiller(
            MacroInfoHolder info,
            DerivativeAndPowerHolder diffPowerHolder,
            List<String> optionalParas
    ) {
        try {
            return new PatternFiller(info, diffPowerHolder, optionalParas);
        } catch (IndexOutOfBoundsException ioobe ) {
            String errorMsg = "No slot of differentiation available for " + macro + ", " +
                    "but found differentiation notation " + diffPowerHolder.getDifferentiation();
            throw throwMacroException(errorMsg);
        }
    }

    private TranslatedExpression parseNoArgumentMacro(MacroInfoHolder macroInfo) {
        // inform about the translation decision
        super.getInfoLogger().addMacroInfo(
                macroInfo.getMacro(),
                createFurtherInformation(macroInfo)
        );

        MacroTranslationInformation info = macroInfo.getTranslationInformation();
        // just add the translated representation extracted from feature set
        localTranslations.addTranslatedExpression(info.getTranslationPattern());
        super.getGlobalTranslationList()
                .addTranslatedExpression(info.getTranslationPattern());

        // done
        return localTranslations;
    }

    /**
     * This function checks if the very next arguments are
     * 1) optional parameters (indicated by [])
     * <p>
     * This function manipulates the argument in case of optional arguments or carets!
     *
     * @param followingExps the expressions right after the macro itself
     */
    private LinkedList<String> parseOptionalParameters(List<PomTaggedExpression> followingExps) {
        LinkedList<String> optionalArguments = new LinkedList<>();

        // check for optional arguments
        boolean continueParsing = true;
        while (!followingExps.isEmpty() && continueParsing) {
            PomTaggedExpression first = followingExps.get(0);

            // if the next one is empty expression, it cannot be a prime, or caret, or [
            if (first.isEmpty()) {
                break;
            }

            continueParsing = continueOptionalParameterParsing(first, followingExps, optionalArguments);
        }

        return optionalArguments;
    }

    private boolean continueOptionalParameterParsing(
            PomTaggedExpression first,
            List<PomTaggedExpression> followingExps,
            List<String> optionalArguments
    ) {
        if ( MathTermUtility.equals(first.getRoot(), MathTermTags.left_bracket) ) {
            String optional = translateInnerExp(followingExps.remove(0), followingExps).toString();
            Matcher m = OPTIONAL_PARAMS_PATTERN.matcher(optional);
            if (m.matches()) {
                optionalArguments.add(m.group(1));
            } else {
                optionalArguments.add(optional);
            }
            // if there were one optional argument, there might be others also
            return true;
        }
        return false;
    }

    private LinkedList<String> parseParameters(List<PomTaggedExpression> followingExps, int numberOfParameters) {
        LinkedList<String> parameters = new LinkedList<>();
        for (int i = 0; i < numberOfParameters && !followingExps.isEmpty(); i++) {
            PomTaggedExpression exp = followingExps.remove(0);

            // check if that's valid notation
            MathTermTags tag = MathTermTags.getTagByKey(exp.getTag());
            if ( tag != null ) {
                switch (tag) {
                    case prime:
                    case caret:
                        throw TranslationException.buildException(this, "Prime and carets are not allowed before parameters!",
                                TranslationExceptionReason.INVALID_LATEX_INPUT);
                }
            }

            // ok, everything's valid, lets move on
            String translatedPara = translateInnerExp(exp, followingExps).toString();
            parameters.addLast(translatedPara);
        }

        return parameters;
    }

    private LinkedList<String> handleArguments(
            List<PomTaggedExpression> followingExps,
            MacroInfoHolder info,
            MacroDerivativesTranslator derivativesTranslator
    ) {
        // in case of derivatives, they come prior to the @ symbols and arguments
        if ( isDeriv ) {
            return derivativesTranslator.parseDerivativeArguments(followingExps, info);
        }

        skipAts(followingExps, info);

        if ( info.isWronskian() ) {
            // in the case of wronskian, there is only one argument following (must be a sequence)
            PomTaggedExpression wronskianComponent = followingExps.remove(0);
            followingExps = splitSequenceAtComma(wronskianComponent);
            String derivVariable = derivativesTranslator.extractVariableOfDifferentiation(followingExps);
            info.setVariableOfDifferentiation(derivVariable);
        }

        return parseArguments(followingExps, info);
    }

    /**
     * Parses the arguments of the semantic macro. Note that optional parameters,
     * parameters, derivatives and at symbols has to be managed prior to this method.
     * @param followingExps first element should be also the first argument (no ats, optional parameter or
     *                      something like this).
     * @param holder the information of the current macro
     * @return the list of parsed arguments
     */
    protected LinkedList<String> parseArguments(
            List<PomTaggedExpression> followingExps,
            MacroInfoHolder holder
    ) {
        LinkedList<String> arguments = new LinkedList<>();

        for (int i = 0; !followingExps.isEmpty() && i < holder.getTranslationInformation().getNumOfVars(); i++) {
            // get first expression
            PomTaggedExpression exp = followingExps.remove(0);
            String translation = translateInnerExp(exp, followingExps).toString();
            arguments.addLast(translation);
        }

        return arguments;
    }

    /**
     * Skips the leading @ symbols in {@param followingExps} and prints a warning
     * if there are too many for the given macro.
     * @param followingExps following expressions
     * @param holder information holder
     */
    private void skipAts(List<PomTaggedExpression> followingExps, MacroInfoHolder holder) {
        // check for optional arguments
        int atCounter = 0;
        boolean printedInfo = false;
        while (!followingExps.isEmpty()) {
            PomTaggedExpression exp = followingExps.get(0);
            if (MathTermUtility.equals(exp.getRoot(), MathTermTags.at)) {
                if (atCounter > holder.getTranslationInformation().getNumOfAts() && !printedInfo) {
                    LOG.warn("Too many @'s in macro. This may throw an exception in future releases.");
                    printedInfo = true;
                }
                atCounter++;
                followingExps.remove(0);
            } else return;
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
     * @param patternFiller the object that fills the macro pattern with information
     */
    private void fillVars(
            String[] args,
            PatternFiller patternFiller
    ) {
        try {
            String pattern = patternFiller.fillPatternWithComponents(getConfig(), args);

            // finally, update translation lists
            localTranslations.addTranslatedExpression(pattern);
            getGlobalTranslationList().addTranslatedExpression(pattern);
        } catch (NullPointerException npe) {
            throw throwMacroException("Argument of macro seems to be missing for " + macro);
        }
    }

    private String createFurtherInformation(MacroInfoHolder info) {
        MacroMetaInformation metaInfo = info.getMetaInformation();
        MacroTranslationInformation translationInfo = info.getTranslationInformation();
        String extraInformation = metaInfo.getMeaningDescriptionString();

        extraInformation += "; Example: " + metaInfo.getExample() + System.lineSeparator();
        extraInformation += "Will be translated to: " + translationInfo.getTranslationPattern() + System.lineSeparator();

        if (!metaInfo.getCasComment().isEmpty()) {
            extraInformation += "Translation Information: " + metaInfo.getCasComment() + System.lineSeparator();
        }

        StringBuilder sb = new StringBuilder(extraInformation);
        translationInfo.appendNonEssentialInfo(sb, cas);

        String generalTab = getConfig().getTAB();
        String currTab = generalTab.substring(0, generalTab.length() - ("DLMF: ").length());
        sb.append("Relevant links to definitions:").append(System.lineSeparator());
        sb.append("DLMF: ").append(currTab).append(translationInfo.getDefDlmf()).append(System.lineSeparator());
        currTab = generalTab.substring(0,
                ((cas + ": ").length() >= generalTab.length() ?
                        0 : (generalTab.length() - (cas + ": ").length()))
        );
        sb.append(cas).append(": ").append(currTab).append(translationInfo.getDefCas());
        return sb.toString();
    }

    protected TranslationException throwMacroException(String message) {
        return TranslationException.buildExceptionObj(this, message, TranslationExceptionReason.DLMF_MACRO_ERROR, macro);
    }
}
