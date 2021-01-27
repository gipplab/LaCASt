package gov.nist.drmf.interpreter.pom.common.interfaces;

import gov.nist.drmf.interpreter.pom.MacrosLexicon;
import mlp.FeatureSet;

import java.util.*;

/**
 * @author Andre Greiner-Petter
 */
@FunctionalInterface
public interface IFeatureExtractor {
    /**
     * Returns the sorted values of a feature set for a given cas
     * @param featureSet the feature set
     * @return the feature values
     */
    SortedSet<String> getFeatureSet(FeatureSet featureSet);

    /**
     * Returns the string of a feature set for a given cas.
     * @param featureSet the feature set
     * @return the string representation
     */
    default String getFeatureValue(FeatureSet featureSet) {
        return IFeatureExtractor.getStringFromSet(getFeatureSet(featureSet));
    }

    /**
     * Sets the value for the name and values in the given feature set
     * @param featureSet the feature set
     * @param name the name of the new feature
     * @param values the values
     */
    static void setFeatureValue(FeatureSet featureSet, String name, String... values) {
        if ( featureSet != null && values != null )
            featureSet.setFeature(name, Arrays.asList(values.clone()));
    }

    /**
     * Converts the given sorted set back to a single string in the same way as it was
     * given in the PoM-tagger lexicon, i.e., multiple values are concatenated via
     * {@link MacrosLexicon#SIGNAL_INLINE}.
     * @param set the set of entries
     * @return string representation of the set of entries.
     */
    static String getStringFromSet( SortedSet<String> set ){
        if ( set == null ) return "";
        String output = "";
        List<String> list = new ArrayList<>(set);
        while ( list.size() > 1 ){
            output += list.remove(0) + MacrosLexicon.SIGNAL_INLINE;
        }
        output += list.remove(0);
        return output;
    }
}
