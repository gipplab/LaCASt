package gov.nist.drmf.interpreter.generic.elasticsearch;

import gov.nist.drmf.interpreter.common.tests.AssumeToolAvailabilityCondition;

/**
 * @author Andre Greiner-Petter
 */
public class AssumeElasticsearchAvailabilityCondition extends AssumeToolAvailabilityCondition<AssumeElasticsearchAvailability> {
    @Override
    public Class<AssumeElasticsearchAvailability> getInterface() {
        return AssumeElasticsearchAvailability.class;
    }

    @Override
    public boolean isToolAvailable() {
        return DLMFElasticSearchClient.isEsAvailable();
    }

    @Override
    public String getToolName() {
        return "Elasticsearch";
    }
}
