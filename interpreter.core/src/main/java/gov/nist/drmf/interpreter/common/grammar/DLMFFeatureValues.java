package gov.nist.drmf.interpreter.common.grammar;

import gov.nist.drmf.interpreter.common.GlobalConstants;
import mlp.FeatureSet;

/**
 * @author Andre Greiner-Petter
 */
public enum DLMFFeatureValues implements IFeatureExtractor{
    areas(
            t -> t.getFeature("Areas").first()
    ),
    description(
            t -> t.getFeature("Description").first()
    ),
    dlmf_link(
            t -> DLMFFeatureValues.DLMF_LINK_PREFIX +
                    t.getFeature("DLMF-Link").first()
    ),
    params(
            t -> t.getFeature("Number of Parameters").first()
    ),
    ats(
            t -> t.getFeature("Number of optional ats").first()
    ),
    variables(
            t -> t.getFeature("Number of Variables").first()
    ),
    CAS(
            t -> t.getFeature(GlobalConstants.CAS_KEY +
                    " Representation").first()
    ),
    CAS_Link(
            t -> DLMFFeatureValues.MAPLE_LINK_PREFIX +
                    t.getFeature(GlobalConstants.CAS_KEY + "-Link").first()
    ),
    constraints(
            t -> t.getFeature("Constraints").toString()
    ),
    branch_cuts(
            t -> t.getFeature("Branch Cuts").toString()
    );

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
