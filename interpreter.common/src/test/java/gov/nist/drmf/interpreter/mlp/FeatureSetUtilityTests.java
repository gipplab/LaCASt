package gov.nist.drmf.interpreter.mlp;

import gov.nist.drmf.interpreter.mlp.extensions.PrintablePomTaggedExpression;
import mlp.FeatureSet;
import mlp.MathTerm;
import mlp.ParseException;
import mlp.PomTaggedExpression;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Andre Greiner-Petter
 */
public class FeatureSetUtilityTests {

    private static SemanticMLPWrapper mlp;

    @BeforeAll
    public static void setup() throws IOException {
        mlp = new SemanticMLPWrapper();
    }

    @Test
    public void getAllFeaturesTest() throws ParseException {
        PrintablePomTaggedExpression pte = mlp.parse("\\JacobiP{\\alpha}{\\beta}{n}@{\\cos@{x}}");

        MathTerm jacobiTerm = pte.getComponents().get(0).getRoot();
        Map<String, List<String>> features = FeatureSetUtility.getAllFeatures(jacobiTerm);

        checkJacobiFeatures(features);
    }

    @Test
    public void getAllFeaturesListTest() throws ParseException {
        PrintablePomTaggedExpression pte = mlp.parse("\\JacobiP{\\alpha}{\\beta}{n}@{\\cos@{x}}");

        MathTerm jacobiTerm = pte.getComponents().get(0).getRoot();
        Map<String, List<String>> features = FeatureSetUtility.getAllFeatures(
                jacobiTerm.getAlternativeFeatureSets()
        );

        checkJacobiFeatures(features);
    }

    private void checkJacobiFeatures( Map<String, List<String>> features ) {
        assertTrue( features.containsKey("DLMF") );
        assertTrue( features.containsKey("Maple") );
        assertTrue( features.containsKey("Mathematica") );
        assertEquals( 1, features.get("Mathematica").size() );
        assertEquals( "JacobiP[$2, $0, $1, $3]", features.get("Mathematica").get(0) );
    }

    @Test
    public void getAllSpecificFeaturesTest() throws ParseException {
        PrintablePomTaggedExpression pte = mlp.parse("A");

        MathTerm jacobiTerm = pte.getRoot();
        List<FeatureSet> features = FeatureSetUtility.getAllFeatureSetsWithFeature(
                jacobiTerm, "Role"
        );

        assertNotNull(features);
        assertFalse(features.isEmpty());
    }

    @Test
    public void getSpecificValueFeaturesTest() throws ParseException {
        PrintablePomTaggedExpression pte = mlp.parse("A");

        MathTerm jacobiTerm = pte.getRoot();
        FeatureSet featureSet = FeatureSetUtility.getSetByFeatureValue(
                jacobiTerm, "Role", "function"
        );

        assertNotNull(featureSet);
        assertEquals( 1, featureSet.getFeature("Role").size() );
        assertEquals( "function", featureSet.getFeature("Role").first() );
    }
}
