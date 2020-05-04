package gov.nist.drmf.interpreter.cas.translation.components.util;

import gov.nist.drmf.interpreter.common.constants.Keys;
import gov.nist.drmf.interpreter.common.exceptions.TranslationException;
import gov.nist.drmf.interpreter.common.exceptions.TranslationExceptionReason;
import gov.nist.drmf.interpreter.common.grammar.DLMFFeatureValues;
import gov.nist.drmf.interpreter.mlp.MacrosLexicon;
import mlp.FeatureSet;
import org.apache.logging.log4j.Logger;

import static org.apache.logging.log4j.LogManager.getLogger;

/**
 * @author Andre Greiner-Petter
 */
public class MacroTranslationInformation {
    private static final Logger LOG = getLogger(MacroTranslationInformation.class.getName());

    private int numOfParams, numOfAts, numOfVars;

    private String constraints;

    private String defDlmf, defCas;

    private String translationPattern, alternativePattern;

    private String branchCuts, casBranchCuts;

    public MacroTranslationInformation(FeatureSet fset, String cas) {
        // now store all additional information
        // first of all number of parameters, ats and vars
        numOfParams = Integer.parseInt(DLMFFeatureValues.params.getFeatureValue(fset, cas));
        numOfAts = Integer.parseInt(DLMFFeatureValues.ats.getFeatureValue(fset, cas));
        numOfVars = Integer.parseInt(DLMFFeatureValues.variables.getFeatureValue(fset, cas));

        // now store additional information about the translation
        // Meaning: name of the function (defined by DLMF)
        // Description: same like meaning, but more rough. Usually there is only one of them defined (meaning|descreption)
        // Constraints: of the DLMF definition
        // Branch Cuts: of the DLMF definition
        // DLMF: its the plain, smallest version of the macro. Like \JacobiP{a}{b}{c}@{d}
        //      we can reference our Constraints to a, b, c and d now. That makes it easier to read
        constraints = DLMFFeatureValues.constraints.getFeatureValue(fset, cas);
        branchCuts = DLMFFeatureValues.branch_cuts.getFeatureValue(fset, cas);

        // Translation information
        translationPattern = DLMFFeatureValues.CAS.getFeatureValue(fset, cas);
        alternativePattern = DLMFFeatureValues.CAS_Alternatives.getFeatureValue(fset, cas);
        casBranchCuts = DLMFFeatureValues.CAS_BranchCuts.getFeatureValue(fset, cas);

        // links to the definitions
        defDlmf = DLMFFeatureValues.dlmf_link.getFeatureValue(fset, cas);
        defCas = DLMFFeatureValues.CAS_Link.getFeatureValue(fset, cas);
    }

    void handleMultipleAlternativePatterns(String cas, String macro) {
        // maybe the alternative pattern got multiple alternatives
        if (!alternativePattern.isEmpty()) {
            try {
                alternativePattern = alternativePattern.split(MacrosLexicon.SIGNAL_INLINE)[0];
            } catch (Exception e) {
                throw new TranslationException(
                        Keys.KEY_LATEX,
                        cas,
                        "Cannot split alternative macro pattern!",
                        TranslationExceptionReason.DLMF_MACRO_ERROR);
            }

            if (translationPattern.isEmpty()) {
                LOG.warn("No direct translation available! Switch to alternative mode for " + macro);
                translationPattern = alternativePattern;
            }
        }
    }

    public int getNumOfParams() {
        return numOfParams;
    }

    public int getNumOfAts() {
        return numOfAts;
    }

    public int getNumOfVars() {
        return numOfVars;
    }

    public String getConstraints() {
        return constraints;
    }

    public String getDefDlmf() {
        return defDlmf;
    }

    public String getDefCas() {
        return defCas;
    }

    public String getTranslationPattern() {
        return translationPattern;
    }

    public String getAlternativePattern() {
        return alternativePattern;
    }

    public String getBranchCuts() {
        return branchCuts;
    }

    public String getCasBranchCuts() {
        return casBranchCuts;
    }
}
