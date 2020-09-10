package gov.nist.drmf.interpreter.cas.translation;

import gov.nist.drmf.interpreter.cas.translation.cases.InvalidTests;
import gov.nist.drmf.interpreter.common.constants.CASSupporter;
import gov.nist.drmf.interpreter.common.exceptions.InitTranslatorException;
import gov.nist.drmf.interpreter.common.exceptions.TranslationException;
import gov.nist.drmf.interpreter.common.meta.AssumeMLPAvailability;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Andre Greiner-Petter
 */
@AssumeMLPAvailability
@DisplayName("Forward Translation Tests")
public class ForwardTranslationTests {
    private static final Logger LOG = LogManager.getLogger(ForwardTranslationTests.class.getName());

    private static Map<String, SemanticLatexTranslator> translatorMap;

    @BeforeAll
    public static void setup() throws InitTranslatorException {
        translatorMap = new HashMap<>();
        for ( String cas : CASSupporter.getSupportedCAS().getAllCAS() ) {
            SemanticLatexTranslator translator = new SemanticLatexTranslator(cas);
            translatorMap.put(cas, translator);
        }
    }

    @TestFactory
    @DisplayName("Integral Translation Tests")
    Stream<DynamicContainer> integralTests() throws Exception {
        TestCaseLoader loader = new TestCaseLoader(translatorMap, "translations/Integrals.json");
        return loader.forwardTranslations();
    }

    @TestFactory
    @DisplayName("Sum Translation Tests")
    Stream<DynamicContainer> sumTests() throws Exception {
        TestCaseLoader loader = new TestCaseLoader(translatorMap, "translations/Sums.json");
        return loader.forwardTranslations();
    }

    @TestFactory
    @DisplayName("Product Translation Tests")
    Stream<DynamicContainer> productTests() throws Exception {
        TestCaseLoader loader = new TestCaseLoader(translatorMap, "translations/Products.json");
        return loader.forwardTranslations();
    }

    @TestFactory
    @DisplayName("Limit Translation Tests")
    Stream<DynamicContainer> limitTests() throws Exception {
        TestCaseLoader loader = new TestCaseLoader(translatorMap, "translations/Limits.json");
        return loader.forwardTranslations();
    }

    @TestFactory
    @DisplayName("Special Function Translation Tests")
    Stream<DynamicContainer> specialFunctionTests() throws Exception {
        TestCaseLoader loader = new TestCaseLoader(translatorMap, "translations/SpecialFunctions.json");
        return loader.forwardTranslations();
    }

    @ParameterizedTest
    @EnumSource(InvalidTests.class)
    @DisplayName("Invalid Translation Tests")
    void invalidTestCase(InvalidTests test) {
        for ( String cas : translatorMap.keySet() ) {
            SemanticLatexTranslator slt = translatorMap.get(cas);
            LOG.info("Test invalid translation expression for " + cas + " translator.");
            assertThrows(
                    TranslationException.class,
                    () -> slt.translate(test.getTeX())
            );
        }
    }
}
