package gov.nist.drmf.interpreter.common.tests;

import java.lang.annotation.*;

/**
 * This annotation specifies the source file to load JSON translation test cases.
 * The {@link #value()} field is mandatory and specifies the path from the resources root folder.
 * The {@link #require()} field allows to specify mandatory CAS that must exist in order
 * to load that test case.
 */
@Target({ElementType.ANNOTATION_TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TestJsonSource {
    /**
     * The path to the test json file from the root resources directory.
     * @return the path to the test file (mandatory and not null)
     */
    String value();

    /**
     * Specifies if specific CAS must be available in order to load a single test case.
     * For example, by adding 'Maple' to the required CAS, only test cases are loaded that
     * contains the 'Maple' field in the JSON test file.
     * @return a list of required CAS
     */
    String[] require() default {""};
}
