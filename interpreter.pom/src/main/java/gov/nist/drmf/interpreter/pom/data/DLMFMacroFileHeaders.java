package gov.nist.drmf.interpreter.pom.data;

import gov.nist.drmf.interpreter.common.constants.Keys;
import gov.nist.drmf.interpreter.pom.LineAnalyzer;
import mlp.FeatureSet;

import java.util.Arrays;
import java.util.stream.Stream;

public enum DLMFMacroFileHeaders implements FeatureFiller {
    DLMF(true, Keys.KEY_DLMF),
    LINK(false, Keys.KEY_DLMF + Keys.KEY_LINK_SUFFIX),
    MEANING(false, Keys.FEATURE_MEANINGS),
    PARAMETERS(true, Keys.NUM_OF_PARAMS),
    ATS(true, Keys.NUM_OF_ATS),
    VARIABLES(true, Keys.NUM_OF_VARS),
    SLOT_OF_DIFF(false, Keys.SLOT_OF_DIFF),
    CONSTRAINTS(false, Keys.FEATURE_CONSTRAINTS),
    BRANCH_CUT(false, Keys.FEATURE_BRANCH_CUTS),
    ROLE(false, Keys.FEATURE_ROLE);

    private final boolean mandatory;
    private final String key;

    DLMFMacroFileHeaders(boolean mandatory, String key) {
        this.mandatory = mandatory;
        this.key = key;
    }

    @Override
    public String getKey(String cas) {
        return key;
    }

    @Override
    public boolean isMandatory() {
        return mandatory;
    }

    @Override
    public String getValue(String cas, LineAnalyzer lineAnalyzer) throws NullPointerException {
        String key = getKey(cas);
        String value = lineAnalyzer.getValue(key);
        if (LINK.equals(this) && value.startsWith("http://")) value = value.substring("http://".length());
        if ( isMandatory() && (value.isEmpty()) ) {
            throw new NullPointerException(this + " is a mandatory value which is missing");
        } else return value;
    }

    public static void fillFeatureSet(
            FeatureSet featureSet,
            LineAnalyzer lineAnalyzer
    ) {
        DLMF.fillFeatureSet(featureSet, lineAnalyzer, "");
    }

    @Override
    public Stream<FeatureFiller> allValues() {
        return Arrays.stream(DLMFMacroFileHeaders.values());
    }
}
