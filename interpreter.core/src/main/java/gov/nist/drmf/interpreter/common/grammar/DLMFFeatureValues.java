package gov.nist.drmf.interpreter.common.grammar;

import gov.nist.drmf.interpreter.common.Keys;
import mlp.FeatureSet;

import java.util.SortedSet;

/**
 * @author Andre Greiner-Petter
 */
public enum DLMFFeatureValues implements IFeatureExtractor{
    areas(
            t -> IFeatureExtractor.getStringFromSet(t.getFeature(Keys.FEATURE_AREAS))
    ),
    description(
            t -> IFeatureExtractor.getStringFromSet(t.getFeature(Keys.FEATURE_DESCRIPTION))
    ),
    meaning(
            t -> IFeatureExtractor.getStringFromSet(t.getFeature(Keys.FEATURE_MEANINGS))
    ),
    dlmf_link(
            t -> DLMFFeatureValues.DLMF_LINK_PREFIX +
                    IFeatureExtractor.getStringFromSet(
                            t.getFeature(Keys.KEY_DLMF + DLMFFeatureValues.LINK_SUFFIX))
    ),
    params(
            t -> IFeatureExtractor.getStringFromSet(t.getFeature(Keys.NUM_OF_PARAMS))
    ),
    ats(
            t -> IFeatureExtractor.getStringFromSet(t.getFeature(Keys.NUM_OF_ATS))
    ),
    variables(
            t -> IFeatureExtractor.getStringFromSet(t.getFeature(Keys.NUM_OF_VARS))
    ),
    CAS(
            t -> IFeatureExtractor.getStringFromSet(t.getFeature(Keys.CAS_KEY))
    ),
    CAS_Link(
            t -> DLMFFeatureValues.MAPLE_LINK_PREFIX +
                    IFeatureExtractor.getStringFromSet(
                            t.getFeature(Keys.CAS_KEY + DLMFFeatureValues.LINK_SUFFIX)
                    )
    ),
    DLMF(
            t -> IFeatureExtractor.getStringFromSet(t.getFeature(Keys.KEY_DLMF))
    ),
    constraints(
            t -> IFeatureExtractor.getStringFromSet(t.getFeature(Keys.FEATURE_CONSTRAINTS))
    ),
    branch_cuts(
            t -> IFeatureExtractor.getStringFromSet(t.getFeature(Keys.FEATURE_BRANCH_CUTS))
    );

    private static final String LINK_SUFFIX = "-Link";

    private static final String DLMF_LINK_PREFIX = "http://";

    private static final String MAPLE_LINK_PREFIX = "https://";

    //private String key;
    private IFeatureExtractor extractor;

    DLMFFeatureValues( IFeatureExtractor extractor ){
        this.extractor = extractor;
    }

    public String getFeatureValue( FeatureSet t ){
        return extractor.getFeatureValue(t);
    }
}
