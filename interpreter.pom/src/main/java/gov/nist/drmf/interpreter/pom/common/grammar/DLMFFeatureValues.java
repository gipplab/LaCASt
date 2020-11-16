package gov.nist.drmf.interpreter.pom.common.grammar;

import gov.nist.drmf.interpreter.common.constants.Keys;
import gov.nist.drmf.interpreter.pom.common.FeatureSetUtility;
import gov.nist.drmf.interpreter.pom.common.interfaces.ICASFeatureExtractor;
import mlp.FeatureSet;

import java.util.SortedSet;

/**
 * This enum provides easy and fast access to all feature set values given in the
 * lexicon. There are two main functions you are supposed to use if you want to extract
 * specific values from a feature set: {@link #getFeatureValue(FeatureSet, String)} and
 * {@link #getFeatureSet(FeatureSet, String)}.
 *
 * Suppose you have a feature set and want to get the Maple translation pattern. You get this
 * information by calling:
 * <pre>
 *     // featureSet is your feature set
 *     String mapleTranslationPattern = DLMFFeatureValues.CAS.getFeatureValue(featureSet, "Maple");
 * </pre>
 *
 * To get a specific feature set use {@link mlp.MathTerm#getNamedFeatureSet(String)}.
 *
 * @author Andre Greiner-Petter
 * @see FeatureSetUtility
 * @see mlp.FeatureSet
 * @see mlp.MathTerm
 */
public enum DLMFFeatureValues implements ICASFeatureExtractor {
    AREAS(
            (t, c) -> t.getFeature(Keys.FEATURE_AREAS)
    ),
    DESCRIPTION(
            (t, c) -> t.getFeature(Keys.FEATURE_DESCRIPTION)
    ),
    MEANING(
            (t, c) -> t.getFeature(Keys.FEATURE_MEANINGS)
    ),
    DLMF_LINK("http://",
            (t, c) -> t.getFeature(Keys.KEY_DLMF + Keys.KEY_LINK_SUFFIX)
    ),
    NUMBER_OF_OPTIONAL_PARAMETERS(
            (t, c) -> t.getFeature(Keys.NUM_OF_OPT_PARAMS)
    ),
    NUMBER_OF_PARAMETERS(
            (t, c) -> t.getFeature(Keys.NUM_OF_PARAMS)
    ),
    NUMBER_OF_ATS(
            (t, c) -> t.getFeature(Keys.NUM_OF_ATS)
    ),
    NUMBER_OF_VARIABLES(
            (t, c) -> t.getFeature(Keys.NUM_OF_VARS)
    ),
    SLOT_DERIVATIVE(
            (t, c) -> t.getFeature(Keys.SLOT_OF_DIFF)
    ),
    CAS_TRANSLATIONS(FeatureSet::getFeature),
    CAS_HYPERLINK("https://",
            (t, c) -> t.getFeature(c + Keys.KEY_LINK_SUFFIX)
    ),
    CAS_TRANSLATION_ALTERNATIVES(
            (t, c) -> t.getFeature(c + Keys.KEY_ALTERNATIVE_SUFFX)
    ),
    CAS_COMMENT(
            (t, c) -> t.getFeature(c + Keys.KEY_COMMENT_SUFFIX)
    ),
    CAS_BRANCH_CUTS(
            (t, c) -> t.getFeature(c + "-" + Keys.FEATURE_BRANCH_CUTS)
    ),
    REQUIRED_PACKAGES(
            (t, c) -> t.getFeature(c + Keys.KEY_EXTRA_PACKAGE_SUFFIX)
    ),
    DLMF_EXAMPLE(
            (t, c) -> t.getFeature(Keys.KEY_DLMF)
    ),
    CONSTRAINTS(
            (t, c) -> t.getFeature(Keys.FEATURE_CONSTRAINTS)
    ),
    BRANCH_CUTS(
            (t, c) -> t.getFeature(Keys.FEATURE_BRANCH_CUTS)
    );

    /**
     * The prefix and feature extractor
     */
    private final String prefix;
    private final ICASFeatureExtractor extractor;

    DLMFFeatureValues(ICASFeatureExtractor extractor) {
        this("", extractor);
    }

    DLMFFeatureValues(String prefix, ICASFeatureExtractor extractor) {
        this.prefix = prefix;
        this.extractor = extractor;
    }

    @Override
    public SortedSet<String> getFeatureSet(FeatureSet t, String cas) {
        return extractor.getFeatureSet(t, cas);
    }

    @Override
    public String getFeatureValue(FeatureSet featureSet, String cas) {
        return prefix + extractor.getFeatureValue(featureSet, cas);
    }
}
