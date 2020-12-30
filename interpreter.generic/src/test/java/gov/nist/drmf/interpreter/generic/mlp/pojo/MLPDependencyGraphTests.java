package gov.nist.drmf.interpreter.generic.mlp.pojo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.formulasearchengine.mathosphere.mlp.pojos.MathTag;
import com.formulasearchengine.mathosphere.mlp.text.WikiTextUtils;
import gov.nist.drmf.interpreter.common.pojo.CASResult;
import gov.nist.drmf.interpreter.common.tests.Resource;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Andre Greiner-Petter
 */
public class MLPDependencyGraphTests {
    @Resource("SingleFormulaSerialized.json")
    void testSingleFormulaJson(String json) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new GuavaModule());
        MOIPresentations sed = mapper.readValue(json, MOIPresentations.class);
        assertEquals( SemanticEnhancedAnnotationStatus.COMPUTED, sed.getStatus() );
        assertEquals( "P_{n}^{(\\alpha, \\beta)}(x)", sed.getGenericLatex() );
        assertEquals( 1, sed.getDefiniens().size() );

        Map<String, CASResult> casRes = sed.getCasRepresentations();
        assertEquals(2, casRes.keySet().size());
        assertTrue(casRes.containsKey("Maple"));
        assertTrue(casRes.containsKey("Mathematica"));

        CASResult mathRes = casRes.get("Mathematica");
        assertEquals( "JacobiP[n, \\[Alpha], \\[Beta], x]", mathRes.getCasRepresentation() );
        assertFalse( mathRes.getNumericResults().isSuccessful() );
        assertEquals( 0, mathRes.getNumericResults().getNumberOfTotalTests() );
        assertEquals( 0, mathRes.getNumericResults().getNumberOfFailedTests() );
        assertEquals( 0, mathRes.getNumericResults().getNumberOfSuccessfulTests() );
        assertEquals( 1, mathRes.getNumericResults().getTestCalculations().size() );
    }

    @Resource("SingleNodeExample.json")
    void testSingleNodeJson(String json) throws JsonProcessingException {
        SemanticEnhancedDocument sed = SemanticEnhancedDocument.deserialize(json);
        assertEquals( SemanticEnhancedAnnotationStatus.TRANSLATED, sed.getSemanticState() );

        MLPDependencyGraph graph = new MLPDependencyGraph(sed.getFormulae());
        assertEquals( 1, graph.getVertices().size() );
    }

    @Resource("JacobiResults.json")
    void testGeneratingGraphFromJson(String json) throws JsonProcessingException {
        SemanticEnhancedDocument sed = SemanticEnhancedDocument.deserialize(json);
        assertEquals( SemanticEnhancedAnnotationStatus.COMPUTED, sed.getSemanticState() );

        MLPDependencyGraph graph = new MLPDependencyGraph(sed.getFormulae());
        assertEquals( 48, graph.getVertices().size() );

        MathTag mTag = new MathTag("P_{n}^{(\\alpha, \\beta)}(x)", WikiTextUtils.MathMarkUpType.LATEX);
        Collection<MathTag> ingoing = graph.getIngoingEdges(mTag);
        assertEquals( 4, ingoing.size() );
        Set<String> ingoingNodes = ingoing.stream().map(MathTag::getContent).collect(Collectors.toSet());
        assertTrue( ingoingNodes.contains("x") );
        assertTrue( ingoingNodes.contains("n") );
        assertTrue( ingoingNodes.contains("P_{n}^{(\\alpha, \\beta)}") );
        assertTrue( ingoingNodes.contains("\\alpha,\\beta") );

        Collection<MathTag> outgoing = graph.getOutgoingEdges(mTag);
        assertEquals( 19, outgoing.size() );
        Set<String> outgoingNodes = outgoing.stream().map(MathTag::getContent).collect(Collectors.toSet());
        assertTrue( outgoingNodes.contains("P_0^{(\\alpha,\\beta)}(z)= 1") );
        assertTrue( outgoingNodes.contains("P_n^{(\\alpha,\\beta)}(z)=\\frac{(\\alpha+1)_n}{n!}{}_2F_1\\left(-n,1+\\alpha+\\beta+n;\\alpha+1;\\tfrac{1}{2}(1-z)\\right)") );
        assertTrue( outgoingNodes.contains("\\begin{align}&2n (n + \\alpha + \\beta) (2n + \\alpha + \\beta - 2) P_n^{(\\alpha,\\beta)}(z) \\\\&= (2n+\\alpha + \\beta-1) \\{ (2n+\\alpha + \\beta)(2n+\\alpha+\\beta-2) z +  \\alpha^2 - \\beta^2 \\} P_{n-1}^{(\\alpha,\\beta)}(z) - 2 (n+\\alpha - 1) (n + \\beta-1) (2n+\\alpha + \\beta) P_{n-2}^{(\\alpha, \\beta)}(z),\\end{align}") );
    }
}
