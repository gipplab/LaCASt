package gov.nist.drmf.interpreter.mathematica.common;

import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @author Andre Greiner-Petter
 */
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(AssumeMathematicaAvailabilityCondition.class)
public @interface AssumeMathematicaAvailability {}
