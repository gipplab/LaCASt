package gov.nist.drmf.interpreter.generic.mlp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.formulasearchengine.mathosphere.mlp.pojos.Relation;
import gov.nist.drmf.interpreter.common.tests.Resource;
import gov.nist.drmf.interpreter.generic.elasticsearch.AssumeElasticsearchAvailability;
import gov.nist.drmf.interpreter.generic.elasticsearch.DLMFElasticSearchClient;
import gov.nist.drmf.interpreter.generic.mlp.pojo.MLPDependencyGraph;
import gov.nist.drmf.interpreter.generic.mlp.pojo.MOIAnnotation;
import gov.nist.drmf.interpreter.generic.mlp.pojo.MOIPresentations;
import gov.nist.drmf.interpreter.generic.mlp.pojo.SemanticEnhancedDocument;
import gov.nist.drmf.interpreter.pom.moi.MOINode;
import gov.nist.drmf.interpreter.pom.moi.MathematicalObjectOfInterest;
import mlp.ParseException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Andre Greiner-Petter
 */
@AssumeElasticsearchAvailability
public class SemanticEnhancerTests {
    @BeforeAll
    static void setup() throws IOException {
        new DLMFElasticSearchClient().indexDLMFDatabaseIfNotExist();
    }

    @Test
    void leviCivitaTest() throws ParseException {
        String genericLaTeXExample = "\\epsilon_{i j k}";
        String exampleAnnotationText = "Levi Civita Symbol";
        MOINode<MOIAnnotation> node = buildNode("1", genericLaTeXExample, exampleAnnotationText);
        MOIPresentations moi = new MOIPresentations(node);

        // this node has no further dependencies. Simply "Levi Civita Symbol" is attached and should be performed.
        SemanticEnhancer semanticEnhancer = new SemanticEnhancer();
        semanticEnhancer.appendSemanticLatex(moi, node);
        assertNotNull(moi.getSemanticLatex());
        assertEquals("\\LeviCivitasym{i}{j}{k}", moi.getSemanticLatex());
    }

    @Test
    void hermiteTest() throws ParseException {
        String genericLaTeXExample = "\\mathit{H}_{k-1}(z)";
        String exampleAnnotationText = "physicists ' Hermite polynomial";
        MOINode<MOIAnnotation> node = buildNode("1", genericLaTeXExample, exampleAnnotationText);
        MOIPresentations moi = new MOIPresentations(node);

        // this node has no further dependencies. Simply "Levi Civita Symbol" is attached and should be performed.
        SemanticEnhancer semanticEnhancer = new SemanticEnhancer();
        semanticEnhancer.appendSemanticLatex(moi, node);
        assertNotNull(moi.getSemanticLatex());
        assertEquals("\\HermitepolyH{k-1}@{z}", moi.getSemanticLatex());
    }

    @Test
    void leviCivitaInPlaceSourceNotSinkTest() throws ParseException {
        String genericLaTeXExample = "x + \\epsilon_{i j k}";
        String exampleAnnotationText = "Levi Civita Symbol";
        MOINode<MOIAnnotation> node = buildNode("2", genericLaTeXExample, exampleAnnotationText);
        MOINode<MOIAnnotation> nodeBig = buildNode("3", "x + \\epsilon_{i j k} + y", "compound expression");

        node.setupDependency(nodeBig);
        MOIPresentations moi = new MOIPresentations(node);

        // since node is a source (only outgoing edges), we should only allow exact matches which does not work here
        SemanticEnhancer semanticEnhancer = new SemanticEnhancer();
        semanticEnhancer.appendSemanticLatex(moi, node);
        assertNotNull(moi.getSemanticLatex());
        assertEquals("x + \\epsilon_{i j k}", moi.getSemanticLatex());

        // once we add another node which makes the levi civita symbol to a compound formula, we allow in-place matches
        MOINode<MOIAnnotation> nodeX = buildNode("1", "x", "variable");
        node.setupDependency(nodeX);
        moi = new MOIPresentations(node);

        semanticEnhancer.appendSemanticLatex(moi, node);
        assertNotNull(moi.getSemanticLatex());
        assertEquals("x + \\LeviCivitasym{i}{j}{k}", moi.getSemanticLatex());
    }

    @Test
    void leviCivitaDependencyTest() throws ParseException {
        String genericLaTeXExample = "x + \\epsilon_{i j k}";
        MOINode<MOIAnnotation> node = buildNode("1", genericLaTeXExample, "real variable");
        MOINode<MOIAnnotation> leviNode = buildNode("2", "\\epsilon_{i j k}", "Levi Civita Symbol");

        // setup dependency between both nodes
        node.setupDependency(leviNode);

        MOIPresentations moi = new MOIPresentations(node);

        assertEquals( 1, node.getIngoingDependencies().size() );
        assertEquals( 1, leviNode.getOutgoingDependencies().size() );

        SemanticEnhancer semanticEnhancer = new SemanticEnhancer();
        semanticEnhancer.appendSemanticLatex(moi, node);
        assertNotNull(moi.getSemanticLatex());
        assertEquals("x + \\LeviCivitasym{i}{j}{k}", moi.getSemanticLatex());
    }

    @Test
    void multiDependencyTest() throws ParseException {
        String genericLaTeXExample = "\\operatorname{ln}(x) + \\epsilon_{i j k}";
        MOINode<MOIAnnotation> node = buildNode("1", genericLaTeXExample, "complex equation");
        MOINode<MOIAnnotation> leviNode = buildNode("2", "\\epsilon_{i j k}", "Levi Civita Symbol");
        MOINode<MOIAnnotation> logNode = buildNode("3", "\\operatorname{ln} (x)", "logarithmic function");

        // setup dependency between both nodes
        node.setupDependency(leviNode);
        node.setupDependency(logNode);

        MOIPresentations moi = new MOIPresentations(node);

        assertEquals( 2, node.getIngoingDependencies().size() );
        assertEquals( 1, leviNode.getOutgoingDependencies().size() );
        assertEquals( 1, logNode.getOutgoingDependencies().size() );

        SemanticEnhancer semanticEnhancer = new SemanticEnhancer();
        semanticEnhancer.appendSemanticLatex(moi, node);
        assertNotNull(moi.getSemanticLatex());
        assertEquals("\\ln@{x} + \\LeviCivitasym{i}{j}{k}", moi.getSemanticLatex());
    }

    @Test
    void derivTestTest() throws ParseException {
        String genericLaTeXExample = "P_n^{(\\alpha,\\beta)}(z) = \\frac{d^n}{dz^n} \\left\\{ z \\left (1 - z^2 \\right )^n \\right\\}";
        MOINode<MOIAnnotation> node = buildNode("1", genericLaTeXExample, "complex equation");
        MOINode<MOIAnnotation> jacobiNode = buildNode("2", "P_n^{(\\alpha,\\beta)}(z)", "Jacobi polynomial");

        // setup dependency between both nodes
        node.setupDependency(jacobiNode);
        MOIPresentations moi = new MOIPresentations(node);
        assertEquals( 1, node.getIngoingDependencies().size() );

        SemanticEnhancer semanticEnhancer = new SemanticEnhancer();
        semanticEnhancer.appendSemanticLatex(moi, node);
        assertNotNull(moi.getSemanticLatex());
        assertEquals("\\JacobipolyP{\\alpha}{\\beta}{n}@{z} = \\deriv [n]{ }{z} \\{z(1 - z^2)^n \\}", moi.getSemanticLatex());
    }

    @Resource("ErrorFunctionMOI.json")
    void errorFunctionTranslationTest(String json) throws JsonProcessingException, ParseException {
        MOIPresentations moi = SemanticEnhancedDocument.getMapper().readValue(json, MOIPresentations.class);
        List<MOIPresentations> singleFormula = List.of(moi);
        MLPDependencyGraph graph = new MLPDependencyGraph(singleFormula);
        MOINode<MOIAnnotation> node = graph.getNode(moi.getId());

        SemanticEnhancer semanticEnhancer = new SemanticEnhancer();
        semanticEnhancer.appendSemanticLatex(moi, node);
        assertEquals("\\erf@@{z} = \\frac{2}{\\sqrt\\pi} \\int_0^z e^{-t^2} \\diff{t}", moi.getSemanticLatex());
    }

    private MOINode<MOIAnnotation> buildNode(String id, String genericTex, String annotationText) throws ParseException {
        MOIAnnotation complexExpression = new MOIAnnotation();
        complexExpression.appendRelation(new Relation(genericTex, annotationText));
        MathematicalObjectOfInterest complexMOI = new MathematicalObjectOfInterest(genericTex);
        return new MOINode<>(id, complexMOI, complexExpression);
    }
}
