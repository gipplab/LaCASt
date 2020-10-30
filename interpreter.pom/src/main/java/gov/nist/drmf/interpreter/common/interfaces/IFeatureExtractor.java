package gov.nist.drmf.interpreter.common.interfaces;

import gov.nist.drmf.interpreter.pom.MacrosLexicon;
import mlp.FeatureSet;

import java.util.*;

/**
 * @author Andre Greiner-Petter
 */
public interface IFeatureExtractor {
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
        return getStringFromSet(getFeatureSet(featureSet, cas));
    }

    static void setFeatureValue(FeatureSet featureSet, String name, String... values) {
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
