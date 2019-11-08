package gov.nist.drmf.interpreter.maple.setup;

import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * This is an annotation for test classes that require Maple
 * to run properly. Annotate the class with this annotation
 * and the tests are skipped if Maple is not available.
 *
 * @author Andre Greiner-Petter
 */
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(AssumeMapleAvailabilityCondition.class)
public @interface AssumeMapleAvailability {}
