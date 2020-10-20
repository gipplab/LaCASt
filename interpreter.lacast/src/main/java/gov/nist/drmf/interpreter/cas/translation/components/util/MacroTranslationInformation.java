package gov.nist.drmf.interpreter.cas.translation.components.util;

import gov.nist.drmf.interpreter.common.constants.Keys;
import gov.nist.drmf.interpreter.common.exceptions.TranslationException;
import gov.nist.drmf.interpreter.common.exceptions.TranslationExceptionReason;
import gov.nist.drmf.interpreter.common.grammar.DLMFFeatureValues;
import gov.nist.drmf.interpreter.mlp.MacrosLexicon;
import mlp.FeatureSet;
import org.apache.logging.log4j.Logger;

import java.util.Set;
import java.util.SortedSet;

import static org.apache.logging.log4j.LogManager.getLogger;

/**
 * @author Andre Greiner-Petter
 */
public class MacroTranslationInformation {
    private static final Logger LOG = getLogger(MacroTranslationInformation.class.getName());

    private int numOfOptionalParas, numOfParams, numOfAts, numOfVars;

    private String constraints;

    private String defDlmf, defCas;

    private String translationPattern;
    private SortedSet<String> alternativePattern;

    private String branchCuts, casBranchCuts;

    private Set<String> requiredPackages;

    public MacroTranslationInformation(FeatureSet fset, String cas) {
        // now store all additional information
        // first of all number of parameters, ats and vars
        String optParaString = DLMFFeatureValues.NUMBER_OF_OPTIONAL_PARAMETERS.getFeatureValue(fset, cas);
        numOfOptionalParas = optParaString.isBlank() ? 0 : Integer.parseInt(optParaString);
        numOfParams = Integer.parseInt(DLMFFeatureValues.NUMBER_OF_PARAMETERS.getFeatureValue(fset, cas));
        numOfAts = Integer.parseInt(DLMFFeatureValues.NUMBER_OF_ATS.getFeatureValue(fset, cas));
        numOfVars = Integer.parseInt(DLMFFeatureValues.NUMBER_OF_VARIABLES.getFeatureValue(fset, cas));

        // now store additional information about the translation
        // Meaning: name of the function (defined by DLMF)
        // Description: same like meaning, but more rough. Usually there is only one of them defined (meaning|descreption)
        // Constraints: of the DLMF definition
        // Branch Cuts: of the DLMF definition
        // DLMF: its the plain, smallest version of the macro. Like \JacobiP{a}{b}{c}@{d}
        //      we can reference our Constraints to a, b, c and d now. That makes it easier to read
        constraints = DLMFFeatureValues.CONSTRAINTS.getFeatureValue(fset, cas);
        branchCuts = DLMFFeatureValues.BRANCH_CUTS.getFeatureValue(fset, cas);

        // Translation information
        translationPattern = DLMFFeatureValues.CAS_TRANSLATIONS.getFeatureValue(fset, cas);
        alternativePattern = DLMFFeatureValues.CAS_TRANSLATION_ALTERNATIVES.getFeatureSet(fset, cas);
        casBranchCuts = DLMFFeatureValues.CAS_BRANCH_CUTS.getFeatureValue(fset, cas);

        // links to the definitions
        defDlmf = DLMFFeatureValues.DLMF_LINK.getFeatureValue(fset, cas);
        defCas = DLMFFeatureValues.CAS_HYPERLINK.getFeatureValue(fset, cas);

        requiredPackages = DLMFFeatureValues.REQUIRED_PACKAGES.getFeatureSet(fset, cas);
    }

    public void setNumOfOptionalParas( int numOptionalParas ) {
        this.numOfOptionalParas = numOptionalParas;
    }

    public int getNumOfOptionalParas() {
        return numOfOptionalParas;
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

    public SortedSet<String> getAlternativePattern() {
        return alternativePattern;
    }

    public String getBranchCuts() {
        return branchCuts;
    }

    public String getCasBranchCuts() {
        return casBranchCuts;
    }

    public Set<String> getRequiredPackages() {
        return requiredPackages;
    }

    public boolean requirePackages() {
        return requiredPackages != null && !requiredPackages.isEmpty();
    }

    public boolean hasNoTranslations() {
        return (translationPattern == null || translationPattern.isBlank()) &&
                (alternativePattern == null || alternativePattern.isEmpty());
    }

    public void appendNonEssentialInfo(StringBuilder sb, String cas) {
        if ( requiredPackages != null && !requiredPackages.isEmpty() ) {
            sb.append("Required Packages: ").append(requiredPackages).append(System.lineSeparator());
        }

        if ( alternativePattern != null && alternativePattern.size() > 0 ) {
            sb.append("Alternative translations: ").append(alternativePattern);
        }

        if (!getConstraints().isEmpty()) {
            sb.append("Constraints: ").append(getConstraints()).append(System.lineSeparator());
        }

        if (!getBranchCuts().isEmpty()) {
            sb.append("Branch Cuts: ").append(getBranchCuts()).append(System.lineSeparator());
        }

        if (!getCasBranchCuts().isEmpty()) {
            sb.append(cas).append(" uses other branch cuts: ").append(getCasBranchCuts()).append(System.lineSeparator());
        }
    }
}
