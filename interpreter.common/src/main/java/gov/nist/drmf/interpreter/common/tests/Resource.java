package gov.nist.drmf.interpreter.common.tests;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.lang.annotation.*;

/**
 * This argument is used to easily load resources from the resources directory simply via providing this
 * annotation at the method. It loads the given resource file to a string and provides the string as an argument to
 * test method. The paths you must provide are the same path as your class path. That means if you test class
 * is in example/path/test and you provide "Test.txt" the file must be in resources/example/path/test/Test.txt
 *
 * Simple use it via
 * <pre>
 *  {@literal @}Resource("myFile.txt")
 *  void test(String contentOfFile) { ... }
 *
 *  {@literal @}Resource({"myFile1.txt", "myFile2.txt"})
 *  void testMulti(String contentFile1, String contentFile2) {...}
 * </pre>
 *
 * @author Andre Greiner-Petter
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@ArgumentsSource(ResourceProvider.class)
@ParameterizedTest(name = "[{index}] {displayName}")
public @interface Resource {
    /**
     * Provide a list of resource files that will be loaded at once
     * @return the array of resources that will be loaded in that order
     */
    String[] value();
}
