package gov.nist.drmf.interpreter.cas.translation.components.util;

import gov.nist.drmf.interpreter.cas.common.DLMFPatterns;
import gov.nist.drmf.interpreter.cas.common.ForwardTranslationProcessConfig;
import gov.nist.drmf.interpreter.cas.translation.AbstractListTranslator;
import gov.nist.drmf.interpreter.common.constants.GlobalConstants;
import gov.nist.drmf.interpreter.common.symbols.BasicFunctionsTranslator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.SortedSet;

/**
 * This class fills a translation pattern by the provided information.
 * @author Andre Greiner-Petter
 */
public class PatternFiller {
    private static final Logger LOG = LogManager.getLogger(PatternFiller.class.getName());

    private static final boolean DISMISS_SUBSTITUTION_MODE = true;

    private final MacroInfoHolder macroInfo;
    private final DerivativeAndPowerHolder derivInfo;

    private final String tempVariableString;

    private int slotOfDifferentiation;

    public PatternFiller(
            MacroInfoHolder macroInfo,
            DerivativeAndPowerHolder derivInfo,
            List<String> optionalParameters,
            String tempVariableString
    ) throws IndexOutOfBoundsException {
        this.macroInfo = macroInfo;
        this.derivInfo = derivInfo;
        this.tempVariableString = tempVariableString;

        slotOfDifferentiation = macroInfo.getSlotOfDifferentiation();

        if ( slotOfDifferentiation < 1 && derivInfo.getDifferentiation() != null ) {
            throw new IndexOutOfBoundsException("Cannot access slot of differentiation.");
        }

        // if we found optional parameter, the slot of differentiation changes
        // TODO check lexicon entries if that's actually necessary
        if (!optionalParameters.isEmpty()) {
            slotOfDifferentiation += optionalParameters.size();
        }
    }

    public String fillPatternWithComponents(
            ForwardTranslationProcessConfig config,
            String[] args
    ) throws NullPointerException {
        // when the alternative mode is activated, it tries to translate
        // the alternative translation
        MacroTranslationInformation translationInformation = macroInfo.getTranslationInformation();
        String pattern = getTranslationPattern(translationInformation);

        // Maybe, we need to substitute an argument.
        String subbedExpression = null;
        if ( triggerSubstitution(!DISMISS_SUBSTITUTION_MODE) ) {
            // substitute out argument in slot of differentiation
            subbedExpression = args[slotOfDifferentiation - 1];
            args[slotOfDifferentiation - 1] = this.tempVariableString;
        } else if ( derivInfo.isComplexDerivativeVar() && macroInfo.isDeriv() ) {
            args = updateArgumentsForSubstitution(args);
            subbedExpression = args[2];
            pattern = args[1].replaceAll(
                    "\\Q"+args[2]+"\\E",
                    this.tempVariableString
            );
            args[2] = this.tempVariableString;
        }

        // if we translating a wronskian here, we need to be a bit more careful.
        if (macroInfo.isWronskian()) { // plugs in variable of differentiation
            args = getArgumentsOfWronskian(args);
        }

        // finally, fill up pattern with arguments
        LOG.debug("Fill pattern: " + pattern);
        pattern = fillPattern(args, pattern);
        LOG.debug("Translated DLMF macro to: " + pattern);

        // apply derivative and plug in the subbed out expression to replace temp during execution in CAS
        return postPatternProcessing(pattern, subbedExpression, config, args);
    }

    private String postPatternProcessing(
            String pattern,
            String subbedExpression,
            ForwardTranslationProcessConfig config,
            String[] args
    ) {
        if (subbedExpression != null) {
            return fixSubstitution(config, pattern, subbedExpression);
        } else if ( triggerSubstitution(DISMISS_SUBSTITUTION_MODE) ) {
            return simpleDerivative(config, pattern, args[slotOfDifferentiation - 1]);
        } else return pattern;
    }

    private boolean triggerSubstitution(boolean dismissComplexArguments) {
        return slotOfDifferentiation >= 1 &&
                derivInfo.getDifferentiation() != null &&
                (dismissComplexArguments != derivInfo.isComplexDerivativeVar());
    }

    private String[] updateArgumentsForSubstitution(String[] args) {
        if ( args.length == 2 ) {
            args = new String[]{"1", args[0], args[1]};
        }
        derivInfo.setDifferentiation(args[0]);
        return args;
    }

    private String getTranslationPattern(MacroTranslationInformation translationInformation) {
        String pattern = translationInformation.getTranslationPattern();
        if ( pattern == null || pattern.isEmpty() ) {
            LOG.debug("No direct translation available, switch to alternative mode.");
            SortedSet<String> alts = translationInformation.getAlternativePattern();
            if ( alts.size() > 1 )
                LOG.debug("Found multiple alternative translations. We choose first. " +
                        "Check translation information for other options");
            pattern = alts.first();
        }
        return pattern;
    }

    private String[] getArgumentsOfWronskian(String[] args) {
        String[] newComponents = new String[args.length + 1];
        newComponents[0] = macroInfo.getVariableOfDifferentiation();
        System.arraycopy(args, 0, newComponents, 1, args.length);
        return newComponents;
    }

    private String fillPattern(String[] args, String pattern) {
        for (int i = 0; i < args.length; i++) {
            pattern = pattern.replace(
                    GlobalConstants.POSITION_MARKER + i,
                    AbstractListTranslator.stripMultiParentheses(args[i])
            );
        }
        return pattern;
    }

    private String simpleDerivative(
            ForwardTranslationProcessConfig config,
            String translatedExpression,
            String variable
    ) {
        LOG.debug("Fill differentiation pattern for " + macroInfo.getMacro());
        BasicFunctionsTranslator bft = config.getBasicFunctionsTranslator();

        String[] diffArgs = new String[]{
                translatedExpression,
                variable,
                derivInfo.getDifferentiation()
        };

        String translation = bft.translate(diffArgs, DLMFPatterns.DERIVATIVE_SIMPLE_MLP_KEY);
        LOG.debug("Translated diff: " + translation);
        return translation;
    }

    private String fixSubstitution(ForwardTranslationProcessConfig config, String pattern, String subbedExpression) {
        LOG.debug("Fill differentiation pattern for " + macroInfo.getMacro());
        BasicFunctionsTranslator bft = config.getBasicFunctionsTranslator();

        String[] diffArgs = new String[]{
                pattern,  // the argument for this pattern is the entire translation
                subbedExpression,
                derivInfo.getDifferentiation()
        };
        pattern = bft.translate(diffArgs, DLMFPatterns.DERIVATIVE_SUB_MLP_KEY);
        LOG.debug("Translated diff: " + pattern);
        return pattern;
    }
}
