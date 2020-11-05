package gov.nist.drmf.interpreter.generic.elasticsearch;

import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @author Andre Greiner-Petter
 */
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(AssumeElasticsearchAvailabilityCondition.class)
public @interface AssumeElasticsearchAvailability {
}
