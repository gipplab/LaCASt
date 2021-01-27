package gov.nist.drmf.interpreter.common.test;

import gov.nist.drmf.interpreter.common.tests.Resource;
import gov.nist.drmf.interpreter.common.tests.TestJsonSource;
import gov.nist.drmf.interpreter.common.tests.TranslationTestCase;
import gov.nist.drmf.interpreter.common.tests.TranslationTestCaseProvider;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Andre Greiner-Petter
 */
public class AnnotationTests {

    @ParameterizedTest
    @ArgumentsSource(TranslationTestCaseProvider.class)
    @TestJsonSource(value = "Test.json", require = {"Maple", "Mathematica"})
    void test( TranslationTestCase testCase ) {
        assertEquals("ANNOTATION_TEST", testCase.getName());
        assertEquals("latex", testCase.getLatex());
        assertEquals("maple", testCase.getCASTranslation("Maple"));
        assertEquals("mathematica", testCase.getCASTranslation("Mathematica"));
    }

    @Resource({"Test.txt"})
    void loadSingleResourceTest(String content) {
        assertEquals( "TEST test TEST", content );
    }

    @Resource({"Test.txt","Test.txt"})
    void loadMultiResourceTest(String content1, String content2) {
        assertEquals( "TEST test TEST", content1 );
        assertEquals( content1, content2 );
    }
}
