package gov.nist.drmf.interpreter.common.grammar;

import mlp.FeatureSet;

/**
 * @author Andre Greiner-Petter
 */
public interface IFeatureExtractor {
    String getFeatureValue(FeatureSet feature_set);
}
