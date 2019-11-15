package gov.nist.drmf.interpreter.common.tests;

import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @author Andre Greiner-Petter
 */
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(AssumeMLPAvailabilityCondition.class)
public @interface AssumeMLPAvailability {
}
