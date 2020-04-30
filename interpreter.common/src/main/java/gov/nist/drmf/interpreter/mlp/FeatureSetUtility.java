package gov.nist.drmf.interpreter.mlp;

import gov.nist.drmf.interpreter.common.constants.Keys;
import gov.nist.drmf.interpreter.common.grammar.MathTermTags;
import mlp.FeatureSet;
import mlp.MathTerm;

import java.util.*;

/**
 * This class provides typical functions on FeatureSets.
 *
 * @author Andre Greiner-Petter
 */
public final class FeatureSetUtility {
    public static final String LATEX_FEATURE_KEY = "LaTeX";

    private FeatureSetUtility() {
        throw new UnsupportedOperationException();
    }

    /**
     * Collects all features of all feature sets of a given MathTerm object
     * and returns a map of all features for this term.
     * Each feature possibly contains multiple values now.
     * @param term given math term
     * @return map of all features and all values
     */
    public static Map<String, List<String>> getAllFeatures(MathTerm term){
        List<FeatureSet> sets = term.getAlternativeFeatureSets();
        return getAllFeatures(sets);
    }

    /**
     *
     * @param sets
     * @return
     */
    public static Map<String, List<String>> getAllFeatures(List<FeatureSet> sets){
        Map<String,List<String>> map = new HashMap<>();
        for ( FeatureSet fset : sets ){
            Set<String> features = fset.getFeatureNames();
            for ( String name : features ){
                SortedSet<String> fValues = fset.getFeature(name);
                if ( fValues.isEmpty() ) continue;
                List<String> values = new ArrayList<>(fValues);
                if ( !map.containsKey(name) ){
                    List<String> old_list = map.get(name);
                    old_list.addAll( values );
                } else {
                    map.put(name, values);
                }
            }
        }
        return map;
    }

    /**
     *
     * @param term
     * @param feature
     * @return could be an empty list but could not be null!
     */
    public static List<FeatureSet> getAllFeatureSetsWithFeature( MathTerm term, String feature ){
        List<FeatureSet> list = term.getAlternativeFeatureSets();
        List<FeatureSet> finalList = new LinkedList<>();
        for( FeatureSet fset : list ){
            if ( fset.getFeature(feature) != null )
                finalList.add(fset);
        }
        return finalList;
    }

    /**
     *
     * @param term
     * @param feature
     * @param value
     * @return
     */
    public static FeatureSet getSetByFeatureValue( MathTerm term, String feature, String value ){
        List<FeatureSet> list = term.getAlternativeFeatureSets();
        for ( FeatureSet fset : list ){
            SortedSet<String> set = fset.getFeature(feature);
            if ( set == null ) continue;
            if ( set.contains(value) ) return fset;
        }
        return null;
    }

    /**
     *
     * @param term
     * @return
     */
    public static boolean isGreekLetter( MathTerm term ){
        List<FeatureSet> list = term.getAlternativeFeatureSets();
        for ( FeatureSet fset : list ){
            SortedSet<String> set = fset.getFeature(Keys.FEATURE_ALPHABET);
            if ( set == null ) continue;
            if ( set.contains(Keys.FEATURE_VALUE_GREEK) ) return true;
        }
        return false;
    }

    public static boolean isFunction( MathTerm term ){
        FeatureSet set = getSetByFeatureValue(term, Keys.FEATURE_ROLE, MathTermTags.function.tag());
        return set != null;
    }
}
