package gov.nist.drmf.interpreter.common.grammar;

import gov.nist.drmf.interpreter.common.constants.Keys;
import gov.nist.drmf.interpreter.common.interfaces.IFeatureExtractor;
import mlp.FeatureSet;

/**
 * @author Andre Greiner-Petter
 */
public enum DLMFFeatureValues implements IFeatureExtractor {
    areas(
            (t,c) -> IFeatureExtractor.getStringFromSet(t.getFeature(Keys.FEATURE_AREAS))
    ),
    description(
            (t,c) -> IFeatureExtractor.getStringFromSet(t.getFeature(Keys.FEATURE_DESCRIPTION))
    ),
    meaning(
            (t,c) -> IFeatureExtractor.getStringFromSet(t.getFeature(Keys.FEATURE_MEANINGS))
    ),
    dlmf_link(
            (t,c) -> DLMFFeatureValues.DLMF_LINK_PREFIX +
                    IFeatureExtractor.getStringFromSet(
                            t.getFeature(Keys.KEY_DLMF + DLMFFeatureValues.LINK_SUFFIX))
    ),
    params(
            (t,c) -> IFeatureExtractor.getStringFromSet(t.getFeature(Keys.NUM_OF_PARAMS))
    ),
    ats(
            (t,c) -> IFeatureExtractor.getStringFromSet(t.getFeature(Keys.NUM_OF_ATS))
    ),
    variables(
            (t,c) -> IFeatureExtractor.getStringFromSet(t.getFeature(Keys.NUM_OF_VARS))
    ),
    slot(
            (t,c) -> IFeatureExtractor.getStringFromSet(t.getFeature(Keys.SLOT_OF_DIFF))
    ),
    CAS(
            (t,c) -> IFeatureExtractor.getStringFromSet(t.getFeature(c))
    ),
    CAS_Alternatives(
            (t,c) -> IFeatureExtractor.getStringFromSet(
                    t.getFeature( c + DLMFFeatureValues.ALTERNATIVE_SUFFIX ))
    ),
    CAS_Link(
            (t,c) -> DLMFFeatureValues.CAS_LINK_PREFIX +
                    IFeatureExtractor.getStringFromSet(
                    t.getFeature( c + DLMFFeatureValues.LINK_SUFFIX ))
    ),
    CAS_Comment(
            (t,c) -> IFeatureExtractor.getStringFromSet(
                    t.getFeature( c + DLMFFeatureValues.COMMENT_SUFFIX ))
    ),
    CAS_BranchCuts(
            (t,c) -> IFeatureExtractor.getStringFromSet(
                    t.getFeature( c + DLMFFeatureValues.BRANCH_CUTS_SUFFIX ))
    ),
    DLMF(
            (t,c) -> IFeatureExtractor.getStringFromSet(t.getFeature(Keys.KEY_DLMF))
    ),
    constraints(
            (t,c) -> IFeatureExtractor.getStringFromSet(t.getFeature(Keys.FEATURE_CONSTRAINTS))
    ),
    branch_cuts(
            (t,c) -> IFeatureExtractor.getStringFromSet(t.getFeature(Keys.FEATURE_BRANCH_CUTS))
    );

    public static final String LINK_SUFFIX = "-Link";

    public static final String COMMENT_SUFFIX = "-Comment";

    public static final String ALTERNATIVE_SUFFIX = "-Alternatives";

    public static final String BRANCH_CUTS_SUFFIX = "-Branch Cuts";

    private static final String DLMF_LINK_PREFIX = "http://";

    private static final String CAS_LINK_PREFIX = "https://";

    private IFeatureExtractor extractor;

    DLMFFeatureValues( IFeatureExtractor extractor ){
        this.extractor = extractor;
    }

    @Override
    public String getFeatureValue( FeatureSet t, String cas ){
        return extractor.getFeatureValue(t, cas);
    }
}
