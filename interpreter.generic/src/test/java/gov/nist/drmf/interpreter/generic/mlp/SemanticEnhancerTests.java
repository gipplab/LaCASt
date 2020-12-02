package gov.nist.drmf.interpreter.generic.mlp;

import com.formulasearchengine.mathosphere.mlp.pojos.Relation;
import gov.nist.drmf.interpreter.generic.elasticsearch.AssumeElasticsearchAvailability;
import gov.nist.drmf.interpreter.generic.elasticsearch.ElasticSearchConnector;
import gov.nist.drmf.interpreter.generic.mlp.struct.MOIAnnotation;
import gov.nist.drmf.interpreter.pom.extensions.PrintablePomTaggedExpression;
import gov.nist.drmf.interpreter.pom.moi.MOIDependencyGraph;
import gov.nist.drmf.interpreter.pom.moi.MOIDependencyGraphBuilder;
import gov.nist.drmf.interpreter.pom.moi.MOINode;
import gov.nist.drmf.interpreter.pom.moi.MathematicalObjectOfInterest;
import mlp.ParseException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Andre Greiner-Petter
 */
@AssumeElasticsearchAvailability
public class SemanticEnhancerTests {
    @BeforeAll
    static void setup() throws IOException {
        ElasticSearchConnector.getDefaultInstance().indexDLMFDatabase();
    }

    @Test
    void leviCivitaTest() throws ParseException, IOException {
        String genericLaTeXExample = "\\epsilon_{i j k}";
        String exampleAnnotationText = "Levi Civita Symbol";
        MOINode<MOIAnnotation> node = buildNode("1", genericLaTeXExample, exampleAnnotationText);

        // this node has no further dependencies. Simply "Levi Civita Symbol" is attached and should be performed.
        SemanticEnhancer semanticEnhancer = new SemanticEnhancer();
        PrintablePomTaggedExpression semanticallyEnhancedLaTeX = semanticEnhancer.semanticallyEnhance(node);
        assertNotNull(semanticallyEnhancedLaTeX);
        assertEquals("\\LeviCivitasym{i}{j}{k}", semanticallyEnhancedLaTeX.getTexString());
    }

    @Test
    void leviCivitaInPlaceSourceNotSinkTest() throws ParseException, IOException {
        String genericLaTeXExample = "x + \\epsilon_{i j k}";
        String exampleAnnotationText = "Levi Civita Symbol";
        MOINode<MOIAnnotation> node = buildNode("2", genericLaTeXExample, exampleAnnotationText);
        MOINode<MOIAnnotation> nodeBig = buildNode("3", "x + \\epsilon_{i j k} + y", "compound expression");
        node.setupDependency(nodeBig);

        // since node is a source (only outgoing edges), we should only allow exact matches which does not work here
        SemanticEnhancer semanticEnhancer = new SemanticEnhancer();
        PrintablePomTaggedExpression semanticallyEnhancedLaTeX = semanticEnhancer.semanticallyEnhance(node);
        assertNotNull(semanticallyEnhancedLaTeX);
        assertEquals("x + \\epsilon_{i j k}", semanticallyEnhancedLaTeX.getTexString());

        // once we add another node which makes the levi civita symbol to a compound formula, we allow in-place matches
        MOINode<MOIAnnotation> nodeX = buildNode("1", "x", "variable");
        node.setupDependency(nodeX);

        semanticallyEnhancedLaTeX = semanticEnhancer.semanticallyEnhance(node);
        assertNotNull(semanticallyEnhancedLaTeX);
        assertEquals("x + \\LeviCivitasym{i}{j}{k}", semanticallyEnhancedLaTeX.getTexString());
    }

    @Test
    void leviCivitaDependencyTest() throws ParseException, IOException {
        String genericLaTeXExample = "x + \\epsilon_{i j k}";
        MOINode<MOIAnnotation> node = buildNode("1", genericLaTeXExample, "real variable");
        MOINode<MOIAnnotation> leviNode = buildNode("2", "\\epsilon_{i j k}", "Levi Civita Symbol");

        // setup dependency between both nodes
        node.setupDependency(leviNode);

        assertEquals( 1, node.getIngoingDependencies().size() );
        assertEquals( 1, leviNode.getOutgoingDependencies().size() );

        SemanticEnhancer semanticEnhancer = new SemanticEnhancer();
        PrintablePomTaggedExpression semanticallyEnhancedLaTeX = semanticEnhancer.semanticallyEnhance(node);
        assertNotNull(semanticallyEnhancedLaTeX);
        assertEquals("x + \\LeviCivitasym{i}{j}{k}", semanticallyEnhancedLaTeX.getTexString());
    }

    @Test
    void multiDependencyTest() throws ParseException, IOException {
        String genericLaTeXExample = "\\operatorname{ln}(x) + \\epsilon_{i j k}";
        MOINode<MOIAnnotation> node = buildNode("1", genericLaTeXExample, "complex equation");
        MOINode<MOIAnnotation> leviNode = buildNode("2", "\\epsilon_{i j k}", "Levi Civita Symbol");
        MOINode<MOIAnnotation> logNode = buildNode("3", "\\operatorname{ln} (x)", "logarithmic function");

        // setup dependency between both nodes
        node.setupDependency(leviNode);
        node.setupDependency(logNode);

        assertEquals( 2, node.getIngoingDependencies().size() );
        assertEquals( 1, leviNode.getOutgoingDependencies().size() );
        assertEquals( 1, logNode.getOutgoingDependencies().size() );

        SemanticEnhancer semanticEnhancer = new SemanticEnhancer();
        PrintablePomTaggedExpression semanticallyEnhancedLaTeX = semanticEnhancer.semanticallyEnhance(node);
        assertNotNull(semanticallyEnhancedLaTeX);
        assertEquals("\\ln@{x} + \\LeviCivitasym{i}{j}{k}", semanticallyEnhancedLaTeX.getTexString());
    }

    @Test
    void derivTestTest() throws ParseException, IOException {
        String genericLaTeXExample = "P_n^{(\\alpha,\\beta)}(z) = \\frac{d^n}{dz^n} \\left\\{ z \\left (1 - z^2 \\right )^n \\right\\}";
        MOINode<MOIAnnotation> node = buildNode("1", genericLaTeXExample, "complex equation");
        MOINode<MOIAnnotation> jacobiNode = buildNode("2", "P_n^{(\\alpha,\\beta)}(z)", "Jacobi polynomial");

        // setup dependency between both nodes
        node.setupDependency(jacobiNode);
        assertEquals( 1, node.getIngoingDependencies().size() );

        SemanticEnhancer semanticEnhancer = new SemanticEnhancer();
        PrintablePomTaggedExpression semanticallyEnhancedLaTeX = semanticEnhancer.semanticallyEnhance(node);
        assertNotNull(semanticallyEnhancedLaTeX);
        assertEquals("\\JacobipolyP{\\alpha}{\\beta}{n}@{z} = \\deriv [n]{ }{z} \\{z (1 - z^2)^n \\}", semanticallyEnhancedLaTeX.getTexString());
    }

    private MOINode<MOIAnnotation> buildNode(String id, String genericTex, String annotationText) throws ParseException {
        MOIAnnotation complexExpression = new MOIAnnotation();
        complexExpression.appendRelation(new Relation(genericTex, annotationText));
        MathematicalObjectOfInterest complexMOI = new MathematicalObjectOfInterest(genericTex);
        return new MOINode<>(id, complexMOI, complexExpression);
    }
}
