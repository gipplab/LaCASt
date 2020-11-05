package gov.nist.drmf.interpreter.generic.pojo;

import gov.nist.drmf.interpreter.generic.mlp.struct.MLPDependencyGraph;
import gov.nist.drmf.interpreter.pom.moi.MOINode;
import gov.nist.drmf.interpreter.pom.moi.MathematicalObjectOfInterest;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Andre Greiner-Petter
 */
public class SemanticEnhancedDocument {

    private final List<MOIPresentations> formulae;

    public SemanticEnhancedDocument() {
        this.formulae = new LinkedList<>();
    }

    public SemanticEnhancedDocument(MLPDependencyGraph graph) {
        this.formulae = graph.getVertices().stream()
                .map(MOINode::getNode)
                .map(MathematicalObjectOfInterest::getOriginalLaTeX)
                .map(MOIPresentations::new)
                .collect(Collectors.toList());
    }

    public Stream<MOIPresentations> stream() {
        return formulae.stream();
    }

    public List<MOIPresentations> getFormulae() {
        return formulae;
    }
}
