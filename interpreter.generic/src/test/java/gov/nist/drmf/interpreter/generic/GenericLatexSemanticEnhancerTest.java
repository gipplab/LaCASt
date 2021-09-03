package gov.nist.drmf.interpreter.generic;

import com.fasterxml.jackson.core.JsonProcessingException;
import gov.nist.drmf.interpreter.common.pojo.FormulaDefinition;
import gov.nist.drmf.interpreter.common.tests.Resource;
import gov.nist.drmf.interpreter.generic.elasticsearch.AssumeElasticsearchAvailability;
import gov.nist.drmf.interpreter.generic.mlp.pojo.MOIPresentations;
import gov.nist.drmf.interpreter.common.pojo.SemanticEnhancedAnnotationStatus;
import gov.nist.drmf.interpreter.generic.mlp.pojo.SemanticEnhancedDocument;
import gov.nist.drmf.interpreter.maple.setup.AssumeMapleAvailability;
import gov.nist.drmf.interpreter.mathematica.common.AssumeMathematicaAvailability;
import mlp.ParseException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Andre Greiner-Petter
 */
@AssumeElasticsearchAvailability
public class GenericLatexSemanticEnhancerTest {
    @Resource("mlp/simpleWikitest.xml")
    void simpleWikitextTest(String text) {
        GenericLatexSemanticEnhancer enhancer = new GenericLatexSemanticEnhancer();
        SemanticEnhancedDocument semanticDocument = enhancer.generateAnnotatedDocument(text);
        assertEquals(SemanticEnhancedAnnotationStatus.SEMANTICALLY_ANNOTATED, semanticDocument.getRank());
        enhancer.appendTranslationsToDocument(semanticDocument);
        assertEquals(SemanticEnhancedAnnotationStatus.TRANSLATED, semanticDocument.getRank());

        List<MOIPresentations> moiPresentationsList = semanticDocument.getFormulae();

        MOIPresentations jacobi = moiPresentationsList.stream()
                .filter( m -> m.getGenericLatex().equals("P_n^{(\\alpha, \\beta)} (x)") )
                .findFirst()
                .orElse(null);

        assertNotNull(jacobi);
        assertEquals("\\JacobipolyP{\\alpha}{\\beta}{n}@{x}", jacobi.getSemanticLatex());
        assertEquals("JacobiP(n, alpha, beta, x)", jacobi.getCasResults("Maple").getCasRepresentation());
        assertEquals("JacobiP[n, \\[Alpha], \\[Beta], x]", jacobi.getCasResults("Mathematica").getCasRepresentation());

        List<FormulaDefinition> definitions = jacobi.getDefiniens();
        long hits = definitions.stream()
                .filter( d ->
                        d.getDefinition().equals("Jacobi polynomial") ||
                                d.getDefinition().equals("Carl Gustav Jacob Jacobi")
                )
                .count();
        assertEquals(2, hits);

        assertEquals(0, jacobi.getIngoingNodes().size());
        assertEquals(0, jacobi.getOutgoingNodes().size());
    }

    @Test
    void annotateSingleFormulaTest() throws ParseException {
        String context =
                "The Gamma function <math>\\Gamma(z)</math> and the pochhammer symbol <math>(a)_n</math> are often used together.";

        String includedMath = "\\Gamma(z)";
        String notIncludedMath = "\\Gamma( (\\alpha+1)_n )";

        GenericLatexSemanticEnhancer enhancer = new GenericLatexSemanticEnhancer();
        SemanticEnhancedDocument sed = enhancer.generateAnnotatedDocument(context);

        MOIPresentations gammaMOI = enhancer.generateMOIPresentationFromDocument(sed, includedMath);
        assertNotNull(gammaMOI);
        assertEquals("\\Gamma(z)", gammaMOI.getGenericLatex());
        assertEquals(SemanticEnhancedAnnotationStatus.TRANSLATED, gammaMOI.getRank());
        assertEquals("\\EulerGamma@{z}", gammaMOI.getSemanticLatex());
        assertEquals("GAMMA(z)", gammaMOI.getCasResults("Maple").getCasRepresentation());
        assertEquals("Gamma[z]", gammaMOI.getCasResults("Mathematica").getCasRepresentation());

        MOIPresentations gammaCompositionMOI = enhancer.generateMOIPresentationFromDocument(sed, notIncludedMath);
        assertNotNull(gammaCompositionMOI);
        assertEquals("\\Gamma( (\\alpha+1)_n )", gammaCompositionMOI.getGenericLatex());
        assertEquals(SemanticEnhancedAnnotationStatus.TRANSLATED, gammaMOI.getRank());
        assertEquals("\\EulerGamma@{\\Pochhammersym{\\alpha + 1}{n}}", gammaCompositionMOI.getSemanticLatex());
        assertEquals("GAMMA(pochhammer(alpha + 1, n))", gammaCompositionMOI.getCasResults("Maple").getCasRepresentation());
        assertEquals("Gamma[Pochhammer[\\[Alpha]+ 1, n]]", gammaCompositionMOI.getCasResults("Mathematica").getCasRepresentation());
    }

    /**
     * This rather short test case is quite heavy. Providing a json of the annotated document (i.e., a document with
     * definitions for each formula and the entire dependency graph) it checks if the outcome is the same document
     * annotated with translations to semantic LaTeX and the CAS Maple/Mathematica when possible.
     */
    @Resource({"mlp/JacobiSemanticAnnotatedDoc.json", "mlp/JacobiTranslatedDoc.json"})
    void addTranslationsTest(String annotatedDoc, String translatedDoc) throws JsonProcessingException {
        SemanticEnhancedDocument sed = SemanticEnhancedDocument.deserialize(annotatedDoc);
        GenericLatexSemanticEnhancer enhancer = new GenericLatexSemanticEnhancer();
        enhancer.appendTranslationsToDocument(sed);
        assertEquals( translatedDoc, sed.serialize(), "Translation does not match mlp/JacobiTranslatedDoc.json" );
    }
}
