package gov.nist.drmf.interpreter.cas.translation.components.util;

import gov.nist.drmf.interpreter.common.grammar.DLMFFeatureValues;
import mlp.FeatureSet;

/**
 * @author Andre Greiner-Petter
 */
public class MacroMetaInformation {
    private final String example;
    private final String description;
    private final String meaning;
    private final String casComment;

    public MacroMetaInformation(
            FeatureSet fset,
            String cas
    ) {
        // all meta information
        meaning = DLMFFeatureValues.meaning.getFeatureValue(fset, cas);
        description = DLMFFeatureValues.description.getFeatureValue(fset, cas);
        example = DLMFFeatureValues.DLMF.getFeatureValue(fset, cas);
        casComment = DLMFFeatureValues.CAS_Comment.getFeatureValue(fset, cas);
    }

    public String getExample() {
        return example;
    }

    public String getDescription() {
        return description;
    }

    public String getMeaning() {
        return meaning;
    }

    public String getCasComment() {
        return casComment;
    }

    public String getMeaningDescriptionString() {
        String extraInformation = "";
        if (!getMeaning().isEmpty()) {
            extraInformation += getMeaning();
        } else if (!getDescription().isEmpty()) {
            extraInformation += getDescription();
        }
        return extraInformation;
    }
}
