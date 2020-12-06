package gov.nist.drmf.interpreter.cas.translation;

import gov.nist.drmf.interpreter.common.config.CASSupporter;
import gov.nist.drmf.interpreter.common.tests.TranslationTestCase;
import gov.nist.drmf.interpreter.common.tests.TranslationTestCaseProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.DynamicContainer;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static gov.nist.drmf.interpreter.common.tests.IgnoresAllWhitespacesMatcher.ignoresAllWhitespaces;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.DynamicContainer.dynamicContainer;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

/**
 * @author Andre Greiner-Petter
 */
public class TestCaseLoader {
    private static final Logger LOG = LogManager.getLogger(TestCaseLoader.class.getName());

    private final Map<String, SemanticLatexTranslator> translatorMap;
    private final String testFilePath;

    public TestCaseLoader(Map<String, SemanticLatexTranslator> translatorMap, String path) {
        this.testFilePath = path;
        this.translatorMap = translatorMap;
    }

    public Stream<DynamicContainer> forwardTranslations() throws Exception {
        Stream<TranslationTestCase> testStream = TranslationTestCaseProvider
                .provideTestCases(this.testFilePath, new String[]{});

        List<TranslationTestCase> tests = testStream.collect(Collectors.toList());
        LinkedList<DynamicContainer> testCaseList = new LinkedList<>();
        List<String> casList = CASSupporter.getSupportedCAS().getAllCAS();

        for ( String cas : casList ) {
            testCaseList.add( dynamicContainer(cas,
                    tests.stream()
                            .filter( test -> test.getCASTranslation(cas) != null)
                            .map( test -> {
                                return dynamicTest(test.toString(), () -> {
                                    LOG.debug("Testing " + test.toString());
                                    LOG.info( "Test:   " + test.getLatex());
                                    LOG.info( "Expect: " + test.getCASTranslation(cas));

                                    SemanticLatexTranslator slt = translatorMap.get(cas);

                                    String translation = null;
                                    if ( test.getLabel() == null || test.getLabel().isBlank() )
                                        translation = slt.translate(test.getLatex());
                                    else translation = slt.translate(test.getLatex(), test.getLabel());

                                    LOG.debug("Expected: " + test.getCASTranslation(cas));
                                    LOG.debug("Result:   " + translation);
                                    LOG.info( "Translated to: " + translation);

                                    translation = translation.replaceAll("\\s+", "");

                                    assertThat(translation, ignoresAllWhitespaces(test.getCASTranslation(cas)));
                                });
                            })
            ) );
        }

        return testCaseList.stream();
    }
}
