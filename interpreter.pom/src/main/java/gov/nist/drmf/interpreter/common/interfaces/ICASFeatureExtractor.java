package gov.nist.drmf.interpreter.common.interfaces;

import gov.nist.drmf.interpreter.common.constants.Keys;
import gov.nist.drmf.interpreter.pom.MacrosLexicon;
import mlp.FeatureSet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.SortedSet;

/**
 * @author Andre Greiner-Petter
 */
@FunctionalInterface
public interface ICASFeatureExtractor extends IFeatureExtractor {
    /**
     * Returns the sorted values of a feature set for a given cas
     * @param featureSet the feature set
     * @param cas the cas
     * @retur the feature values
     */
    SortedSet<String> getFeatureSet(FeatureSet featureSet, String cas);

    /**
     * Returns the string of a feature set for a given cas.
     * @param featureSet the feature set
     * @param cas the computer algebra system
     * @return the string representation
     */
    default String getFeatureValue(FeatureSet featureSet, String cas) {
        return IFeatureExtractor.getStringFromSet(this.getFeatureSet(featureSet, cas));
    }

    @Override
    default SortedSet<String> getFeatureSet(FeatureSet featureSet) {
        return getFeatureSet(featureSet, Keys.KEY_MAPLE);
    }

    @Override
    default String getFeatureValue(FeatureSet featureSet) {
        return getFeatureValue(featureSet, Keys.KEY_MAPLE);
    }
}
