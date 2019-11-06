package gov.nist.drmf.interpreter.common.meta;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation to show which tests come from DLMF
 * @author Andre Greiner-Petter
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface DLMF {
    String value() default "";
}
