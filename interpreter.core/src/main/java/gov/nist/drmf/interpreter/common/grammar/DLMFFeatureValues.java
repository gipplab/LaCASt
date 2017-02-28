package gov.nist.drmf.interpreter.common.grammar;

import gov.nist.drmf.interpreter.common.GlobalConstants;
import gov.nist.drmf.interpreter.common.Keys;
import mlp.FeatureSet;

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
            t -> IFeatureExtractor.getStringFromSet(t.getFeature(GlobalConstants.CAS_KEY))
    ),
    CAS_Alternatives(
            t -> IFeatureExtractor.getStringFromSet(
                    t.getFeature( GlobalConstants.CAS_KEY + DLMFFeatureValues.ALTERNATIVE_SUFFIX ))
    ),
    CAS_Link(
            t -> DLMFFeatureValues.CAS_LINK_PREFIX +
                    IFeatureExtractor.getStringFromSet(
                    t.getFeature( GlobalConstants.CAS_KEY + DLMFFeatureValues.LINK_SUFFIX ))
    ),
    CAS_Comment(
            t -> IFeatureExtractor.getStringFromSet(
                    t.getFeature( GlobalConstants.CAS_KEY + DLMFFeatureValues.COMMENT_SUFFIX ))
    ),
    CAS_BranchCuts(
            t -> IFeatureExtractor.getStringFromSet(
                    t.getFeature( GlobalConstants.CAS_KEY + DLMFFeatureValues.BRANCH_CUTS_SUFFIX ))
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

    public static final String LINK_SUFFIX = "-Link";

    public static final String COMMENT_SUFFIX = "-Comment";

    public static final String ALTERNATIVE_SUFFIX = "-Alternatives";

    public static final String BRANCH_CUTS_SUFFIX = "-Branch Cuts";

    private static final String DLMF_LINK_PREFIX = "http://";

    private static final String CAS_LINK_PREFIX = "https://";

    //private String key;
    private IFeatureExtractor extractor;

    DLMFFeatureValues( IFeatureExtractor extractor ){
        this.extractor = extractor;
    }

    public String getFeatureValue( FeatureSet t ){
        return extractor.getFeatureValue(t);
    }
}
