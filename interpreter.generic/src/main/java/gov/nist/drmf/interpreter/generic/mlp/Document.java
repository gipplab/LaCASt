package gov.nist.drmf.interpreter.generic.mlp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nist.drmf.interpreter.generic.mlp.pojo.MLPDependencyGraph;
import gov.nist.drmf.interpreter.generic.mlp.pojo.MOIAnnotation;
import gov.nist.drmf.interpreter.generic.mlp.pojo.SemanticEnhancedDocument;
import gov.nist.drmf.interpreter.pom.moi.MOINode;
import mlp.ParseException;

public interface Document {
    /**
     * @return the title of the document
     */
    String getTitle();

    /**
     * @return the content of the document
     */
    String getContent();

    /**
     * Extracts the formula dependency graph from the document.
     * This analyzes the entire document and returns the full
     * annotated graph structure of the document as {@link MLPDependencyGraph}.
     *
     * If you want to semantically annotate a single formula in the document
     * or annotate a formula that does not appear in the document itself
     * use {@link #getAnnotatedMOINode(String)} instead.
     *
     * @return the dependency graph of all formulae in the document
     */
    MLPDependencyGraph getMOIDependencyGraph();

    /**
     * Generates a {@link MLPDependencyGraph} from the given serialized JSON of a single {@link SemanticEnhancedDocument}.
     * @param serializedSemanticEnhancedDocument the serialized JSON string of a single {@link SemanticEnhancedDocument}
     * @return the graph {@link MLPDependencyGraph} of the serialized JSON object
     * @throws JsonProcessingException if the JSON cannot be deserialized
     */
    default MLPDependencyGraph deserializeDependencyGraph(String serializedSemanticEnhancedDocument) throws JsonProcessingException {
        SemanticEnhancedDocument sed = SemanticEnhancedDocument.deserialize(serializedSemanticEnhancedDocument);
        return new MLPDependencyGraph(sed.getFormulae());
    }

    /**
     * This annotates the given latex expression and returns the
     * node representation including attached definitions and dependencies
     * for the given expression. It does not generate the entire semantic
     * graph structure as with {@link #getMOIDependencyGraph()}. If you want
     * to annotate an entire document, use {@link #getMOIDependencyGraph()}.
     *
     * If the given expression is contained in this document or not does not matter.
     *
     * @param latex the latex expression you want to annotate (can be in the document
     *              or not, it doesn't matter).
     * @return the annotated node for the given expression
     * @throws ParseException if the given latex expression cannot be parsed
     */
    MOINode<MOIAnnotation> getAnnotatedMOINode(String latex) throws ParseException;
}
