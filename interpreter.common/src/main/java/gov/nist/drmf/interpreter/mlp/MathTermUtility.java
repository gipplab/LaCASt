package gov.nist.drmf.interpreter.mlp;

import gov.nist.drmf.interpreter.common.constants.Keys;
import gov.nist.drmf.interpreter.common.grammar.LimitedExpressions;
import gov.nist.drmf.interpreter.common.grammar.MathTermTags;
import mlp.FeatureSet;
import mlp.MathTerm;

import java.util.List;
import java.util.SortedSet;

import static gov.nist.drmf.interpreter.mlp.FeatureSetUtility.getSetByFeatureValue;

/**
 * @author Andre Greiner-Petter
 */
public abstract class MathTermUtility {

    private MathTermUtility(){}

    /**
     * @param term math term
     * @return true if the given term is a greek letter
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

    /**
     * @param term math term
     * @return true if the given term is a function
     */
    public static boolean isFunction( MathTerm term ){
        MathTermTags tag = MathTermTags.getTagByKey(term.getTag());
        if (tag == null) {
            FeatureSet set = getSetByFeatureValue(term, Keys.FEATURE_ROLE, MathTermTags.function.tag());
            return set != null;
        } else return tag.equals(MathTermTags.function);
    }

    /**
     * Check if the provided math term is of the type tag.
     * @param term math term
     * @param tag the math term tag that should equal the given math term
     * @return true if the math term has the given tag
     */
    public static boolean equals( MathTerm term, MathTermTags tag ) {
        if ( term == null ) return false;
        MathTermTags t = MathTermTags.getTagByKey(term.getTag());
        return tag.equals(t);
    }

    public static boolean isSumOrProductOrLimit(MathTerm term) {
        return LimitedExpressions.isLimitedExpression(term);
    }
}
