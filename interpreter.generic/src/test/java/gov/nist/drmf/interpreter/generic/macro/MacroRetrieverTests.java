package gov.nist.drmf.interpreter.generic.macro;

import com.fasterxml.jackson.core.JsonProcessingException;
import gov.nist.drmf.interpreter.common.config.GenericLacastConfig;
import gov.nist.drmf.interpreter.common.tests.Resource;
import gov.nist.drmf.interpreter.generic.GenericLatexSemanticEnhancer;
import gov.nist.drmf.interpreter.generic.elasticsearch.AssumeElasticsearchAvailability;
import gov.nist.drmf.interpreter.generic.mlp.pojo.*;
import gov.nist.drmf.interpreter.pom.moi.MOINode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Andre Greiner-Petter
 */
@AssumeElasticsearchAvailability
public class MacroRetrieverTests {

    private static MacroRetriever retriever;

    @BeforeAll
    static void setup() {
        GenericLacastConfig config = new GenericLacastConfig(GenericLacastConfig.getDefaultConfig());
        config.getSupportDescriptions().clear();
        config.setMaxDepth(1);
        config.setMaxMacros(3);
        config.setMaxRelations(3);
        retriever = new MacroRetriever(config);
    }

    @Resource({"../mlp/JacobiSemanticAnnotatedDoc.json"})
    void addTranslationsTest(String annotatedDoc) throws JsonProcessingException {
        SemanticEnhancedDocument sed = SemanticEnhancedDocument.deserialize(annotatedDoc);
        MLPDependencyGraph graph = new MLPDependencyGraph(sed.getFormulae());
        MOINode<MOIAnnotation> node = graph.getNode("FORMULA_c8b5b9184e45bca39744427c45693115");

        RetrievedMacros rm = retriever.retrieveReplacements(node);
        List<SemanticReplacementRule> rules = rm.getPatterns();
        for ( SemanticReplacementRule r : rules ) {
            System.out.println(r.toString());
        }

        Set<String> retrievedMacros = rules.stream().map(r -> r.getMacro().getName() ).collect(Collectors.toSet());
        assertTrue( retrievedMacros.contains("JacobipolyP") );
        assertTrue( retrievedMacros.contains("genhyperF") );
        assertTrue( retrievedMacros.contains("Pochhammersym") );
    }
}
