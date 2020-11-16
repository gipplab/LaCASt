package gov.nist.drmf.interpreter.cas.translation.components.util;

import gov.nist.drmf.interpreter.cas.common.DLMFPatterns;
import gov.nist.drmf.interpreter.cas.common.IForwardTranslator;
import gov.nist.drmf.interpreter.common.exceptions.TranslationException;
import gov.nist.drmf.interpreter.common.exceptions.TranslationExceptionReason;
import gov.nist.drmf.interpreter.pom.common.grammar.DLMFFeatureValues;
import mlp.FeatureSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author Andre Greiner-Petter
 */
public class MacroInfoHolder {
    private static final Logger LOG = LogManager.getLogger(MacroInfoHolder.class.getName());

    private int slotOfDifferentiation = Integer.MIN_VALUE;

    private final String macro;

    private String variableOfDifferentiation = null;

    private MacroTranslationInformation translationInformation;

    private MacroMetaInformation metaInformation;

    /**
     * Store information about the macro from an feature set.
     * @param fset future set
     * @param macro the macro
     * @throws TranslationException if the feature set does not provide
     * the necessary information for a translation
     */
    public MacroInfoHolder(
            IForwardTranslator translator,
            FeatureSet fset,
            String cas,
            String macro
    ) throws TranslationException {
        this.macro = macro;
        this.checkFeatureSetValidity(translator, fset);
        this.storeInfosValidityCheck(translator, fset, cas);
    }

    private void checkFeatureSetValidity(IForwardTranslator translator, FeatureSet fset)
            throws TranslationException {
        if ( fset == null ) {
            throw TranslationException.buildExceptionObj(
                    translator, "Cannot extract information from feature set: " + macro,
                    TranslationExceptionReason.MISSING_TRANSLATION_INFORMATION,
                    macro);
        }
    }

    private void storeInfosValidityCheck(IForwardTranslator translator, FeatureSet fset, String cas)
            throws TranslationException {
        // try to extract the information
        try {
            this.storeInfos(fset, cas);
            if (this.translationInformation.hasNoTranslations()) {
                throw TranslationException.buildExceptionObj(
                        translator, "There are no translation patterns available for: " + macro,
                        TranslationExceptionReason.MISSING_TRANSLATION_INFORMATION, macro);
            }
        } catch (NullPointerException | TranslationException npe) {
            throw TranslationException.buildExceptionObj(
                    translator, "Cannot extract information from feature set: " + macro,
                    TranslationExceptionReason.MISSING_TRANSLATION_INFORMATION, macro);
        }
    }

    /**
     * Analyzes and extracts all information from a given feature set of a DLMF macro.
     * @param fset the feature set
     */
    private void storeInfos(FeatureSet fset, String cas) {
        this.translationInformation = new MacroTranslationInformation(fset, cas);

        try { // true slot is argument slot + numOfParams
            slotOfDifferentiation = Integer.parseInt(DLMFFeatureValues.SLOT_DERIVATIVE.getFeatureValue(fset, cas))
                    + translationInformation.getNumOfParams();
        } catch (NumberFormatException e) {
            LOG.debug("Cannot extract slot of differentiation for " + macro);
        }

        metaInformation = new MacroMetaInformation(fset, cas);
    }

    public boolean isWronskian() {
        return macro.equals("\\Wronskian");
    }

    public boolean isDeriv() {
        return macro.matches(DLMFPatterns.DERIV_NOTATION);
    }

    public String getMacro() {
        return macro;
    }

    public String getVariableOfDifferentiation() {
        return variableOfDifferentiation;
    }

    public void setVariableOfDifferentiation(String variableOfDifferentiation) {
        this.variableOfDifferentiation = variableOfDifferentiation;
    }

    public int getNumberOfArguments() {
        return translationInformation.getNumOfOptionalParas() +
                translationInformation.getNumOfParams() +
                translationInformation.getNumOfVars();
    }

    public boolean hasNoArguments() {
        return getNumberOfArguments() + translationInformation.getNumOfAts() == 0;
    }

    public int getSlotOfDifferentiation() {
        return slotOfDifferentiation;
    }

    public void overwriteSlotOfDifferentiation(int slot) {
        this.slotOfDifferentiation = slot;
    }

    public MacroTranslationInformation getTranslationInformation() {
        return translationInformation;
    }

    public MacroMetaInformation getMetaInformation() {
        return metaInformation;
    }
}
