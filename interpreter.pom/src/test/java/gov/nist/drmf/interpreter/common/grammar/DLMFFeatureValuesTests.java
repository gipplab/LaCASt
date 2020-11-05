package gov.nist.drmf.interpreter.common.grammar;

import gov.nist.drmf.interpreter.common.constants.Keys;
import gov.nist.drmf.interpreter.common.meta.AssumeMLPAvailability;
import gov.nist.drmf.interpreter.pom.FeatureSetUtility;
import gov.nist.drmf.interpreter.pom.SemanticMLPWrapper;
import gov.nist.drmf.interpreter.pom.extensions.PrintablePomTaggedExpression;
import mlp.FeatureSet;
import mlp.ParseException;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Andre Greiner-Petter
 */
@AssumeMLPAvailability
public class DLMFFeatureValuesTests {
    @Test
    void extractFeatureValuesTest() {
        String testMeaning = "test meaning";
        Map<String, SortedSet<String>> map = new TreeMap<>();
        SortedSet<String> set = new TreeSet<>();
        set.add(testMeaning);
        map.put(Keys.FEATURE_MEANINGS, set);

        FeatureSet fset = new FeatureSet(map);
        String entry = DLMFFeatureValues.MEANING.getFeatureValue(fset, "Maple");
        assertEquals(testMeaning, entry);
    }

    @Test
    void semanticMacroTest() throws ParseException {
        SemanticMLPWrapper mlp = SemanticMLPWrapper.getStandardInstance();
        PrintablePomTaggedExpression ppte = mlp.parse("\\acot@{x}");
        PrintablePomTaggedExpression sinPPTE = ppte.getPrintableComponents().get(0);
        FeatureSet fset = sinPPTE.getRoot().getNamedFeatureSet(Keys.KEY_DLMF_MACRO);
        String cas = Keys.KEY_MAPLE;

        assertTrue(DLMFFeatureValues.NUMBER_OF_OPTIONAL_PARAMETERS.getFeatureValue(fset, cas).isBlank());
        assertEquals(0, Integer.parseInt(DLMFFeatureValues.NUMBER_OF_PARAMETERS.getFeatureValue(fset, cas)));
        assertEquals(2, Integer.parseInt(DLMFFeatureValues.NUMBER_OF_ATS.getFeatureValue(fset, cas)));
        assertEquals(1, Integer.parseInt(DLMFFeatureValues.NUMBER_OF_VARIABLES.getFeatureValue(fset, cas)));

        assertEquals("z not element of {0,-\\iunit,+\\iunit}", DLMFFeatureValues.CONSTRAINTS.getFeatureValue(fset, cas));
        assertEquals("(-\\iunit, \\iunit)", DLMFFeatureValues.BRANCH_CUTS.getFeatureValue(fset, cas));

        assertEquals("arccot($0)", DLMFFeatureValues.CAS_TRANSLATIONS.getFeatureValue(fset, cas));
        SortedSet<String> alternativePattern = DLMFFeatureValues.CAS_TRANSLATION_ALTERNATIVES.getFeatureSet(fset, cas);
        assertEquals(2, alternativePattern.size());
        assertTrue( alternativePattern.contains("I/2*ln(($0-I)/($0+I) )") );
        assertTrue( alternativePattern.contains("arctan(1/($0))") );
        assertEquals("(- I infinity, - I ), ( I, I infinity )", DLMFFeatureValues.CAS_BRANCH_CUTS.getFeatureValue(fset, cas));

        assertEquals("http://dlmf.nist.gov/4.23#SS2.p1", DLMFFeatureValues.DLMF_LINK.getFeatureValue(fset, cas));
        assertEquals("https://www.maplesoft.com/support/help/maple/view.aspx?path=invtrig", DLMFFeatureValues.CAS_HYPERLINK.getFeatureValue(fset, cas));

        assertNull(DLMFFeatureValues.REQUIRED_PACKAGES.getFeatureSet(fset, cas));
    }
}
