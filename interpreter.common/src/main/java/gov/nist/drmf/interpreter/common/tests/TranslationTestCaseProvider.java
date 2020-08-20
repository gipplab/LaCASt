package gov.nist.drmf.interpreter.common.tests;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;

import java.io.IOException;
import java.lang.reflect.AnnotatedElement;
import java.net.URL;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * This class can be used to load test files for the translator.
 * <pre>
 *     @ParameterizedTest
 *     @ArgumentsSource(TranslationTestCaseProvider.class)
 *     @TestJsonSource(value = "TranslationTestCases.json", require = {"Maple"})
 *     void testTranslationsToMaple(TranslationTestCase tmp) {
 *         System.out.println("Example: " + tmp);
 *     }
 * </pre>
 *
 * @author Andre Greiner-Petter
 */
public class TranslationTestCaseProvider implements ArgumentsProvider {
    private static final Logger LOG = LogManager.getLogger(TranslationTestCaseProvider.class.getName());

    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext extensionContext) throws Exception {
        return provideTestCases(extensionContext).map(Arguments::of);
    }

    public static Stream<TranslationTestCase> provideTestCases(ExtensionContext extensionContext) throws Exception {
        TestJsonSource annotation = getPath(extensionContext);
        return provideTestCases(annotation.value(), annotation.require());
    }

    /**
     * Loads a stream of test cases for the given path from the resources directory.
     * The required argument is optional and can be null. If specified, it filters the
     * stream so that only test cases remain that contains the required translation.
     *
     * @param path path to the test json in the resources
     * @param requirements an array of required translations in the json or null
     * @return a stream of test cases loaded from the specified path and filtered by requirements
     * @throws IOException if the specified file cannot be loaded
     */
    public static Stream<TranslationTestCase> provideTestCases(String path, String... requirements) throws IOException {
        URL url = ClassLoader.getSystemResources(path).nextElement();
        ObjectMapper om = new ObjectMapper(new JsonFactory());
        TranslationTestCase[] cases = om.readValue(url, TranslationTestCase[].class);
        return Stream.of(cases)
                .filter( ttc -> {
                    if ( requirements == null ) return true;
                    for ( String req : requirements ){
                        if ( ttc.getCASTranslation(req) == null ) return false;
                    }
                    return true;
                });
    }

    private static TestJsonSource getPath(ExtensionContext extensionContext) {
        Optional<AnnotatedElement> element = extensionContext.getElement();
        if ( element.isPresent() ) {
            AnnotatedElement aelement = element.get();
            TestJsonSource[] annotations = aelement.getAnnotationsByType(TestJsonSource.class);
            if ( annotations != null && annotations.length > 0 ) {
                if ( annotations.length > 1 ) {
                    LOG.warn("Encountered multiple @TestJsonSources but only first is considered at "
                            + aelement.toString());
                }
                return annotations[0];
            }
        }
        return null;
    }
}
